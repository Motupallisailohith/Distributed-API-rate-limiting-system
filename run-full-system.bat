@echo off
echo ========================================
echo DISTRIBUTED RATE LIMITER - FULL SYSTEM
echo ========================================

echo.
echo [1/4] Cleaning up any running processes...
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im node.exe >nul 2>&1

echo [2/4] Compiling Java backend...
javac -cp . -target 8 -source 8 src\main\java\com\Motupallisailohith\ratelimit\*.java src\main\java\com\Motupallisailohith\ratelimit\bucket\*.java src\main\java\com\Motupallisailohith\ratelimit\server\*.java src\main\java\com\Motupallisailohith\ratelimit\security\*.java src\main\java\com\Motupallisailohith\ratelimit\reliability\*.java src\main\java\com\Motupallisailohith\ratelimit\protocol\*.java

if %errorlevel% neq 0 (
    echo ERROR: Backend compilation failed!
    pause
    exit /b 1
)

echo [3/4] Starting Java backend...
start "Backend" cmd /k "echo BACKEND STARTING... && java -cp . com.Motupallisailohith.ratelimit.Main --algorithm=tokenbucket --http.port=8080 --udp.port=7000"

timeout /t 5 >nul

echo [4/4] Starting React frontend...
cd frontend
start "Frontend" cmd /k "echo FRONTEND STARTING... && npm start"

echo.
echo ========================================
echo SYSTEM STARTED SUCCESSFULLY!
echo ========================================
echo.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:3000
echo.
echo The system will open automatically in your browser.
echo Check both terminal windows for logs.
echo.
echo ========================================

pause

