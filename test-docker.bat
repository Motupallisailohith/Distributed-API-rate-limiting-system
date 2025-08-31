@echo off
echo 🐳 Testing Docker build locally...

echo 📦 Building Docker image...
docker build -t distributed-rate-limiter .

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker build failed!
    pause
    exit /b 1
)

echo ✅ Docker build successful!

echo 🚀 Testing container startup...
docker run -d -p 8080:8080 -e ALGORITHM=tokenbucket -e UDP_PORT=7001 -e NODE_ID=node1 --name test-container distributed-rate-limiter

echo ⏳ Waiting for container to start...
timeout /t 10

echo 🧪 Testing API endpoint...
curl http://localhost:8080/api/status

echo 🛑 Stopping test container...
docker stop test-container
docker rm test-container

echo ✅ Docker test complete!
pause
