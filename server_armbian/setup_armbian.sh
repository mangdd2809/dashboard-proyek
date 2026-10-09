#!/bin/bash
# =============================================================================
# SCRIPT OTOMATIS SETUP PYTHON, SYSTEMD & MYSQL DI SERVER ARMBIAN (LINUX SBC)
# Jalankan dengan: sudo bash setup_armbian.sh
# =============================================================================

set -e

echo "=========================================================="
echo "🔧 MEMULAI SETUP PYTHON & MYSQL DI ARMBIAN SERVER..."
echo "=========================================================="

# 1. Update paket sistem
echo "[1/6] Memperbarui repositori apt..."
apt update -y

# 2. Install Python3, pip, MariaDB / MySQL Server, curl, git
echo "[2/6] Menginstal Python3, Pip, dan MariaDB Server..."
apt install -y python3 python3-pip python3-venv mariadb-server mariadb-client curl git

# 3. Verifikasi versi Python
echo "[3/6] Memeriksa versi Python di Armbian:"
python3 --version
pip3 --version

# 4. Install dependensi Python (Flask, PyMySQL, Cryptography, Requests, dotenv, gunicorn)
echo "[4/6] Memasang library Python yang dibutuhkan..."
pip3 install flask pymysql cryptography requests python-dotenv gunicorn --break-system-packages 2>/dev/null || pip3 install flask pymysql cryptography requests python-dotenv gunicorn

# 5. Inisialisasi Database MariaDB / MySQL
echo "[5/6] Menyiapkan database MySQL (db_rekap_konstruksi)..."
systemctl start mariadb || systemctl start mysql
systemctl enable mariadb || systemctl enable mysql

mariadb -u root -e "CREATE DATABASE IF NOT EXISTS \`db_rekap_konstruksi\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Buat file schema jika ada
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/schema.sql" ]; then
    mariadb -u root db_rekap_konstruksi < "$SCRIPT_DIR/schema.sql"
    echo "    -> Schema tabel schema.sql berhasil dieksekusi!"
fi

# 6. Pasang Systemd Service agar server berjalan otomatis saat Armbian booting
echo "[6/6] Mendaftarkan systemd service 'rekap-proyek'..."
SERVICE_FILE="/etc/systemd/system/rekap-proyek.service"
cat <<EOF > "$SERVICE_FILE"
[Unit]
Description=Rekap Proyek AI Server (Armbian Linux)
After=network.target mariadb.service mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=$SCRIPT_DIR
ExecStart=/usr/bin/python3 $SCRIPT_DIR/app.py
Restart=always
RestartSec=5
Environment=PYTHONUNBUFFERED=1

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable rekap-proyek
systemctl restart rekap-proyek

echo "=========================================================="
echo "✅ SETUP SELESAI & SERVICE BERJALAN OTOMATIS!"
echo "Status Service Armbian:"
systemctl status rekap-proyek --no-pager || true
echo ""
echo "Untuk menguji koneksi dari terminal:"
echo "   curl http://localhost:8000/ping"
echo "Buka di browser PC / Laptop:"
echo "   http://<IP_ARMBIAN>:8000"
echo "=========================================================="
