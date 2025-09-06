@echo off
echo ========================================
echo DISTRIBUTED RATE LIMITER - BACKEND
echo ========================================

REM Parse command line arguments
set ALGORITHM=tokenbucket
set HTTP_PORT=8080
set UDP_PORT=7000
set DEMO_MODE=false

:parse_args
if "%~1"=="" goto start_backend
if "%~1"=="--algorithm" set ALGORITHM=%~2 & shift & shift & goto parse_args
if "%~1"=="--http-port" set HTTP_PORT=%~2 & shift & shift & goto parse_args
if "%~1"=="--udp-port" set UDP_PORT=%~2 & shift & shift & goto parse_args
if "%~1"=="--demo" set DEMO_MODE=true & shift & goto parse_args
shift
goto parse_args

:start_backend
echo.
echo Configuration:
echo   Algorithm: %ALGORITHM%
echo   HTTP Port: %HTTP_PORT%
echo   UDP Port:  %UDP_PORT%
echo   Demo Mode: %DEMO_MODE%
echo.

echo [1/2] Cleaning up any running Java processes...
taskkill /f /im java.exe >nul 2>&1

echo [2/2] Starting backend server...

REM Use Maven compiled classes if available, otherwise compile manually
if exist target\classes (
    echo Using Maven compiled classes...
    if "%DEMO_MODE%"=="true" (
        java -cp target\classes com.Motupallisailohith.ratelimit.Main --demo --algorithm=%ALGORITHM% --http.port=%HTTP_PORT% --udp.port=%UDP_PORT%
    ) else (
        java -cp target\classes com.Motupallisailohith.ratelimit.Main --algorithm=%ALGORITHM% --http.port=%HTTP_PORT% --udp.port=%UDP_PORT%
    )
) else (
    echo Compiling Java sources...
    javac -cp . -d . src\main\java\com\Motupallisailohith\ratelimit\*.java src\main\java\com\Motupallisailohith\ratelimit\bucket\*.java src\main\java\com\Motupallisailohith\ratelimit\server\*.java src\main\java\com\Motupallisailohith\ratelimit\security\*.java src\main\java\com\Motupallisailohith\ratelimit\reliability\*.java src\main\java\com\Motupallisailohith\ratelimit\protocol\*.java
    
    if %errorlevel% neq 0 (
        echo ERROR: Backend compilation failed!
        pause
        exit /b 1
    )
    
    if "%DEMO_MODE%"=="true" (
        java -cp . com.Motupallisailohith.ratelimit.Main --demo --algorithm=%ALGORITHM% --http.port=%HTTP_PORT% --udp.port=%UDP_PORT%
    ) else (
        java -cp . com.Motupallisailohith.ratelimit.Main --algorithm=%ALGORITHM% --http.port=%HTTP_PORT% --udp.port=%UDP_PORT%
    )
)

echo.
echo Backend server stopped.
pause
