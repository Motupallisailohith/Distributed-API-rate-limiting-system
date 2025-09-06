@echo off
echo ========================================
echo DISTRIBUTED RATE LIMITER - FRONTEND
echo ========================================

REM Parse command line arguments
set MODE=single
set PORT=3000

:parse_args
if "%~1"=="" goto start_frontend
if "%~1"=="--mode" set MODE=%~2 & shift & shift & goto parse_args
if "%~1"=="--port" set PORT=%~2 & shift & shift & goto parse_args
shift
goto parse_args

:start_frontend
echo.
echo Configuration:
echo   Mode: %MODE%
echo   Port: %PORT%
echo.

echo [1/3] Cleaning up any running Node processes...
taskkill /f /im node.exe >nul 2>&1

echo [2/3] Checking frontend setup...
cd frontend

if not exist node_modules (
    echo Installing Node.js dependencies...
    call npm install
    if %errorlevel% neq 0 (
        echo ERROR: npm install failed!
        pause
        exit /b 1
    )
)

echo [3/3] Starting React development server...

REM Set environment variables based on mode
if "%MODE%"=="multi" (
    echo Starting multi-node dashboard...
    set REACT_APP_MODE=multi
) else (
    echo Starting single-node dashboard...
    set REACT_APP_MODE=single
)

set PORT=%PORT%
call npm start

echo.
echo Frontend server stopped.
cd ..
pause
