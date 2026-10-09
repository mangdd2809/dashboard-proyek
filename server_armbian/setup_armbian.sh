#!/bin/bash
# =============================================================================
# SCRIPT OTOMATIS SETUP PYTHON & MYSQL DI SERVER ARMBIAN (LINUX SBC)
# Jalankan dengan: sudo bash setup_armbian.sh
# =============================================================================

set -e

echo "=========================================================="
echo "🔧 MEMULAI SETUP PYTHON & MYSQL DI ARMBIAN..."
echo "=========================================================="

# 1. Update paket sistem
echo "[1/5] Memperbarui repositori apt..."
apt update -y

# 2. Install Python3, pip, MariaDB / MySQL Server
echo "[2/5] Menginstal Python3, Pip, dan MariaDB Server..."
apt install -y python3 python3-pip python3-venv mariadb-server mariadb-client curl git

# 3. Verifikasi versi Python
echo "[3/5] Memeriksa versi Python:"
python3 --version
pip3 --version

# 4. Install dependensi Python
echo "[4/5] Memasang library Python yang dibutuhkan..."
pip3 install flask pymysql cryptography requests python-dotenv gunicorn --break-system-packages 2>/dev/null || pip3 install flask pymysql cryptography requests python-dotenv gunicorn

# 5. Inisialisasi Database MariaDB / MySQL
echo "[5/5] Menyiapkan database MySQL..."
systemctl start mariadb || systemctl start mysql
mariadb -u root -e "CREATE DATABASE IF NOT EXISTS \`db_rekap_konstruksi\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "=========================================================="
echo "✅ SETUP SELESAI!"
echo "Untuk menjalankan server secara manual:"
echo "   python3 app.py"
echo ""
echo "Untuk menguji koneksi dari terminal:"
echo "   curl http://localhost:8000/ping"
echo "=========================================================="
