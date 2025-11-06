# ORCA System Monitor - Desktop App for Ubuntu

Ứng dụng quản lý bộ nhớ hệ thống Linux với giao diện hiện đại.

## Kiến trúc

- **Backend**: Java Spring Boot REST API (sử dụng OSHI để đọc thông tin hệ thống)
- **Frontend**: React + TypeScript + Vite + shadcn/ui
- **Desktop**: Tauri (đóng gói thành native app cho Ubuntu)

## Yêu cầu

### Để chạy Backend
- Java 17+
- Maven 3.6+
- Ubuntu/Linux (để đọc được /proc filesystem)

### Để chạy Frontend
- Node.js 18+
- npm hoặc yarn

### Để build Desktop App
- Rust (cho Tauri)
- Tất cả dependencies của Backend và Frontend

## Hướng dẫn cài đặt

### 1. Cài đặt dependencies

```bash
# Cài đặt Java và Maven (nếu chưa có)
sudo apt update
sudo apt install openjdk-17-jdk maven

# Cài đặt Node.js (nếu chưa có)
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# Cài đặt Rust và Tauri dependencies
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
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

### 2. Build và chạy Backend

```bash
# Build Java backend
mvn clean package

# Chạy Spring Boot server
java -jar target/PBL4_vr2-1.0-SNAPSHOT.jar
# hoặc
mvn spring-boot:run
```

Backend sẽ chạy tại: `http://localhost:8080`

### 3. Build và chạy Frontend

```bash
cd "Memory Management App Design"

# Cài đặt dependencies
npm install

# Chạy development server
npm run dev
```

Frontend sẽ chạy tại: `http://localhost:5173`

### 4. Build Desktop App với Tauri

```bash
cd "Memory Management App Design"

# Cài đặt Tauri CLI
npm install -D @tauri-apps/cli

# Build desktop app
npm run tauri build
```

File .deb sẽ được tạo trong thư mục `src-tauri/target/release/bundle/deb/`

## Chạy ứng dụng

### Development Mode

1. Chạy Backend:
```bash
mvn spring-boot:run
```

2. Trong terminal khác, chạy Frontend:
```bash
cd "Memory Management App Design"
npm run dev
```

3. Mở browser tại `http://localhost:5173`

### Production Mode (Desktop App)

1. Chạy Backend:
```bash
java -jar target/PBL4_vr2-1.0-SNAPSHOT.jar
```

2. Chạy Desktop App:
```bash
cd "Memory Management App Design"
npm run tauri dev
```

## API Endpoints

- `GET /api/health` - Health check
- `GET /api/memory` - Thông tin memory
- `GET /api/cpu` - Thông tin CPU
- `GET /api/processes` - Danh sách processes
- `GET /api/disk` - Thông tin disk
- `GET /api/network` - Thông tin network
- `GET /api/system` - Toàn bộ thông tin hệ thống

## Tính năng

- ✅ Hiển thị thông tin Memory (RAM, Swap, Cached, Available)
- ✅ Biểu đồ thời gian thực
- ✅ Danh sách processes
- ✅ Tự động cập nhật mỗi 5 giây
- ✅ Giao diện đẹp, hiện đại với shadcn/ui
- ✅ Desktop app native cho Ubuntu

## Cấu trúc thư mục

```
PBL4-linux-hdh-/
├── src/main/java/com/orca/pbl4/     # Java Backend
│   ├── App.java                     # Spring Boot entry point
│   ├── api/                         # REST Controllers
│   │   └── SystemMonitorController.java
│   └── core/                        # Core logic
│       ├── model/                   # Data models
│       └── system/                  # System readers
│
├── Memory Management App Design/    # Frontend
│   ├── src/
│   │   ├── App.tsx                  # Main React component
│   │   ├── services/api.ts          # API client
│   │   └── components/              # UI components
│   └── package.json
│
├── pom.xml                          # Maven config
└── README.md                        # This file
```

## Troubleshooting

### Backend không khởi động được
- Kiểm tra Java version: `java -version` (cần >= 17)
- Kiểm tra port 8080 đã được sử dụng chưa: `sudo lsof -i :8080`

### Frontend không kết nối được Backend
- Đảm bảo Backend đang chạy tại `http://localhost:8080`
- Kiểm tra CORS settings trong `application.properties`

### Không đọc được thông tin hệ thống
- Ứng dụng cần chạy trên Linux để đọc được `/proc` filesystem
- Một số thông tin có thể cần quyền sudo

## License

MIT
