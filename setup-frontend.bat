@echo off
echo ========================================
echo DISTRIBUTED RATE LIMITER - FRONTEND SETUP
echo ========================================

echo.
echo [1/3] Installing Node.js dependencies...
cd frontend
call npm install

echo.
echo [2/3] Building React application...
call npm run build

echo.
echo [3/3] Setup complete!
echo.
echo ========================================
echo NEXT STEPS:
echo ========================================
echo.
echo 1. Start your Java backend:
echo    java -cp target\classes com.Motupallisailohith.ratelimit.Main
echo.
echo 2. Start the React development server:
echo    cd frontend
echo    npm start
echo.
echo 3. Open browser to:
echo    Frontend: http://localhost:3000
echo    Backend:  http://localhost:8080
echo.
echo ========================================

pause

