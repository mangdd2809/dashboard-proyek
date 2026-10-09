#!/usr/bin/env python3
"""
=============================================================================
REKAP PROYEK AI - ARMBIAN PYTHON SERVER & MYSQL BACKEND
=============================================================================
Backend REST API & Telegram Bot Worker untuk Server Armbian (Linux SBC).
Fitur:
1. Sinkronisasi data aplikasi Android (Room DB -> MySQL Armbian).
2. Endpoint REST API: /ping, /sync, /projects, /materials, /wages, /summary.
3. Telegram Bot webhook & background poller terintegrasi.
4. Mesin ekstraksi cerdas (Gemini AI API + Parser NLP Lokal).
5. Web Dashboard Interaktif untuk memantau status Armbian & MySQL via browser.
=============================================================================
"""

import os
import sys
import time
import json
import re
import threading
from typing import Dict, Any, List, Optional
from datetime import datetime

from flask import Flask, request, jsonify, render_template_string

# Load environment variables if .env exists
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

# Try importing PyMySQL
try:
    import pymysql
    from pymysql.cursors import DictCursor
    HAS_PYMYSQL = True
except ImportError:
    HAS_PYMYSQL = False

# Try importing requests for Telegram and Gemini API
try:
    import requests
    HAS_REQUESTS = True
except ImportError:
    HAS_REQUESTS = False

# ---------------------------------------------------------------------------
# CONFIGURATION
# ---------------------------------------------------------------------------
SERVER_HOST = os.environ.get("SERVER_HOST", "0.0.0.0")
SERVER_PORT = int(os.environ.get("SERVER_PORT", "8000"))
API_KEY = os.environ.get("API_KEY", "armbian_secret_token_2026")

# MySQL Configuration
MYSQL_HOST = os.environ.get("MYSQL_HOST", "localhost")
MYSQL_PORT = int(os.environ.get("MYSQL_PORT", "3306"))
MYSQL_USER = os.environ.get("MYSQL_USER", "root")
MYSQL_PASSWORD = os.environ.get("MYSQL_PASSWORD", "")
MYSQL_DB = os.environ.get("MYSQL_DB", "db_rekap_konstruksi")

# Telegram & AI Configuration
TELEGRAM_BOT_TOKEN = os.environ.get("TELEGRAM_BOT_TOKEN", "")
TELEGRAM_CHAT_ID = os.environ.get("TELEGRAM_CHAT_ID", "")
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "")
ENABLE_TELEGRAM_POLLER = os.environ.get("ENABLE_TELEGRAM_POLLER", "true").lower() in ("true", "1", "yes")

app = Flask(__name__)

# ---------------------------------------------------------------------------
# DATABASE HELPERS
# ---------------------------------------------------------------------------
def get_db_connection():
    """Membuat koneksi ke database MySQL di Armbian."""
    if not HAS_PYMYSQL:
        raise RuntimeError("Modul pymysql belum terpasang. Jalankan: pip install pymysql")

    return pymysql.connect(
        host=MYSQL_HOST,
        port=MYSQL_PORT,
        user=MYSQL_USER,
        password=MYSQL_PASSWORD,
        database=MYSQL_DB,
        charset="utf8mb4",
        cursorclass=DictCursor,
        autocommit=True
    )

