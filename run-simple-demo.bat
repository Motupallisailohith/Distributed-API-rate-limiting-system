@echo off
echo ========================================
echo DISTRIBUTED RATE LIMITER - SIMPLE DEMO
echo ========================================

echo.
echo [1/4] Cleaning up any running processes...
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im node.exe >nul 2>&1

echo [2/4] Compiling demo version (no JWT dependencies)...
javac -cp . -target 8 -source 8 src\main\java\com\Motupallisailohith\ratelimit\bucket\*.java src\main\java\com\Motupallisailohith\ratelimit\protocol\*.java src\main\java\com\Motupallisailohith\ratelimit\reliability\*.java src\main\java\com\Motupallisailohith\ratelimit\server\UDPListener.java src\main\java\com\Motupallisailohith\ratelimit\server\DemoHTTPServer.java src\main\java\com\Motupallisailohith\ratelimit\security\JwtException.java src\main\java\com\Motupallisailohith\ratelimit\DemoMain.java

if %errorlevel% neq 0 (
    echo ERROR: Demo compilation failed!
    pause
    exit /b 1
)

echo [3/4] Starting demo backend...
start "Demo-Backend" cmd /k "echo DEMO BACKEND STARTING... && java -cp . com.Motupallisailohith.ratelimit.DemoMain --algorithm=tokenbucket --http.port=8080 --udp.port=7000"

timeout /t 5 >nul

echo [4/4] Starting React frontend...
cd frontend

if not exist node_modules (
    echo Installing frontend dependencies...
    call npm install
)

start "Frontend" cmd /k "echo FRONTEND STARTING... && npm start"

echo.
echo ========================================
echo DEMO SYSTEM STARTED!
echo ========================================
echo.
echo Backend:  http://localhost:8080 (Demo Mode)
echo Frontend: http://localhost:3000 (Full Dashboard)
echo.
echo Features working:
echo ✅ Rate limiting algorithms (Token Bucket, Leaky Bucket, Sliding Window)
echo ✅ UDP distribution and synchronization
echo ✅ Real-time dashboard with charts
echo ✅ Interactive testing (simplified JWT)
echo ✅ CORS support for frontend
echo.
echo Note: This demo uses simplified JWT for compatibility.
echo For production JWT support, use Maven with dependencies.
echo.
echo ========================================

pause

