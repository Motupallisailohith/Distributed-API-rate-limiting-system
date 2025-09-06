@echo off
echo ========================================
echo VERCEL DEPLOYMENT - FREE TIER
echo ========================================

echo.
echo 🎯 Step 1: Installing Vercel CLI...
npm install -g vercel

echo.
echo 🎯 Step 2: Preparing frontend for deployment...
cd frontend

echo Installing dependencies...
npm install

echo Building for production...
npm run build

if %errorlevel% neq 0 (
    echo ❌ Build failed! Please check for errors.
    pause
    exit /b 1
)

echo ✅ Build successful!

echo.
echo 🎯 Step 3: Deploying to Vercel...
echo.
echo 📋 IMPORTANT: During deployment, Vercel will ask:
echo   - "Set up and deploy?" → Yes
echo   - "Which scope?" → Your personal account
echo   - "Link to existing project?" → No (first time)
echo   - "What's your project's name?" → distributed-rate-limiter
echo   - "In which directory is your code located?" → ./
echo.

vercel --prod

echo.
echo 🎉 Deployment complete!
echo.
echo 📋 NEXT STEPS:
echo 1. Copy your Vercel URL (e.g., https://distributed-rate-limiter.vercel.app)
echo 2. Set up your backend (Railway/Heroku)
echo 3. Update environment variables in Vercel dashboard
echo 4. Test your deployed application
echo.

cd ..
pause



