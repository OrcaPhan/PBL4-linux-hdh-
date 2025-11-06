# Backend - Java Spring Boot

Backend API server cho ORCA System Monitor.

## Công nghệ

- Java 17+
- Spring Boot 3.x
- OSHI (Operating System and Hardware Information)
- Maven

## Build

```bash
mvn clean package
```

## Chạy

```bash
# Development
mvn spring-boot:run

# Production
java -jar target/PBL4_vr2-1.0-SNAPSHOT.jar
```

## API Endpoints

Backend chạy tại `http://localhost:8080`

- `GET /api/health` - Health check
- `GET /api/memory` - Thông tin memory
- `GET /api/cpu` - Thông tin CPU
- `GET /api/processes` - Danh sách processes
- `GET /api/system` - Thông tin hệ thống

## Cấu trúc

```
src/
├── main/
│   ├── java/
│   │   └── com/orca/pbl4/
│   │       ├── App.java                    # Main application
│   │       ├── api/
│   │       │   └── SystemMonitorController.java  # REST API
│   │       ├── core/
│   │       │   ├── model/                  # Data models
│   │       │   ├── system/                 # System info readers
│   │       │   └── util/                   # Utilities
│   │       └── ui/                         # Swing UI (deprecated)
│   └── resources/
│       └── application.properties          # Configuration
└── test/
    └── java/                               # Tests
```
