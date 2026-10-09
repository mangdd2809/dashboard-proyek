# 📖 PANDUAN LENGKAP SERVER ARMBIAN (PYTHON & MYSQL BACKEND)
### Aplikasi Rekap Pemakaian Material & Upah Proyek Konstruksi (AI & Telegram)

Panduan ini berisi langkah demi langkah untuk memeriksa, mengonfigurasi, dan menjalankan server Python serta database MySQL pada Single Board Computer (SBC) seperti Orange Pi, Raspberry Pi, Banana Pi, atau Khadas yang menjalankan OS **Armbian (Debian/Ubuntu)**.

---

## 1. 🔍 Cara Cek Ulang Python di Server Armbian

Masuk ke terminal server Armbian via SSH atau keyboard langsung:

```bash
# 1. Cek apakah Python 3 sudah terpasang
python3 --version

# Output yang diharapkan: Python 3.10.x atau Python 3.11.x / 3.12.x

# 2. Cek lokasi binary Python
which python3

# 3. Cek apakah PIP (Package Installer) sudah ada
pip3 --version
```

Jika Python belum ada atau pip belum terpasang, jalankan:
```bash
sudo apt update
sudo apt install -y python3 python3-pip python3-venv mariadb-server mariadb-client curl
```

---

## 2. 🗄️ Menyiapkan Database MySQL / MariaDB di Armbian

Armbian menggunakan MariaDB (kompatibel penuh 100% dengan MySQL):

```bash
# Pastikan MariaDB berjalan
sudo systemctl enable --now mariadb

# Masuk ke MySQL console
sudo mariadb -u root
```

Di dalam konsol MySQL/MariaDB:
```sql
CREATE DATABASE IF NOT EXISTS `db_rekap_konstruksi` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- (Opsional) Buat user khusus agar aplikasi tidak memakai root:
CREATE USER IF NOT EXISTS 'mandor'@'%' IDENTIFIED BY 'rahasia123';
GRANT ALL PRIVILEGES ON db_rekap_konstruksi.* TO 'mandor'@'%';
FLUSH PRIVILEGES;
EXIT;
```

---

## 3. 📦 Memasang Library Python

Pindah ke direktori proyek server di Armbian (misal `/opt/rekap_server`):

```bash
sudo mkdir -p /opt/rekap_server
cd /opt/rekap_server

# Install dependensi yang dibutuhkan
pip3 install flask pymysql cryptography requests python-dotenv gunicorn --break-system-packages
```

---

## 4. 🚀 Menjalankan Server Python

Jalankan script `app.py`:

```bash
python3 app.py
```

Output di terminal Armbian akan tampak seperti ini:
```
=================================================================
🚀 REKAP PROYEK AI - ARMBIAN PYTHON SERVER RUNNING
   Python: 3.11.2
   Listening on: http://0.0.0.0:8000
   MySQL Target: localhost:3306 (DB: db_rekap_konstruksi)
=================================================================
[OK] Database MySQL `db_rekap_konstruksi` dan tabel berhasil diinisialisasi di Armbian.
```

---

## 5. 🧪 Menguji Koneksi (Health Check)

Buka jendela terminal lain di Armbian atau komputer satu jaringan WiFi:

```bash
# Tes ping status
curl http://localhost:8000/ping
```

Hasil JSON yang diharapkan:
```json
{
  "status": "online",
  "service": "Rekap Proyek AI Server (Armbian Linux)",
  "python_version": "3.11.2",
  "mysql": {
    "status": "connected",
    "database": "db_rekap_konstruksi",
    "total_projects": 3
  }
}
```

---

## 6. ⚙️ Menjalankan Otomatis di Background (Systemd Service)

Agar server Python otomatis menyala saat Armbian dinyalakan (*auto-boot*):

```bash
# 1. Salin file service
sudo cp rekap-server.service /etc/systemd/system/

# 2. Reload daemon dan aktifkan
sudo systemctl daemon-reload
sudo systemctl enable --now rekap-server

# 3. Cek status service
sudo systemctl status rekap-server

# 4. Cek log jika ada kendala
journalctl -u rekap-server -f
```

---

## 7. 📱 Menghubungkan ke Aplikasi Android

1. Buka aplikasi **Rekap Proyek AI** di smartphone Android Anda.
2. Masuk ke menu **Server Armbian MySQL** (Ikon server di pojok kanan atas).
3. Masukkan URL IP Armbian Anda (contoh: `http://192.168.1.100:8000`).
4. Klik tombol **"Tes Koneksi"**.
5. Jika berhasil, klik **"Sinkronisasi Data"** untuk menyalin data lokal ke MySQL Armbian!