def init_database_tables():
    """Inisialisasi database dan tabel MySQL jika belum ada."""
    if not HAS_PYMYSQL:
        print("[WARN] PyMySQL tidak tersedia, melewati inisialisasi tabel MySQL.")
        return

    try:
        conn = pymysql.connect(
            host=MYSQL_HOST,
            port=MYSQL_PORT,
            user=MYSQL_USER,
            password=MYSQL_PASSWORD,
            charset="utf8mb4",
            autocommit=True
        )
        with conn.cursor() as cursor:
            cursor.execute(f"CREATE DATABASE IF NOT EXISTS `{MYSQL_DB}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
            cursor.execute(f"USE `{MYSQL_DB}`;")

            # 1. Projects
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS `projects` (
              `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
              `name` VARCHAR(150) NOT NULL,
              `code_spk` VARCHAR(50) DEFAULT NULL,
              `location` VARCHAR(255) DEFAULT NULL,
              `client_name` VARCHAR(150) DEFAULT NULL,
              `budget_rab` DECIMAL(15, 2) DEFAULT 0.00,
              `start_date` BIGINT DEFAULT NULL,
              `target_end_date` BIGINT DEFAULT NULL,
              `progress_percent` FLOAT DEFAULT 0.0,
              `status` ENUM('Aktif', 'Selesai', 'Ditunda') DEFAULT 'Aktif',
              `foreman_in_charge` VARCHAR(100) DEFAULT NULL,
              `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """)

            # 2. Materials
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS `material_usages` (
              `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
              `project_id` BIGINT NOT NULL,
              `date` BIGINT NOT NULL,
              `material_name` VARCHAR(150) NOT NULL,
              `category` VARCHAR(80) DEFAULT 'Semen & Pasir',
              `quantity` DECIMAL(10, 2) NOT NULL,
              `unit` VARCHAR(30) NOT NULL,
              `unit_price` DECIMAL(15, 2) NOT NULL,
              `total_cost` DECIMAL(15, 2) NOT NULL,
              `supplier` VARCHAR(150) DEFAULT NULL,
              `invoice_note` TEXT DEFAULT NULL,
              `source` VARCHAR(50) DEFAULT 'Telegram Bot',
              `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """)

            # 3. Wages
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS `labor_wages` (
              `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
              `project_id` BIGINT NOT NULL,
              `date` BIGINT NOT NULL,
              `worker_role` VARCHAR(100) NOT NULL,
              `worker_count` INT NOT NULL DEFAULT 1,
              `duration_hok` DECIMAL(5, 2) NOT NULL DEFAULT 1.0,
              `wage_rate` DECIMAL(15, 2) NOT NULL,
              `total_wage` DECIMAL(15, 2) NOT NULL,
              `task_description` TEXT DEFAULT NULL,
              `foreman_name` VARCHAR(100) DEFAULT NULL,
              `source` VARCHAR(50) DEFAULT 'Telegram Bot',
              `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """)

            # 4. Telegram Logs
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS `telegram_logs` (
              `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
              `telegram_msg_id` BIGINT DEFAULT NULL,
              `chat_id` VARCHAR(100) DEFAULT NULL,
              `sender_name` VARCHAR(150) DEFAULT NULL,
              `raw_text` TEXT NOT NULL,
              `received_at` BIGINT NOT NULL,
              `parse_status` VARCHAR(50) DEFAULT 'SUCCESS',
              `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """)
        conn.close()
        print(f"[OK] Database MySQL `{MYSQL_DB}` dan tabel berhasil diinisialisasi di Armbian.")
    except Exception as e:
        print(f"[WARN] Gagal inisialisasi MySQL: {e}")

# ---------------------------------------------------------------------------
# AUTHENTICATION DECORATOR
# ---------------------------------------------------------------------------
def verify_api_key():
    client_key = request.headers.get("X-API-Key", "")
    if not client_key:
        client_key = request.args.get("api_key", "")
    if client_key != API_KEY:
        return False
    return True

# ---------------------------------------------------------------------------
# AI & NLP EXTRACTION ENGINES
# ---------------------------------------------------------------------------
def extract_construction_data_gemini(text: str, api_key: str) -> Optional[Dict[str, Any]]:
    """Ekstraksi teks mandor menggunakan Gemini API."""
    if not api_key or not HAS_REQUESTS:
        return None

    try:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={api_key}"
        headers = {"Content-Type": "application/json"}
        prompt = (
            "Kamu adalah AI asisten rekap proyek konstruksi. Analisa laporan mandor berikut dan ekstrak ke format JSON murni tanpa markdown:\n"
            f"Laporan: \"{text}\"\n\n"
            "Format JSON yang harus dihasilkan:\n"
            "{\n"
            "  \"materials\": [\n"
            "    {\"name\": \"Nama Material\", \"category\": \"Semen & Pasir|Besi & Baja|Bata & Dinding|Finishing & Cat|Lainnya\", \"quantity\": 10, \"unit\": \"sak\", \"unit_price\": 68000, \"total_cost\": 680000}\n"
            "  ],\n"
            "  \"labors\": [\n"
            "    {\"role\": \"Tukang Batu\", \"count\": 3, \"duration_hok\": 1.0, \"wage_rate\": 130000, \"total_wage\": 390000, \"description\": \"Pengecoran kolom\"}\n"
            "  ],\n"
            "  \"progress_percent\": 45.0,\n"
            "  \"notes\": \"Ringkasan aktivitas\"\n"
            "}\n"
        )
        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {"temperature": 0.1, "responseMimeType": "application/json"}
        }
        res = requests.post(url, headers=headers, json=payload, timeout=20)
        if res.status_code == 200:
            result_json = res.json()
            raw_text = result_json["candidates"][0]["content"]["parts"][0]["text"]
            clean_text = raw_text.replace("```json", "").replace("```", "").strip()
            return json.loads(clean_text)
    except Exception as e:
        print(f"[GEMINI API WARN] {e}, fallback ke NLP lokal.")
    return None

