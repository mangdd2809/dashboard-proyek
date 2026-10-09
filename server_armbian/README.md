# Panduan Deploy Server Armbian & MySQL — Rekap Proyek AI

Panduan lengkap untuk menjalankan backend server di SBC (Single Board Computer) berbasis **Armbian Linux** (seperti Orange Pi, Raspberry Pi, NanoPi, Banana Pi, dll) yang terhubung dengan **Database MySQL**, **Bot Telegram**, **Gemini AI**, dan **Aplikasi Android**.

---

## 1. Persyaratan Sistem Armbian
- **OS**: Armbian (Debian 11/12 Bullseye/Bookworm atau Ubuntu 22.04/24.04 LTS)
- **Python**: Versi 3.8, 3.9, 3.10, 3.11, atau 3.12 (`python3 --version`)
- **Database**: MariaDB Server / MySQL Server 8.0+
- **Jaringan**: Terhubung satu jaringan Wi-Fi/LAN dengan HP Android, atau memiliki IP statis / port forwarding / Cloudflare Tunnel / Tailscale.

---

## 2. Cara Cepat Instalasi (1-Langkah)

1. Salin folder `server_armbian` ini ke server Armbian Anda (misal ke `/opt/rekap-proyek` atau `/home/armbian/rekap-proyek`).
2. Masuk ke folder tersebut dan jalankan skrip otomatis:
   ```bash
   cd server_armbian
   sudo bash setup_armbian.sh
   ```
3. Skrip akan secara otomatis:
   - Memperbarui paket sistem `apt`
   - Memasang `python3`, `python3-pip`, `mariadb-server`
   - Memasang pustaka Python: `flask`, `pymysql`, `cryptography`, `requests`, `python-dotenv`, `gunicorn`
   - Menginisialisasi database `db_rekap_konstruksi` dan mengeksekusi `schema.sql`
   - Membuat service background `systemd` (`rekap-proyek.service`) agar server otomatis hidup saat Armbian menyala atau mati lampu/reboot.

---

## 3. Konfigurasi Lingkungan (`.env`)

Buat file `.env` di dalam folder `server_armbian`:
```bash
nano .env
```
Isi konfigurasi berikut:
```env
# Port & Host
SERVER_HOST=0.0.0.0
SERVER_PORT=8000
API_KEY=armbian_secret_token_2026

# MySQL Armbian
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=
MYSQL_DB=db_rekap_konstruksi

# Bot Telegram (Diperoleh dari @BotFather di Telegram)
TELEGRAM_BOT_TOKEN=123456789:ABCdefGhIJKlmNoPQRstuVWxyz
TELEGRAM_CHAT_ID=-1001234567890

# Google Gemini AI (Opsional untuk analisa NLP cerdas langsung di Armbian)
GEMINI_API_KEY=AIzaSy...

# Jalankan poller otomatis Telegram di latar belakang (true/false)
ENABLE_TELEGRAM_POLLER=true
```

---

## 4. Perintah Manajemen Service Armbian

- **Mengecek status server:**
  ```bash
  sudo systemctl status rekap-proyek
  ```
- **Merestart server setelah ubah .env:**
  ```bash
  sudo systemctl restart rekap-proyek
  ```
- **Melihat log real-time Telegram & sync:**
  ```bash
  sudo journalctl -u rekap-proyek -f
  ```
- **Menjalankan manual untuk debug:**
  ```bash
  python3 app.py
  ```

---

## 5. Menghubungkan Aplikasi Android ke Server Armbian

1. Buka aplikasi **Rekap Proyek AI** di smartphone Android Anda.
2. Ketuk ikon server di pojok kanan atas atau buka menu navigasi **Server Armbian**.
3. Masukkan IP Armbian Anda, contoh: `http://192.168.1.100:8000`.
4. Masukkan API Key: `armbian_secret_token_2026` (sesuai yang diatur di `.env`).
5. Ketuk **"Tes Koneksi"** hingga muncul status online dan latensi.
6. Ketuk **"Sinkronisasi Data"** untuk menyinkronkan seluruh proyek, material, dan upah ke MySQL Armbian secara instan!
