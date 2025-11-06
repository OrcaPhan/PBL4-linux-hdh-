#!/bin/bash

# Script cài đặt môi trường cho PBL4 Memory Management App trên Ubuntu

echo "=== Bắt đầu cài đặt môi trường cho PBL4 Project ==="

# 1. Update hệ thống
echo "=== Cập nhật hệ thống ==="
sudo apt update && sudo apt upgrade -y

# 2. Cài đặt Git
echo "=== Cài đặt Git ==="
sudo apt install -y git

# 3. Cài đặt Java (OpenJDK 17)
echo "=== Cài đặt Java Development Kit ==="
sudo apt install -y openjdk-17-jdk openjdk-17-jre

# Kiểm tra Java version
echo "Checking Java version..."
java -version
javac -version

# 4. Cài đặt Maven
echo "=== Cài đặt Apache Maven ==="
sudo apt install -y maven

# Kiểm tra Maven version
echo "Checking Maven version..."
mvn -version

# 5. Cài đặt Node.js và npm (cho frontend React)
echo "=== Cài đặt Node.js và npm ==="
# Cài đặt nvm (Node Version Manager)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# Load nvm
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

# Cài đặt Node.js LTS
nvm install --lts
nvm use --lts

# Kiểm tra Node và npm version
echo "Checking Node.js and npm versions..."
node -v
npm -v

# 6. Clone repository và checkout nhánh dthinh
echo "=== Clone repository và checkout nhánh dthinh ==="
cd ~
git clone https://github.com/OrcaPhan/PBL4-linux-hdh-.git
cd PBL4-linux-hdh-
git checkout dthinh
git pull origin dthinh

# Hiển thị branch hiện tại
echo "=== Branch hiện tại ==="
git branch

# 7. Build backend (Java Spring Boot)
echo "=== Build backend với Maven ==="
cd backend
mvn clean install
cd ..

# 8. Cài đặt dependencies cho frontend
echo "=== Cài đặt dependencies cho frontend ==="
cd frontend
npm install
cd ..

# 9. Cấp quyền thực thi cho các script
echo "=== Cấp quyền thực thi cho scripts ==="
chmod +x start.sh
chmod +x build-desktop.sh

echo ""
echo "=== Hoàn thành cài đặt! ==="
echo ""
echo "📦 Đã cài đặt:"
echo "  ✓ Git"
echo "  ✓ Java 17"
echo "  ✓ Maven"
echo "  ✓ Node.js LTS"
echo "  ✓ Project dependencies"
echo ""
echo "📁 Cấu trúc project:"
echo "  backend/   - Java Spring Boot API"
echo "  frontend/  - React + TypeScript UI"
echo ""
echo "🚀 Để chạy project:"
echo "  1. Tự động: ./start.sh"
echo "  2. Manual Backend: cd backend && mvn spring-boot:run"
echo "  3. Manual Frontend: cd frontend && npm run dev"
echo ""
echo "🌐 URLs:"
echo "  Backend API: http://localhost:8080"
echo "  Frontend UI: http://localhost:5173"
echo ""
echo "📖 Đọc thêm: README.md hoặc SETUP_GUIDE.md"
