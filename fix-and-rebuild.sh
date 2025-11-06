#!/bin/bash

# Quick fix script - Pull latest changes and rebuild
echo "=== Pulling latest changes from dthinh branch ==="
cd ~/PBL4-linux-hdh-
git pull origin dthinh

echo ""
echo "=== Rebuilding backend ==="
cd backend
mvn clean install

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build thành công!"
    echo ""
    echo "🚀 Để chạy project:"
    echo "  cd ~/PBL4-linux-hdh-"
    echo "  ./start.sh"
else
    echo ""
    echo "❌ Build failed. Kiểm tra lỗi ở trên."
fi
