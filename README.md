# ORCA System Monitor

Ứng dụng giám sát hệ thống Linux với giao diện web hiện đại, hiển thị thông tin về Memory, CPU, Processes, Disk và Network theo thời gian thực.

## 📋 Mục lục

- [Giới thiệu](#giới-thiệu)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cài đặt](#cài-đặt)
- [Hướng dẫn sử dụng](#hướng-dẫn-sử-dụng)
- [API Documentation](#api-documentation)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Tính năng](#tính-năng)
- [Xử lý sự cố](#xử-lý-sự-cố)

## 🎯 Giới thiệu

ORCA System Monitor là ứng dụng giám sát hệ thống Linux được xây dựng với kiến trúc client-server:

- **Backend**: REST API được xây dựng bằng Java Spring Boot, sử dụng thư viện OSHI để thu thập thông tin hệ thống
- **Frontend**: Giao diện web responsive được xây dựng với React, TypeScript, Vite và shadcn/ui

Ứng dụng cung cấp giao diện trực quan để theo dõi tài nguyên hệ thống, giúp người dùng dễ dàng quản lý và phân tích hiệu suất máy tính.

## 🛠️ Công nghệ sử dụng

### Backend
- **Java 17** - Ngôn ngữ lập trình
- **Spring Boot 3.x** - Framework backend
- **OSHI (Operating System & Hardware Information)** - Thư viện đọc thông tin hệ thống
- **Maven** - Build tool và dependency management

### Frontend
- **React 18** - UI library
- **TypeScript** - Type-safe JavaScript
- **Vite** - Build tool và dev server
- **shadcn/ui** - UI component library
- **Recharts** - Thư viện vẽ biểu đồ
- **Tailwind CSS** - Styling framework

## 💻 Yêu cầu hệ thống

### Hệ điều hành
- **Ubuntu 20.04+** hoặc các distro Linux khác (ứng dụng cần truy cập `/proc` filesystem)
- Windows và macOS có thể chạy được nhưng một số tính năng có thể bị giới hạn

### Phần mềm cần thiết

#### Backend
- Java Development Kit (JDK) 17 trở lên
- Maven 3.6 trở lên

#### Frontend
- Node.js 18 trở lên
- npm hoặc yarn

## 📦 Cài đặt

### 1. Cài đặt Java và Maven

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk maven

# Kiểm tra cài đặt
java -version
mvn -version
```

### 2. Cài đặt Node.js và npm

```bash
# Ubuntu/Debian - Sử dụng NodeSource
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# Kiểm tra cài đặt
node -v
npm -v
```

### 3. Clone repository

```bash
git clone <repository-url>
cd PBL4-linux-hdh-
```

### 4. Cài đặt dependencies

#### Cách 1: Sử dụng script tự động (Khuyến nghị)

Script `setup.sh` sẽ tự động cài đặt tất cả các dependencies cần thiết:

```bash
# Cấp quyền thực thi cho script
chmod +x setup.sh

# Chạy script setup
./setup.sh
```

Script này sẽ:
- ✅ Cài đặt Java 17
- ✅ Cài đặt Maven
- ✅ Cài đặt Node.js 18 và npm
- ✅ Cài đặt Git (nếu chưa có)
- ✅ Cài đặt dependencies cho Backend
- ✅ Cài đặt dependencies cho Frontend
- ✅ Kiểm tra và xác nhận tất cả cài đặt

#### Cách 2: Cài đặt thủ công

**Backend:**
```bash
cd backend
mvn clean install
cd ..
```

**Frontend:**
```bash
cd frontend
npm install
cd ..
```

## 🚀 Hướng dẫn sử dụng

### Chạy ứng dụng trong môi trường Development

#### Cách 1: Chạy từng phần riêng biệt

**Bước 1: Khởi động Backend**

```bash
cd backend
mvn spring-boot:run
```

Backend sẽ chạy tại `http://localhost:8080`

**Bước 2: Khởi động Frontend** (mở terminal mới)

```bash
cd frontend
npm run dev
```

Frontend sẽ chạy tại `http://localhost:5173`

**Bước 3: Truy cập ứng dụng**

Mở trình duyệt và truy cập: `http://localhost:5173`

#### Cách 2: Sử dụng script (nếu có)

```bash
# Chạy cả backend và frontend
./start-dev.sh
```

### Build Production

#### Build Backend

```bash
cd backend
mvn clean package

# File JAR sẽ được tạo tại: target/PBL4_vr2-1.0-SNAPSHOT.jar
```

#### Build Frontend

```bash
cd frontend
npm run build

# File build sẽ được tạo trong thư mục: dist/
```

### Chạy Production Build

**Backend:**
```bash
cd backend
java -jar target/PBL4_vr2-1.0-SNAPSHOT.jar
```

**Frontend:**
```bash
cd frontend
npm run preview
# Hoặc deploy thư mục dist/ lên web server
```

## 📡 API Documentation

Backend cung cấp các REST API endpoints sau:

### Health Check
```
GET /api/health
```
Kiểm tra trạng thái server

**Response:**
```json
{
  "status": "UP"
}
```

### Memory Information
```
GET /api/memory
```
Lấy thông tin về bộ nhớ hệ thống

**Response:**
```json
{
  "total": 16777216000,
  "available": 8388608000,
  "used": 8388608000,
  "usagePercent": 50.0,
  "cached": 2097152000,
  "swapTotal": 4194304000,
  "swapUsed": 1048576000
}
```

### CPU Information
```
GET /api/cpu
```
Lấy thông tin về CPU

**Response:**
```json
{
  "name": "Intel Core i7-9750H",
  "cores": 6,
  "threads": 12,
  "usage": 45.5,
  "frequency": 2600.0
}
```

### Process List
```
GET /api/processes
```
Lấy danh sách các process đang chạy

**Response:**
```json
[
  {
    "pid": 1234,
    "name": "chrome",
    "cpuUsage": 5.2,
    "memoryUsage": 524288000,
    "user": "username",
    "state": "RUNNING"
  }
]
```

### Disk Information
```
GET /api/disk
```
Lấy thông tin về ổ đĩa

### Network Information
```
GET /api/network
```
Lấy thông tin về network interfaces

### System Overview
```
GET /api/system
```
Lấy toàn bộ thông tin hệ thống (tổng hợp)

## 📁 Cấu trúc dự án

```
PBL4-linux-hdh-/
├── backend/                          # Backend Java Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/orca/pbl4/
│   │   │   │       ├── App.java              # Spring Boot entry point
│   │   │   │       ├── api/                  # REST Controllers
│   │   │   │       │   └── SystemMonitorController.java
│   │   │   │       ├── core/
│   │   │   │       │   ├── model/            # Data models (DTO)
│   │   │   │       │   ├── system/           # System info readers
│   │   │   │       │   └── util/             # Utilities
│   │   │   │       └── ui/                   # UI components (legacy)
│   │   │   └── resources/
│   │   │       └── application.properties    # Spring configuration
│   │   └── test/                             # Unit tests
│   ├── pom.xml                               # Maven configuration
│   └── target/                               # Build output
│
├── frontend/                         # Frontend React TypeScript
│   ├── src/
│   │   ├── App.tsx                           # Main React component
│   │   ├── main.tsx                          # Entry point
│   │   ├── components/                       # React components
│   │   │   ├── MemoryChart.tsx               # Memory chart component
│   │   │   ├── MemoryOverview.tsx            # Memory overview
│   │   │   ├── ProcessTable.tsx              # Process table
│   │   │   └── ui/                           # shadcn/ui components
│   │   ├── services/
│   │   │   └── api.ts                        # API client
│   │   └── styles/
│   │       └── globals.css                   # Global styles
│   ├── package.json                          # npm dependencies
│   ├── vite.config.ts                        # Vite configuration
│   └── index.html                            # HTML template
│
├── .gitignore                        # Git ignore rules
└── README.md                         # Tài liệu này
```

## ✨ Tính năng

### Giám sát Memory
- ✅ Hiển thị tổng memory, memory đang sử dụng, memory available
- ✅ Hiển thị thông tin về Cached memory và Swap
- ✅ Biểu đồ thời gian thực cho việc sử dụng memory
- ✅ Cập nhật tự động mỗi 5 giây

### Giám sát CPU
- ✅ Thông tin về CPU model, số cores/threads
- ✅ CPU usage theo thời gian thực
- ✅ Tần số CPU hiện tại

### Quản lý Processes
- ✅ Danh sách tất cả processes đang chạy
- ✅ Hiển thị PID, tên process, CPU usage, memory usage
- ✅ Lọc và tìm kiếm processes
- ✅ Sắp xếp theo các tiêu chí khác nhau

### Giám sát Disk & Network
- ✅ Thông tin về disk usage
- ✅ Network traffic monitoring
- ✅ Interface statistics

### Giao diện người dùng
- ✅ UI hiện đại, responsive với shadcn/ui
- ✅ Dark mode support
- ✅ Biểu đồ tương tác với Recharts
- ✅ Real-time updates
- ✅ Mobile-friendly design

## 🔧 Xử lý sự cố

### Backend không khởi động

**Lỗi: "Java version not compatible"**
```bash
# Kiểm tra Java version
java -version

# Nếu version < 17, cài đặt lại
sudo apt install openjdk-17-jdk
```

**Lỗi: "Port 8080 already in use"**
```bash
# Tìm process đang sử dụng port 8080
sudo lsof -i :8080

# Hoặc thay đổi port trong application.properties
# server.port=8081
```

**Lỗi: Maven dependencies không download được**
```bash
# Clear Maven cache và rebuild
mvn clean install -U
```

### Frontend không kết nối được Backend

**Kiểm tra Backend đang chạy:**
```bash
curl http://localhost:8080/api/health
```

**Kiểm tra CORS configuration:**
- Mở file `backend/src/main/resources/application.properties`
- Đảm bảo có cấu hình CORS phù hợp

**Kiểm tra API URL trong Frontend:**
- Mở file `frontend/src/services/api.ts`
- Đảm bảo base URL trỏ đúng đến backend

### Không đọc được thông tin hệ thống

**Lỗi: "Cannot read /proc filesystem"**
- Ứng dụng cần chạy trên Linux để đọc được `/proc`
- Một số thông tin có thể cần quyền sudo

**Giải pháp:**
```bash
# Chạy với sudo (nếu cần)
sudo java -jar target/PBL4_vr2-1.0-SNAPSHOT.jar
```

### Frontend build errors

**Lỗi: "Module not found"**
```bash
# Xóa node_modules và reinstall
cd frontend
rm -rf node_modules package-lock.json
npm install
```

**Lỗi: TypeScript errors**
```bash
# Kiểm tra TypeScript configuration
npx tsc --noEmit
```

## 📝 Cấu hình

### Backend Configuration

File: `backend/src/main/resources/application.properties`

```properties
# Server configuration
server.port=8080

# CORS configuration
cors.allowed.origins=http://localhost:5173

# Logging
logging.level.com.orca.pbl4=DEBUG
```

### Frontend Configuration

File: `frontend/src/services/api.ts`

```typescript
const API_BASE_URL = 'http://localhost:8080/api';
```

## 🤝 Đóng góp

Mọi đóng góp đều được hoan nghênh! Vui lòng:

1. Fork repository
2. Tạo branch mới (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Mở Pull Request

## 📄 License

Dự án này được phát hành dưới giấy phép MIT. Xem file `LICENSE` để biết thêm chi tiết.

## 👥 Tác giả

ORCA Team - PBL4 Project

## 📞 Liên hệ

Nếu có bất kỳ câu hỏi hoặc đóng góp nào, vui lòng tạo issue trên GitHub repository.

---

**Happy Monitoring! 🚀**
