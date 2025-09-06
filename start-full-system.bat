@echo off
echo ========================================
echo DISTRIBUTED RATE LIMITER - FULL SYSTEM
echo ========================================

REM Parse command line arguments
set ALGORITHM=tokenbucket
set BACKEND_PORT=8080
set FRONTEND_PORT=3000
set UDP_PORT=7000
set DEMO_MODE=false
set FRONTEND_MODE=single

:parse_args
if "%~1"=="" goto start_system
if "%~1"=="--algorithm" set ALGORITHM=%~2 & shift & shift & goto parse_args
if "%~1"=="--backend-port" set BACKEND_PORT=%~2 & shift & shift & goto parse_args
if "%~1"=="--frontend-port" set FRONTEND_PORT=%~2 & shift & shift & goto parse_args
if "%~1"=="--udp-port" set UDP_PORT=%~2 & shift & shift & goto parse_args
if "%~1"=="--demo" set DEMO_MODE=true & shift & goto parse_args
if "%~1"=="--multi-node" set FRONTEND_MODE=multi & shift & goto parse_args
shift
goto parse_args

:start_system
echo.
echo Configuration:
echo   Algorithm:      %ALGORITHM%
echo   Backend Port:   %BACKEND_PORT%
echo   Frontend Port:  %FRONTEND_PORT%
echo   UDP Port:       %UDP_PORT%
echo   Demo Mode:      %DEMO_MODE%
echo   Frontend Mode:  %FRONTEND_MODE%
echo.

echo [1/4] Cleaning up any running processes...
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im node.exe >nul 2>&1

echo [2/4] Starting backend server...
if "%DEMO_MODE%"=="true" (
    start "Backend Server" cmd /k "title Backend Server && start-backend.bat --algorithm %ALGORITHM% --http-port %BACKEND_PORT% --udp-port %UDP_PORT% --demo"
) else (
    start "Backend Server" cmd /k "title Backend Server && start-backend.bat --algorithm %ALGORITHM% --http-port %BACKEND_PORT% --udp-port %UDP_PORT%"
)

echo [3/4] Waiting for backend to initialize...
timeout /t 8 >nul

echo [4/4] Starting frontend dashboard...
start "Frontend Dashboard" cmd /k "title Frontend Dashboard && start-frontend.bat --mode %FRONTEND_MODE% --port %FRONTEND_PORT%"

echo.
echo ========================================
echo SYSTEM STARTED SUCCESSFULLY!
echo ========================================
echo.
echo Backend Server:     http://localhost:%BACKEND_PORT%
echo Frontend Dashboard: http://localhost:%FRONTEND_PORT%
echo.
echo The system will open automatically in your browser.
echo Check both terminal windows for logs.
echo.
echo To stop the system:
echo   1. Close both terminal windows, or
echo   2. Press Ctrl+C in each window
echo.
echo ========================================

REM Open browser automatically after a short delay
timeout /t 3 >nul
start http://localhost:%FRONTEND_PORT%

pause