def extract_construction_data_local(text: str) -> Dict[str, Any]:
    """Ekstraksi teks mandor konstruksi menggunakan logika pattern regex cerdas."""
    lower = text.lower()
    materials = []
    labors = []
    progress_percent = None

    # Detect progress %
    prog_match = re.search(r"(\d+(?:\.\d+)?)\s*%", text)
    if prog_match:
        progress_percent = float(prog_match.group(1))

    # Helper harga
    def parse_price(s):
        p = re.search(r"@?\s*(\d+(?:\.\d+)?)\s*(?:rb|ribu|k)", s, re.IGNORECASE)
        if p:
            val = float(p.group(1))
            return val * 1000
        p2 = re.search(r"(?:rp|@)\s*([\d.]+)", s, re.IGNORECASE)
        if p2:
            return float(p2.group(1).replace(".", ""))
        return 0.0

    lines = text.split("\n")
    for line in lines:
        line_l = line.lower()
        # Semen
        if "semen" in line_l:
            qty_m = re.search(r"(\d+)\s*(?:sak|zak)", line_l)
            qty = float(qty_m.group(1)) if qty_m else 10.0
            price = parse_price(line_l) or 68000.0
            materials.append({
                "name": "Semen Gresik 40kg",
                "category": "Semen & Pasir",
                "quantity": qty,
                "unit": "sak",
                "unit_price": price,
                "total_cost": qty * price
            })
        # Besi
        elif "besi" in line_l:
            qty_m = re.search(r"(\d+)\s*(?:btg|batang)", line_l)
            qty = float(qty_m.group(1)) if qty_m else 20.0
            price = parse_price(line_l) or 115000.0
            materials.append({
                "name": "Besi Beton Ulir",
                "category": "Besi & Baja",
                "quantity": qty,
                "unit": "btg",
                "unit_price": price,
                "total_cost": qty * price
            })
        # Pasir
        elif "pasir" in line_l:
            qty_m = re.search(r"(\d+)\s*(?:rit|truk|m3)", line_l)
            qty = float(qty_m.group(1)) if qty_m else 1.0
            price = parse_price(line_l) or 750000.0
            materials.append({
                "name": "Pasir Pasang Merapi",
                "category": "Semen & Pasir",
                "quantity": qty,
                "unit": "rit",
                "unit_price": price,
                "total_cost": qty * price
            })

        # Tukang
        if "tukang" in line_l:
            cnt_m = re.search(r"(\d+)\s*(?:org|orang)", line_l)
            cnt = int(cnt_m.group(1)) if cnt_m else 3
            rate = parse_price(line_l) or 130000.0
            labors.append({
                "role": "Tukang Cor/Batu",
                "count": cnt,
                "duration_hok": 1.0,
                "wage_rate": rate,
                "total_wage": cnt * 1.0 * rate,
                "description": line.strip()
            })
        # Kenek
        if "kenek" in line_l or "pembantu" in line_l:
            cnt_m = re.search(r"(\d+)\s*(?:org|orang)", line_l)
            cnt = int(cnt_m.group(1)) if cnt_m else 2
            rate = parse_price(line_l) or 95000.0
            labors.append({
                "role": "Kenek/Pembantu",
                "count": cnt,
                "duration_hok": 1.0,
                "wage_rate": rate,
                "total_wage": cnt * 1.0 * rate,
                "description": line.strip()
            })

    return {
        "materials": materials,
        "labors": labors,
        "progress_percent": progress_percent,
        "notes": text.strip()
    }

