@echo off
echo ========================================
echo DISTRIBUTED RATE LIMITER - WORKING DEMO
echo ========================================

echo.
echo [1/4] Cleaning up any running processes...
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im node.exe >nul 2>&1

echo [2/4] Using Maven to run the original system...
echo This avoids Java version issues by using Maven's classpath management.

echo [3/4] Starting backend with Maven...
start "Backend-Maven" cmd /k "echo BACKEND STARTING (Maven)... && mvn exec:java -Dexec.mainClass=com.Motupallisailohith.ratelimit.Main -Dexec.args='--algorithm=tokenbucket --http.port=8085 --udp.port=7000' -q"

timeout /t 8 >nul

echo [4/4] Starting React frontend...
cd frontend

if not exist node_modules (
    echo Installing frontend dependencies...
    call npm install
)

echo Updating frontend to connect to port 8085...
(
echo const API_BASE_URL = 'http://localhost:8085';
echo.
echo // Update App.js to use port 8085
echo const originalContent = require('fs'^).readFileSync('src/App.js', 'utf8'^);
echo const updatedContent = originalContent.replace('localhost:8080', 'localhost:8085'^);
echo require('fs'^).writeFileSync('src/App.js', updatedContent^);
) > update-port.js

node update-port.js >nul 2>&1

start "Frontend" cmd /k "echo FRONTEND STARTING... && npm start"

echo.
echo ========================================
echo DEMO SYSTEM STARTED!
echo ========================================
echo.
echo Backend:  http://localhost:8085 (Enhanced HTTPServer)
echo Frontend: http://localhost:3000 (React Dashboard)
echo.
echo Features:
echo ✅ JWT Authentication (with nimbus-jose-jwt)
echo ✅ Rate limiting algorithms (Token Bucket, Leaky Bucket, Sliding Window)
echo ✅ UDP distribution and synchronization
echo ✅ Real-time dashboard with charts
echo ✅ Interactive testing
echo ✅ CORS support
echo.
echo Wait 10-15 seconds for both services to start, then visit:
echo http://localhost:3000 - React Dashboard
echo http://localhost:8085 - Backend API
echo.
echo ========================================

pause

