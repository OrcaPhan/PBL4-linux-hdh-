#!/bin/bash

# ORCA System Monitor - Setup Script
# Script tự động cài đặt môi trường cho Ubuntu/Linux

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check version
check_version() {
    local command=$1
    local version_flag=$2
    local required_version=$3
    
    if command_exists "$command"; then
        local current_version=$($command $version_flag 2>&1 | head -n 1)
        print_success "$command is installed: $current_version"
        return 0
    else
        return 1
    fi
}

# Start setup
print_header "ORCA System Monitor - Setup Script"
print_info "This script will install all necessary dependencies for the project"
print_warning "This script requires sudo privileges\n"

read -p "Do you want to continue? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_error "Setup cancelled"
    exit 1
fi

# Update package list
print_header "Step 1: Updating package list"
print_info "Running: sudo apt update"
sudo apt update
print_success "Package list updated"

# Install Java 17
print_header "Step 2: Installing Java 17"
if check_version "java" "-version" "17"; then
    print_info "Java is already installed"
else
    print_info "Installing OpenJDK 17..."
    sudo apt install -y openjdk-17-jdk
    
    if check_version "java" "-version" "17"; then
        print_success "Java 17 installed successfully"
    else
        print_error "Failed to install Java 17"
        exit 1
    fi
fi

# Install Maven
print_header "Step 3: Installing Maven"
if check_version "mvn" "--version" "3.6"; then
    print_info "Maven is already installed"
else
    print_info "Installing Maven..."
    sudo apt install -y maven
    
    if check_version "mvn" "--version" "3.6"; then
        print_success "Maven installed successfully"
    else
        print_error "Failed to install Maven"
        exit 1
    fi
fi

# Install Node.js 18
print_header "Step 4: Installing Node.js 18"
if check_version "node" "--version" "18"; then
    NODE_VERSION=$(node --version)
    NODE_MAJOR_VERSION=$(echo $NODE_VERSION | cut -d'.' -f1 | sed 's/v//')
    
    if [ "$NODE_MAJOR_VERSION" -ge 18 ]; then
        print_info "Node.js is already installed: $NODE_VERSION"
    else
        print_warning "Node.js version is too old. Upgrading to v18..."
        sudo apt remove -y nodejs
        curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
        sudo apt install -y nodejs
    fi
else
    print_info "Installing Node.js 18..."
    
    # Check if curl is installed
    if ! command_exists "curl"; then
        print_info "Installing curl first..."
        sudo apt install -y curl
    fi
    
    # Install Node.js 18 using NodeSource
    curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
    sudo apt install -y nodejs
    
    if check_version "node" "--version" "18"; then
        print_success "Node.js 18 installed successfully"
    else
        print_error "Failed to install Node.js 18"
        exit 1
    fi
fi

# Check npm
print_header "Step 5: Verifying npm"
if check_version "npm" "--version" "8"; then
    print_success "npm is installed"
else
    print_error "npm is not installed properly"
    exit 1
fi

# Install Git (if not present)
print_header "Step 6: Checking Git"
if check_version "git" "--version" "2"; then
    print_info "Git is already installed"
else
    print_info "Installing Git..."
    sudo apt install -y git
    print_success "Git installed successfully"
fi

# Install project dependencies
print_header "Step 7: Installing project dependencies"

# Backend dependencies
print_info "Installing backend dependencies..."
cd backend
if [ -f "pom.xml" ]; then
    print_info "Running: mvn clean install"
    mvn clean install -DskipTests
    print_success "Backend dependencies installed"
else
    print_error "pom.xml not found in backend directory"
    exit 1
fi
cd ..

# Frontend dependencies
print_info "Installing frontend dependencies..."
cd frontend
if [ -f "package.json" ]; then
    print_info "Running: npm install"
    npm install
    print_success "Frontend dependencies installed"
else
    print_error "package.json not found in frontend directory"
    exit 1
fi
cd ..

# Verify installations
print_header "Step 8: Verifying installations"
echo ""
print_info "Java Version:"
java -version
echo ""
print_info "Maven Version:"
mvn --version
echo ""
print_info "Node.js Version:"
node --version
echo ""
print_info "npm Version:"
npm --version
echo ""
print_info "Git Version:"
git --version
echo ""

# Setup complete
print_header "Setup Complete!"
print_success "All dependencies have been installed successfully"
echo ""
print_info "Next steps:"
echo "  1. Start the backend server:"
echo "     ${YELLOW}cd backend && mvn spring-boot:run${NC}"
echo ""
echo "  2. In a new terminal, start the frontend:"
echo "     ${YELLOW}cd frontend && npm run dev${NC}"
echo ""
echo "  3. Open your browser and navigate to:"
echo "     ${YELLOW}http://localhost:5173${NC}"
echo ""
print_success "Happy monitoring! 🚀"
