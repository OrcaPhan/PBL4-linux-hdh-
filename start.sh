#!/bin/bash

# Script khởi động ORCA System Monitor
# Tự động chạy cả Backend (Java) và Frontend (Vite dev server)

echo "==================================="
echo "ORCA System Monitor - Startup Script"
echo "==================================="

# Màu sắc
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Kiểm tra Java
echo -e "\n${YELLOW}Checking Java...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java not found. Please install Java 17+${NC}"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Error: Java 17+ required. Current version: $JAVA_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java version: $(java -version 2>&1 | head -n 1)${NC}"

# Kiểm tra Maven
echo -e "\n${YELLOW}Checking Maven...${NC}"
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven not found. Please install Maven 3.6+${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Maven version: $(mvn -version | head -n 1)${NC}"

# Kiểm tra Node.js
echo -e "\n${YELLOW}Checking Node.js...${NC}"
if ! command -v node &> /dev/null; then
    echo -e "${RED}Error: Node.js not found. Please install Node.js 18+${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Node.js version: $(node -v)${NC}"

# Build Backend nếu chưa có
echo -e "\n${YELLOW}Building Backend...${NC}"
cd backend
if [ ! -f "target/PBL4_vr2-1.0-SNAPSHOT.jar" ]; then
    echo "Building with Maven..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Maven build failed${NC}"
        exit 1
    fi
fi
echo -e "${GREEN}✓ Backend built successfully${NC}"
cd ..

# Install Frontend dependencies nếu chưa có
echo -e "\n${YELLOW}Checking Frontend dependencies...${NC}"
cd frontend
if [ ! -d "node_modules" ]; then
    echo "Installing npm dependencies..."
    npm install
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: npm install failed${NC}"
        exit 1
    fi
fi
cd ..
echo -e "${GREEN}✓ Frontend dependencies installed${NC}"

# Tạo file PID để quản lý processes
BACKEND_PID_FILE="/tmp/orca-backend.pid"
FRONTEND_PID_FILE="/tmp/orca-frontend.pid"

# Cleanup function
cleanup() {
    echo -e "\n${YELLOW}Stopping services...${NC}"
    
    if [ -f "$BACKEND_PID_FILE" ]; then
        BACKEND_PID=$(cat "$BACKEND_PID_FILE")
        kill $BACKEND_PID 2>/dev/null
        rm "$BACKEND_PID_FILE"
        echo -e "${GREEN}✓ Backend stopped${NC}"
    fi
    
    if [ -f "$FRONTEND_PID_FILE" ]; then
        FRONTEND_PID=$(cat "$FRONTEND_PID_FILE")
        kill $FRONTEND_PID 2>/dev/null
        rm "$FRONTEND_PID_FILE"
        echo -e "${GREEN}✓ Frontend stopped${NC}"
    fi
    
    echo -e "\n${GREEN}Shutdown complete${NC}"
    exit 0
}

# Trap SIGINT (Ctrl+C) và SIGTERM
trap cleanup SIGINT SIGTERM

# Chạy Backend
echo -e "\n${YELLOW}Starting Backend on port 8080...${NC}"
cd backend
java -jar target/PBL4_vr2-1.0-SNAPSHOT.jar > /tmp/orca-backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > "$BACKEND_PID_FILE"
echo -e "${GREEN}✓ Backend started (PID: $BACKEND_PID)${NC}"
cd ..

# Đợi Backend khởi động
echo -e "${YELLOW}Waiting for backend to start...${NC}"
sleep 5

# Kiểm tra Backend đã chạy chưa
if ! kill -0 $BACKEND_PID 2>/dev/null; then
    echo -e "${RED}Error: Backend failed to start. Check /tmp/orca-backend.log${NC}"
    cat /tmp/orca-backend.log
    exit 1
fi

# Kiểm tra health endpoint
MAX_RETRIES=10
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s http://localhost:8080/api/health > /dev/null; then
        echo -e "${GREEN}✓ Backend is ready${NC}"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "Waiting for backend... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}Error: Backend health check failed${NC}"
    cleanup
    exit 1
fi

# Chạy Frontend
echo -e "\n${YELLOW}Starting Frontend on port 5173...${NC}"
cd frontend
npm run dev > /tmp/orca-frontend.log 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > "$FRONTEND_PID_FILE"
cd ..
echo -e "${GREEN}✓ Frontend started (PID: $FRONTEND_PID)${NC}"

echo -e "\n${GREEN}==================================="
echo "✓ ORCA System Monitor is running!"
echo "===================================${NC}"
echo ""
echo "Backend API:  http://localhost:8080"
echo "Frontend UI:  http://localhost:5173"
echo ""
echo "Backend logs: /tmp/orca-backend.log"
echo "Frontend logs: /tmp/orca-frontend.log"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all services${NC}"
echo ""

# Chờ vô hạn (cho đến khi Ctrl+C)
wait
