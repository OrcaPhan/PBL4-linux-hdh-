#!/bin/bash

# Script build Desktop App với Tauri
echo "==================================="
echo "ORCA System Monitor - Build Script"
echo "==================================="

# Màu sắc
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Kiểm tra dependencies
echo -e "\n${YELLOW}Checking dependencies...${NC}"

# Kiểm tra Rust
if ! command -v cargo &> /dev/null; then
    echo -e "${RED}Error: Rust not found. Installing Rust...${NC}"
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
    source $HOME/.cargo/env
fi
echo -e "${GREEN}✓ Rust installed${NC}"

# Kiểm tra Tauri dependencies
echo -e "${YELLOW}Checking Tauri system dependencies...${NC}"
MISSING_DEPS=""

for pkg in libwebkit2gtk-4.0-dev build-essential curl wget file libssl-dev libgtk-3-dev libayatana-appindicator3-dev librsvg2-dev; do
    if ! dpkg -l | grep -q "^ii  $pkg"; then
        MISSING_DEPS="$MISSING_DEPS $pkg"
    fi
done

if [ ! -z "$MISSING_DEPS" ]; then
    echo -e "${YELLOW}Installing missing dependencies: $MISSING_DEPS${NC}"
    sudo apt update
    sudo apt install -y $MISSING_DEPS
fi
echo -e "${GREEN}✓ All system dependencies installed${NC}"

# Build Backend
echo -e "\n${YELLOW}Building Java Backend...${NC}"
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Maven build failed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Backend built successfully${NC}"

# Copy backend jar to frontend directory for packaging
mkdir -p "Memory Management App Design/backend"
cp target/PBL4_vr2-1.0-SNAPSHOT.jar "Memory Management App Design/backend/"
echo -e "${GREEN}✓ Backend JAR copied${NC}"

# Build Frontend with Tauri
echo -e "\n${YELLOW}Building Frontend and Desktop App...${NC}"
cd "Memory Management App Design"

# Install dependencies
npm install

# Initialize Tauri if not already done
if [ ! -d "src-tauri" ]; then
    echo -e "${YELLOW}Initializing Tauri...${NC}"
    npm run tauri init
fi

# Build Tauri app
echo -e "${YELLOW}Building Tauri desktop app...${NC}"
npm run tauri:build

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Tauri build failed${NC}"
    exit 1
fi

cd ..

echo -e "\n${GREEN}==================================="
echo "✓ Build completed successfully!"
echo "===================================${NC}"
echo ""
echo "Desktop app packages can be found in:"
echo "  Memory Management App Design/src-tauri/target/release/bundle/"
echo ""
echo "Available formats:"
echo "  - .deb (Debian/Ubuntu package)"
echo "  - AppImage (Universal Linux binary)"
echo ""
echo "To install the .deb package:"
echo "  sudo dpkg -i 'Memory Management App Design/src-tauri/target/release/bundle/deb/orca-system-monitor_*.deb'"
echo ""