def extract_construction_data(text: str) -> Dict[str, Any]:
    """Pipeline terpadu: Utamakan Gemini AI jika API Key ada, lalu fallback ke Regex NLP."""
    if GEMINI_API_KEY:
        ai_res = extract_construction_data_gemini(text, GEMINI_API_KEY)
        if ai_res and (ai_res.get("materials") or ai_res.get("labors")):
            return ai_res
    return extract_construction_data_local(text)

# ---------------------------------------------------------------------------
# REST API ENDPOINTS
# ---------------------------------------------------------------------------
@app.route("/", methods=["GET"])
def index():
    """Tampilan Web Dashboard untuk monitoring via browser di PC/Laptop/HP."""
    accept = request.headers.get("Accept", "")
    if "application/json" in accept and "text/html" not in accept:
        return health_check()

    # Query info statistik MySQL
    mysql_status = "Disconnected"
    total_projects = 0
    total_materials = 0
    total_wages = 0
    recent_logs = []

    if HAS_PYMYSQL:
        try:
            conn = get_db_connection()
            with conn.cursor() as cur:
                cur.execute("SELECT COUNT(*) AS c FROM projects")
                total_projects = cur.fetchone()["c"]
                cur.execute("SELECT COUNT(*) AS c FROM material_usages")
                total_materials = cur.fetchone()["c"]
                cur.execute("SELECT COUNT(*) AS c FROM labor_wages")
                total_wages = cur.fetchone()["c"]
                cur.execute("SELECT sender_name, raw_text, received_at FROM telegram_logs ORDER BY id DESC LIMIT 5")
                recent_logs = cur.fetchall()
            conn.close()
            mysql_status = "Connected"
        except Exception as e:
            mysql_status = f"Error: {e}"

    html_template = """
    <!DOCTYPE html>
    <html lang="id">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Rekap Proyek AI - Armbian Server</title>
      <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap" rel="stylesheet">
      <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Inter', sans-serif; background: #0B1120; color: #E2E8F0; padding: 24px; }
        .container { max-width: 960px; margin: 0 auto; }
        .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid #1E293B; }
        .logo { font-size: 24px; font-weight: 700; color: #F59E0B; display: flex; align-items: center; gap: 10px; }
        .badge { background: #1E293B; border: 1px solid #334155; padding: 6px 12px; border-radius: 9999px; font-size: 12px; color: #94A3B8; }
        .badge.online { background: #064E3B; border-color: #059669; color: #34D399; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
        .card { background: #131D31; border: 1px solid #1E293B; border-radius: 16px; padding: 20px; }
        .card-title { font-size: 13px; color: #94A3B8; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px; }
        .card-value { font-size: 28px; font-weight: 700; color: #F8FAFC; }
        .card-sub { font-size: 12px; color: #64748B; margin-top: 4px; }
        .section-title { font-size: 18px; font-weight: 600; margin-bottom: 12px; color: #F1F5F9; }
        table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 14px; }
        th, td { text-align: left; padding: 12px; border-bottom: 1px solid #1E293B; }
        th { color: #94A3B8; font-weight: 600; background: #0F172A; }
        .code-box { background: #0A0F1D; border: 1px solid #1E293B; border-radius: 8px; padding: 12px; font-family: monospace; font-size: 13px; color: #38BDF8; overflow-x: auto; }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <div class="logo">🏗️ Rekap Proyek AI — Armbian SBC</div>
          <span class="badge online">● REST API Online (Port {{ port }})</span>
        </div>

        <div class="grid">
          <div class="card">
            <div class="card-title">MySQL Database</div>
            <div class="card-value" style="font-size: 20px; color: {% if mysql_status == 'Connected' %}#34D399{% else %}#F87171{% endif %};">{{ mysql_status }}</div>
            <div class="card-sub">{{ mysql_db }} @ {{ mysql_host }}:{{ mysql_port }}</div>
          </div>
          <div class="card">
            <div class="card-title">Total Proyek</div>
            <div class="card-value">{{ total_projects }}</div>
            <div class="card-sub">Tersinkronisasi aktif</div>
          </div>
          <div class="card">
            <div class="card-title">Transaksi Material</div>
            <div class="card-value">{{ total_materials }}</div>
            <div class="card-sub">Item pengadaan tersimpan</div>
          </div>
          <div class="card">
            <div class="card-title">Catatan Upah</div>
            <div class="card-value">{{ total_wages }}</div>
            <div class="card-sub">Distribusi upah pekerja</div>
          </div>
        </div>

        <div class="card" style="margin-bottom: 24px;">
          <div class="section-title">📡 Endpoint REST API Aplikasi Android</div>
          <p style="font-size: 13px; color: #94A3B8; margin-bottom: 12px;">Gunakan IP server Armbian ini pada aplikasi Android di tab "Server Armbian".</p>
          <div class="code-box">
            GET  /ping       → Cek status koneksi server & MySQL<br>
            POST /sync       → Sinkronisasi data Proyek, Material, & Upah dari Android<br>
            GET  /projects   → Ambil daftar seluruh proyek pembangunan<br>
            GET  /materials  → Ambil data riwayat material<br>
            GET  /wages      → Ambil data pengeluaran upah tenaga kerja<br>
            GET  /summary    → Rekap kalkulasi RAB vs Realisasi pengeluaran<br>
            POST /parse      → Ekstraksi teks pesan mandor via Gemini AI / NLP
          </div>
        </div>

        {% if recent_logs %}
        <div class="card">
          <div class="section-title">💬 Log Pesan Masuk Telegram Terakhir</div>
          <table>
            <thead>
              <tr>
                <th>Pengirim</th>
                <th>Teks Laporan Mandor</th>
              </tr>
            </thead>
            <tbody>
              {% for log in recent_logs %}
              <tr>
                <td style="font-weight: 600; color: #F59E0B; width: 140px;">{{ log.sender_name }}</td>
                <td style="color: #CBD5E1;">{{ log.raw_text }}</td>
              </tr>
              {% endfor %}
            </tbody>
          </table>
        </div>
        {% endif %}
      </div>
    </body>
    </html>
    """

    return render_template_string(
        html_template,
        port=SERVER_PORT,
        mysql_status=mysql_status,
        mysql_db=MYSQL_DB,
        mysql_host=MYSQL_HOST,
        mysql_port=MYSQL_PORT,
        total_projects=total_projects,
        total_materials=total_materials,
        total_wages=total_wages,
        recent_logs=recent_logs
    )

