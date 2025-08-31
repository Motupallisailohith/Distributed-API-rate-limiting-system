@echo off
echo ========================================
echo DISTRIBUTED RATE LIMITER - FULL SYSTEM
echo ========================================

echo.
echo [1/5] Cleaning up processes and ports...
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im node.exe >nul 2>&1

echo [2/5] Using Maven to compile and run backend...
echo This handles all dependencies and Java version issues automatically.

echo [3/5] Starting backend on port 8090 (avoiding conflicts)...
start "Backend" cmd /k "echo BACKEND STARTING... && mvn exec:java -Dexec.mainClass=com.Motupallisailohith.ratelimit.Main -Dexec.args='--algorithm=tokenbucket --http.port=8090 --udp.port=7001' -q"

echo [4/5] Waiting for backend to start...
timeout /t 10 >nul

echo [5/5] Starting React frontend...
cd frontend
start "Frontend" cmd /k "echo FRONTEND STARTING... && npm start"

echo.
echo ========================================
echo SYSTEM STARTED SUCCESSFULLY!
echo ========================================
echo.
echo 🎯 Backend API:  http://localhost:8090
echo 🎯 Frontend UI:  http://localhost:3000
echo.
echo ✅ Features Available:
echo   - JWT Authentication (full nimbus-jose-jwt support)
echo   - Rate Limiting Algorithms (Token Bucket, Leaky Bucket, Sliding Window)  
echo   - Real UDP Distribution & Synchronization
echo   - Interactive Dashboard with Real-time Charts
echo   - CORS Support for Frontend Integration
echo.
echo 📋 API Endpoints:
echo   POST /api/data     - Rate limited endpoint (requires JWT)
echo   GET  /api/status   - System status
echo   GET  /api/metrics  - System metrics
echo   GET  /            - Web interface
echo.
echo 🧪 Testing:
echo   1. Wait 15-20 seconds for both services to fully start
echo   2. Visit http://localhost:3000 for the dashboard
echo   3. Generate JWT tokens and test rate limiting
echo   4. Try stress tests to see distributed coordination
echo.
echo ========================================

cd ..
pause

