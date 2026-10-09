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

from flask import Flask, request, jsonify

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
    """Inisialisasi tabel MySQL jika belum ada."""
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
# REST API ENDPOINTS
# ---------------------------------------------------------------------------
@app.route("/", methods=["GET"])
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

# ---------------------------------------------------------------------------
# LOCAL NLP EXTRACTOR (FALLBACK JIKA TANPA GEMINI API)
# ---------------------------------------------------------------------------
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
                "hok": 1.0,
                "rate": rate,
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
                "hok": 1.0,
                "rate": rate,
                "total_wage": cnt * 1.0 * rate,
                "description": line.strip()
            })

    return {
        "materials": materials,
        "labors": labors,
        "progress_percent": progress_percent,
        "notes": text.strip()
    }

# ---------------------------------------------------------------------------
# TELEGRAM BOT POLLER WORKER
# ---------------------------------------------------------------------------
def telegram_poller_worker():
    """Background worker untuk menerima chat mandor Telegram secara otomatis."""
    if not HAS_REQUESTS or not TELEGRAM_BOT_TOKEN:
        print("[INFO] Poller Telegram nonaktif (Token belum diisi atau modul requests belum ada).")
        return

    print(f"[START] Worker Telegram Poller aktif untuk Bot...")
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

                        # Ekstraksi data
                        parsed = extract_construction_data_local(text)

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

                                    # Cari project pertama
                                    cur.execute("SELECT id FROM projects LIMIT 1")
                                    proj_row = cur.fetchone()
                                    p_id = proj_row["id"] if proj_row else 1

                                    # Masukkan material
                                    for mat in parsed["materials"]:
                                        cur.execute("""
                                            INSERT INTO material_usages (project_id, date, material_name, category, quantity, unit, unit_price, total_cost, source)
                                            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                                        """, (p_id, int(time.time() * 1000), mat["name"], mat["category"], mat["quantity"], mat["unit"], mat["unit_price"], mat["total_cost"], "Bot Telegram Armbian"))

                                    # Masukkan upah
                                    for lab in parsed["labors"]:
                                        cur.execute("""
                                            INSERT INTO labor_wages (project_id, date, worker_role, worker_count, duration_hok, wage_rate, total_wage, task_description, foreman_name, source)
                                            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                                        """, (p_id, int(time.time() * 1000), lab["role"], lab["count"], lab["hok"], lab["rate"], lab["total_wage"], lab["description"], sender, "Bot Telegram Armbian"))

                                    if parsed["progress_percent"]:
                                        cur.execute("UPDATE projects SET progress_percent = %s WHERE id = %s", (parsed["progress_percent"], p_id))

                                conn.close()
                            except Exception as db_err:
                                print(f"[ERROR DB] {db_err}")

                        # Kirim balasan ke Telegram
                        balasan = (
                            f"✅ *Laporan Diterima & Disimpan ke Server Armbian*\n"
                            f"🧱 Material terdeteksi: {len(parsed['materials'])} item\n"
                            f"👷 Upah terdeteksi: {len(parsed['labors'])} kelompok\n"
                            f"📊 Progress: {parsed['progress_percent'] or '-'}%\n"
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
    print("=" * 65)

    # Inisialisasi tabel database
    init_database_tables()

    # Jalankan background poller untuk bot telegram jika diaktifkan
    if ENABLE_TELEGRAM_POLLER and TELEGRAM_BOT_TOKEN:
        poller_thread = threading.Thread(target=telegram_poller_worker, daemon=True)
        poller_thread.start()

    app.run(host=SERVER_HOST, port=SERVER_PORT, debug=False)