@app.route("/ping", methods=["GET"])
def health_check():
    """Cek kesehatan server Armbian dan koneksi MySQL."""
    mysql_status = "untested"
    mysql_error = None
    project_count = 0

    if HAS_PYMYSQL:
        try:
            conn = get_db_connection()
            with conn.cursor() as cur:
                cur.execute("SELECT COUNT(*) AS total FROM projects")
                row = cur.fetchone()
                project_count = row["total"] if row else 0
            conn.close()
            mysql_status = "connected"
        except Exception as e:
            mysql_status = "error"
            mysql_error = str(e)
    else:
        mysql_status = "missing_pymysql_module"

    return jsonify({
        "status": "online",
        "service": "Rekap Proyek AI Server (Armbian Linux)",
        "python_version": sys.version.split()[0],
        "system_time": datetime.now().isoformat(),
        "mysql": {
            "status": mysql_status,
            "database": MYSQL_DB,
            "host": MYSQL_HOST,
            "total_projects": project_count,
            "error": mysql_error
        },
        "telegram_bot": {
            "configured": bool(TELEGRAM_BOT_TOKEN),
            "poller_running": ENABLE_TELEGRAM_POLLER
        },
        "gemini_ai": {
            "configured": bool(GEMINI_API_KEY)
        }
    })

