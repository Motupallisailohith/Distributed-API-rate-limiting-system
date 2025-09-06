#!/bin/bash
# Student Deployment Script - FREE HOSTING

echo "🎓 STUDENT DEPLOYMENT - FREE HOSTING SETUP"
echo "=========================================="

# Check if required tools are installed
command -v git >/dev/null 2>&1 || { echo "❌ Git is required but not installed. Aborting." >&2; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "❌ npm is required but not installed. Aborting." >&2; exit 1; }

echo "✅ Prerequisites check passed"

# Step 1: Prepare Frontend for Vercel
echo ""
echo "📦 Step 1: Preparing Frontend for Vercel..."
cd frontend

# Install dependencies
npm install

# Build for production
npm run build

echo "✅ Frontend built successfully"

# Step 2: Setup Vercel deployment
echo ""
echo "🚀 Step 2: Setting up Vercel deployment..."
echo "Run these commands manually:"
echo "  npm install -g vercel"
echo "  vercel --prod"
echo ""

# Step 3: Prepare Backend for Railway
echo "📦 Step 3: Preparing Backend for Railway..."
cd ..

# Create railway project
echo "Run these commands to deploy backend:"
echo "  npm install -g @railway/cli"
echo "  railway login"
echo "  railway init"
echo "  railway up"
echo ""

# Step 4: Update frontend with backend URL
echo "🔗 Step 4: After Railway deployment:"
echo "1. Copy your Railway app URL (e.g., https://your-app.up.railway.app)"
echo "2. Update frontend/src/App.js and frontend/src/MultiNodeApp.js"
echo "3. Replace 'http://localhost:8091' with your Railway URL"
echo "4. Redeploy frontend with: vercel --prod"
echo ""

echo "🎉 Deployment setup complete!"
echo ""
echo "📋 FREE HOSTING SUMMARY:"
echo "Frontend: Vercel (Free) - https://your-app.vercel.app"
echo "Backend:  Railway (Free) - https://your-app.up.railway.app"
echo "Cost:     $0/month 💰"
echo ""
echo "🎯 Next Steps:"
echo "1. Push code to GitHub"
echo "2. Connect Vercel to GitHub repo"
echo "3. Connect Railway to GitHub repo"
echo "4. Enable automatic deployments"



