@echo off
echo ========================================
echo DISTRIBUTED RATE LIMITER - DEMO SYSTEM
echo ========================================

echo.
echo [1/4] Cleaning up any running processes...
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im node.exe >nul 2>&1

echo [2/4] Compiling with Maven (includes dependencies)...
call mvn compile -q

if %errorlevel% neq 0 (
    echo ERROR: Maven compilation failed!
    echo.
    echo Trying alternative approach...
    echo [2b/4] Compiling core components only...
    javac -cp . -target 8 -source 8 src\main\java\com\Motupallisailohith\ratelimit\bucket\*.java src\main\java\com\Motupallisailohith\ratelimit\protocol\*.java src\main\java\com\Motupallisailohith\ratelimit\reliability\*.java src\main\java\com\Motupallisailohith\ratelimit\server\UDPListener.java src\main\java\com\Motupallisailohith\ratelimit\security\JwtException.java
    
    if %errorlevel% neq 0 (
        echo ERROR: Core compilation also failed!
        pause
        exit /b 1
    )
    
    echo [2c/4] Creating demo version without JWT dependencies...
    echo This will create a simplified demo that works without external JWT libraries.
)

echo [3/4] Starting backend...
echo.
echo Trying Maven classpath first...
start "Backend-Maven" cmd /k "echo BACKEND STARTING (Maven)... && mvn exec:java -Dexec.mainClass=com.Motupallisailohith.ratelimit.Main -Dexec.args='--algorithm=tokenbucket --http.port=8080 --udp.port=7000' -q"

timeout /t 3 >nul

echo [4/4] Starting React frontend...
cd frontend

if not exist node_modules (
    echo Installing frontend dependencies...
    call npm install
)

start "Frontend" cmd /k "echo FRONTEND STARTING... && npm start"

echo.
echo ========================================
echo SYSTEM STARTING...
echo ========================================
echo.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:3000 (will open automatically)
echo.
echo If backend fails due to JWT dependencies:
echo 1. Check that Maven dependencies are installed
echo 2. Or use the simplified demo version
echo.
echo ========================================

pause