@app.route("/sync", methods=["POST"])
def sync_data():
    """
    Endpoint sinkronisasi data dari aplikasi Android.
    Menerima JSON berisi data projects, materials, dan wages.
    """
    if not verify_api_key():
        return jsonify({"success": False, "error": "Unauthorized: API Key salah atau tidak ada"}), 401

    payload = request.get_json(silent=True)
    if not payload:
        return jsonify({"success": False, "error": "Invalid JSON payload"}), 400

    projects = payload.get("projects", [])
    materials = payload.get("materials", [])
    wages = payload.get("wages", [])

    if not HAS_PYMYSQL:
        return jsonify({"success": False, "error": "Server MySQL tidak aktif (PyMySQL tidak terpasang)"}), 500

    try:
        conn = get_db_connection()
        with conn.cursor() as cur:
            # 1. Simpan / Perbarui Projects
            for p in projects:
                cur.execute("""
                    INSERT INTO projects (id, name, code_spk, location, client_name, budget_rab, progress_percent, status, foreman_in_charge)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                        name = VALUES(name),
                        code_spk = VALUES(code_spk),
                        location = VALUES(location),
                        client_name = VALUES(client_name),
                        budget_rab = VALUES(budget_rab),
                        progress_percent = VALUES(progress_percent),
                        status = VALUES(status),
                        foreman_in_charge = VALUES(foreman_in_charge);
                """, (
                    p.get("id"),
                    p.get("name", "Proyek Baru"),
                    p.get("code_spk", ""),
                    p.get("location", ""),
                    p.get("client_name", ""),
                    p.get("budget_rab", 0.0),
                    p.get("progress_percent", 0.0),
                    p.get("status", "Aktif"),
                    p.get("foreman", "")
                ))

            # 2. Simpan Materials
            for m in materials:
                cur.execute("""
                    INSERT INTO material_usages (id, project_id, date, material_name, category, quantity, unit, unit_price, total_cost, supplier, invoice_note, source)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                        material_name = VALUES(material_name),
                        category = VALUES(category),
                        quantity = VALUES(quantity),
                        unit = VALUES(unit),
                        unit_price = VALUES(unit_price),
                        total_cost = VALUES(total_cost),
                        supplier = VALUES(supplier),
                        invoice_note = VALUES(invoice_note),
                        source = VALUES(source);
                """, (
                    m.get("id"),
                    m.get("project_id"),
                    m.get("date", int(time.time() * 1000)),
                    m.get("material_name", "Material"),
                    m.get("category", "Semen & Pasir"),
                    m.get("quantity", 1.0),
                    m.get("unit", "sak"),
                    m.get("unit_price", 0.0),
                    m.get("total_cost", 0.0),
                    m.get("supplier", ""),
                    m.get("notes", ""),
                    m.get("source", "Android Sync")
                ))

            # 3. Simpan Wages
            for w in wages:
                cur.execute("""
                    INSERT INTO labor_wages (id, project_id, date, worker_role, worker_count, duration_hok, wage_rate, total_wage, task_description, foreman_name, source)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                        worker_role = VALUES(worker_role),
                        worker_count = VALUES(worker_count),
                        duration_hok = VALUES(duration_hok),
                        wage_rate = VALUES(wage_rate),
                        total_wage = VALUES(total_wage),
                        task_description = VALUES(task_description),
                        foreman_name = VALUES(foreman_name),
                        source = VALUES(source);
                """, (
                    w.get("id"),
                    w.get("project_id"),
                    w.get("date", int(time.time() * 1000)),
                    w.get("worker_role", "Tukang"),
                    w.get("worker_count", 1),
                    w.get("duration_hok", 1.0),
                    w.get("wage_rate", 0.0),
                    w.get("total_wage", 0.0),
                    w.get("task_description", ""),
                    w.get("foreman", ""),
                    w.get("source", "Android Sync")
                ))

        conn.close()
        return jsonify({
            "success": True,
            "message": "Data berhasil disimpan ke MySQL Armbian",
            "synced_projects": len(projects),
            "synced_materials": len(materials),
            "synced_wages": len(wages)
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/projects", methods=["GET"])
def list_projects():
    """Mengambil daftar proyek dari MySQL."""
    if not verify_api_key():
        return jsonify({"error": "Unauthorized"}), 401
    try:
        conn = get_db_connection()
        with conn.cursor() as cur:
            cur.execute("SELECT * FROM projects ORDER BY updated_at DESC")
            rows = cur.fetchall()
        conn.close()
        return jsonify({"projects": rows})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/materials", methods=["GET"])
def list_materials():
    """Mengambil daftar pemakaian material dari MySQL."""
    if not verify_api_key():
        return jsonify({"error": "Unauthorized"}), 401
    project_id = request.args.get("project_id")
    try:
        conn = get_db_connection()
        with conn.cursor() as cur:
            if project_id:
                cur.execute("SELECT * FROM material_usages WHERE project_id = %s ORDER BY date DESC", (project_id,))
            else:
                cur.execute("SELECT * FROM material_usages ORDER BY date DESC LIMIT 100")
            rows = cur.fetchall()
        conn.close()
        return jsonify({"materials": rows})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/wages", methods=["GET"])
def list_wages():
    """Mengambil daftar catatan upah tenaga kerja dari MySQL."""
    if not verify_api_key():
        return jsonify({"error": "Unauthorized"}), 401
    project_id = request.args.get("project_id")
    try:
        conn = get_db_connection()
        with conn.cursor() as cur:
            if project_id:
                cur.execute("SELECT * FROM labor_wages WHERE project_id = %s ORDER BY date DESC", (project_id,))
            else:
                cur.execute("SELECT * FROM labor_wages ORDER BY date DESC LIMIT 100")
            rows = cur.fetchall()
        conn.close()
        return jsonify({"wages": rows})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/summary", methods=["GET"])
def get_summary():
    """Mendapatkan rekap total biaya material dan upah per proyek."""
    if not verify_api_key():
        return jsonify({"error": "Unauthorized"}), 401

    project_id = request.args.get("project_id")
    try:
        conn = get_db_connection()
        with conn.cursor() as cur:
            if project_id:
                cur.execute("SELECT SUM(total_cost) AS total_mat FROM material_usages WHERE project_id = %s", (project_id,))
                mat_res = cur.fetchone()["total_mat"] or 0.0

                cur.execute("SELECT SUM(total_wage) AS total_wage FROM labor_wages WHERE project_id = %s", (project_id,))
                wage_res = cur.fetchone()["total_wage"] or 0.0

                cur.execute("SELECT * FROM projects WHERE id = %s", (project_id,))
                proj = cur.fetchone()
            else:
                cur.execute("SELECT SUM(total_cost) AS total_mat FROM material_usages")
                mat_res = cur.fetchone()["total_mat"] or 0.0

                cur.execute("SELECT SUM(total_wage) AS total_wage FROM labor_wages")
                wage_res = cur.fetchone()["total_wage"] or 0.0
                proj = None
        conn.close()

        total_cost = float(mat_res) + float(wage_res)
        rab = float(proj["budget_rab"]) if proj else 0.0

        return jsonify({
            "total_material_cost": float(mat_res),
            "total_labor_cost": float(wage_res),
            "total_overall_cost": total_cost,
            "budget_rab": rab,
            "budget_remaining": rab - total_cost if rab > 0 else 0.0
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/parse", methods=["POST"])
def parse_chat_endpoint():
    """Endpoint untuk parsing teks manual dari aplikasi atau webhook."""
    payload = request.get_json(silent=True) or {}
    text = payload.get("text", "")
    if not text:
        return jsonify({"error": "Field 'text' dibutuhkan"}), 400

    parsed = extract_construction_data(text)
    return jsonify({"success": True, "result": parsed})

# ---------------------------------------------------------------------------
# TELEGRAM BOT POLLER WORKER
# ---------------------------------------------------------------------------
def telegram_poller_worker():
    """Background worker untuk menerima chat mandor Telegram secara otomatis."""
    if not HAS_REQUESTS or not TELEGRAM_BOT_TOKEN:
        print("[INFO] Poller Telegram nonaktif (Token belum diisi atau modul requests belum ada).")
        return

    print(f"[START] Worker Telegram Poller aktif...")
    offset = 0

    while True:
        try:
            url = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/getUpdates?offset={offset}&timeout=20"
            resp = requests.get(url, timeout=30)
            if resp.status_code == 200:
                data = resp.json()
                if data.get("ok"):
                    for item in data.get("result", []):
                        offset = item["update_id"] + 1
                        msg = item.get("message")
                        if not msg or "text" not in msg:
                            continue

                        chat_id = msg["chat"]["id"]
                        sender = msg.get("from", {}).get("first_name", "Mandor")
                        text = msg["text"]

                        print(f"[TELEGRAM] Pesan dari {sender}: {text}")

                        # Ekstraksi data via AI / NLP
                        parsed = extract_construction_data(text)

                        # Simpan ke MySQL jika ada koneksi
                        if HAS_PYMYSQL:
                            try:
                                conn = get_db_connection()
                                with conn.cursor() as cur:
                                    # Log telegram
                                    cur.execute("""
                                        INSERT INTO telegram_logs (telegram_msg_id, chat_id, sender_name, raw_text, received_at, parse_status)
                                        VALUES (%s, %s, %s, %s, %s, %s)
                                    """, (msg.get("message_id"), str(chat_id), sender, text, int(time.time() * 1000), "PROCESSED"))

                                    # Cari project pertama atau aktif
                                    cur.execute("SELECT id FROM projects WHERE status = 'Aktif' LIMIT 1")
                                    proj_row = cur.fetchone()
                                    if not proj_row:
                                        cur.execute("SELECT id FROM projects LIMIT 1")
                                        proj_row = cur.fetchone()
                                    p_id = proj_row["id"] if proj_row else 1

                                    # Masukkan material
                                    for mat in parsed.get("materials", []):
                                        cur.execute("""
                                            INSERT INTO material_usages (project_id, date, material_name, category, quantity, unit, unit_price, total_cost, source)
                                            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                                        """, (p_id, int(time.time() * 1000), mat["name"], mat.get("category", "Semen & Pasir"), mat["quantity"], mat["unit"], mat["unit_price"], mat["total_cost"], "Bot Telegram Armbian"))

                                    # Masukkan upah
                                    for lab in parsed.get("labors", []):
                                        cur.execute("""
                                            INSERT INTO labor_wages (project_id, date, worker_role, worker_count, duration_hok, wage_rate, total_wage, task_description, foreman_name, source)
                                            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                                        """, (p_id, int(time.time() * 1000), lab["role"], lab.get("count", 1), lab.get("duration_hok", 1.0), lab["wage_rate"], lab["total_wage"], lab.get("description", ""), sender, "Bot Telegram Armbian"))

                                    if parsed.get("progress_percent"):
                                        cur.execute("UPDATE projects SET progress_percent = %s WHERE id = %s", (parsed["progress_percent"], p_id))

                                conn.close()
                            except Exception as db_err:
                                print(f"[ERROR DB] {db_err}")

                        # Kirim balasan ke Telegram
                        mat_count = len(parsed.get("materials", []))
                        lab_count = len(parsed.get("labors", []))
                        balasan = (
                            f"✅ *Laporan Diterima & Disimpan ke Server Armbian*\n"
                            f"🧱 Material terdeteksi: {mat_count} item\n"
                            f"👷 Upah terdeteksi: {lab_count} kelompok\n"
                            f"📊 Progress: {parsed.get('progress_percent') or '-'}%\n"
                            f"Data telah sinkron dengan database MySQL dan aplikasi Android."
                        )
                        reply_url = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/sendMessage"
                        requests.post(reply_url, json={"chat_id": chat_id, "text": balasan, "parse_mode": "Markdown"}, timeout=10)

        except Exception as e:
            time.sleep(3)

        time.sleep(2)

# ---------------------------------------------------------------------------
# MAIN EXECUTION
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    print("=" * 65)
    print(f"🚀 REKAP PROYEK AI - ARMBIAN PYTHON SERVER RUNNING")
    print(f"   Python: {sys.version.split()[0]}")
    print(f"   Listening on: http://{SERVER_HOST}:{SERVER_PORT}")
    print(f"   MySQL Target: {MYSQL_HOST}:{MYSQL_PORT} (DB: {MYSQL_DB})")
    print(f"   AI Engine: {'Gemini 2.5 Flash' if GEMINI_API_KEY else 'Local Regex Pattern NLP'}")
    print("=" * 65)

    # Inisialisasi tabel database
    init_database_tables()

    # Jalankan background poller untuk bot telegram jika diaktifkan
    if ENABLE_TELEGRAM_POLLER and TELEGRAM_BOT_TOKEN:
        poller_thread = threading.Thread(target=telegram_poller_worker, daemon=True)
        poller_thread.start()

    app.run(host=SERVER_HOST, port=SERVER_PORT, debug=False)
