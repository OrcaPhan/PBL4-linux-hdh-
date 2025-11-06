# Hướng dẫn Setup Nhanh - ORCA System Monitor

## Phương pháp đơn giản nhất (không cần Tauri)

### Bước 1: Cài đặt dependencies

```bash
# Cài đặt Java 17
sudo apt update
sudo apt install openjdk-17-jdk maven

# Cài đặt Node.js
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs
```

### Bước 2: Chạy ứng dụng

```bash
# Cho phép script chạy được
chmod +x start.sh

# Chạy script
./start.sh
```

Script sẽ tự động:
1. Build Java backend
2. Cài đặt npm dependencies
3. Khởi động Backend (port 8080)
4. Khởi động Frontend (port 5173)

Mở browser tại: **http://localhost:5173**

Để dừng: nhấn `Ctrl+C`

---

## Phương pháp 2: Build Desktop App với Tauri (nâng cao)

### Bước 1: Cài đặt thêm Rust và Tauri dependencies

```bash
# Cài đặt Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source $HOME/.cargo/env

# Cài đặt Tauri dependencies
sudo apt install libwebkit2gtk-4.0-dev \
    build-essential \
    curl \
    wget \
    file \
    libssl-dev \
    libgtk-3-dev \
    libayatana-appindicator3-dev \
    librsvg2-dev
```

### Bước 2: Build Desktop App

```bash
# Cho phép script chạy được
chmod +x build-desktop.sh

# Build
./build-desktop.sh
```

### Bước 3: Cài đặt .deb package

```bash
# Tìm file .deb vừa build
cd "Memory Management App Design/src-tauri/target/release/bundle/deb/"

# Cài đặt
sudo dpkg -i orca-system-monitor_*.deb
```

---

## Chạy Manual (nếu script không hoạt động)

### Terminal 1 - Backend:
```bash
mvn clean package
java -jar target/PBL4_vr2-1.0-SNAPSHOT.jar
```

### Terminal 2 - Frontend:
```bash
cd "Memory Management App Design"
npm install
npm run dev
```

Mở browser: http://localhost:5173

---

## Troubleshooting

### Lỗi: "Port 8080 already in use"
```bash
# Tìm process đang dùng port 8080
sudo lsof -i :8080

# Kill process đó
sudo kill -9 <PID>
```

### Lỗi: "Cannot connect to backend"
- Kiểm tra Backend đang chạy: `curl http://localhost:8080/api/health`
- Xem log Backend: `cat /tmp/orca-backend.log`

### Lỗi: Maven build failed
```bash
# Cài đặt lại Maven
sudo apt remove maven
sudo apt install maven
```

### Lỗi: npm install failed
```bash
# Xóa node_modules và build lại
cd "Memory Management App Design"
rm -rf node_modules package-lock.json
npm install
```

---

## Các API Endpoints

Test bằng curl:

```bash
# Health check
curl http://localhost:8080/api/health

# Memory info
curl http://localhost:8080/api/memory | jq

# CPU info
curl http://localhost:8080/api/cpu | jq

# Processes
curl http://localhost:8080/api/processes | jq

# Full system snapshot
curl http://localhost:8080/api/system | jq
```

---

## Development Tips

### Hot reload Frontend
Khi chỉnh sửa code React/TypeScript, Vite sẽ tự động reload browser.

### Hot reload Backend
Nếu sửa Java code:
1. Dừng Backend (Ctrl+C trong terminal Backend)
2. Build lại: `mvn clean package`
3. Chạy lại: `java -jar target/PBL4_vr2-1.0-SNAPSHOT.jar`

Hoặc dùng Spring Boot DevTools để auto-reload.

---

## Chạy trên Windows (Development only)

**Lưu ý:** Backend chỉ hoạt động đầy đủ trên Linux vì đọc `/proc` filesystem.

Trên Windows, backend sẽ trả về mock data hoặc lỗi. Bạn có thể:
1. Dùng WSL2 (Windows Subsystem for Linux)
2. Dùng Docker với Ubuntu container
3. Hoặc test Frontend với mock data

---

## Next Steps

1. ✅ Backend API đã sẵn sàng
2. ✅ Frontend đã tích hợp với Backend
3. ⏳ Tùy chỉnh thêm: Cài đặt auto-start, themes, etc.
4. ⏳ Deploy: Đóng gói .deb hoặc AppImage

Chúc bạn thành công! 🚀
