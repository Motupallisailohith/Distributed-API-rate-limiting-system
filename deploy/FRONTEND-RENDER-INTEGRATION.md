# 🔗 Frontend-Render Integration Guide

## 🎯 Overview
This guide shows how to connect your Vercel frontend to real Render.com backends, replacing demo mode with actual distributed rate limiting.

## 📋 Prerequisites
- ✅ Render.com account created
- ✅ 3 services deployed on Render (Gateway A, B, C)
- ✅ All services showing "Live" status
- ✅ Vercel account with existing project

## 🌐 Your Render Service URLs
After deploying to Render, you'll have these URLs:
- **Gateway A**: `https://gateway-a-east.onrender.com`
- **Gateway B**: `https://gateway-b-west.onrender.com`  
- **Gateway C**: `https://gateway-c-intl.onrender.com`

## 🔧 Frontend Configuration Updated

### Files Modified:
1. ✅ **`vercel.json`** - Environment variables for production
2. ✅ **`frontend/env.production`** - Production environment config
3. ✅ **`frontend/env.local`** - Development environment config
4. ✅ **`frontend/src/config.js`** - Centralized configuration
5. ✅ **`frontend/src/MultiNodeApp.js`** - Updated node URLs
6. ✅ **`frontend/src/App.js`** - Updated API base URL

### Environment Variables Set:
```bash
REACT_APP_API_BASE=https://gateway-a-east.onrender.com
REACT_APP_NODE1_URL=https://gateway-a-east.onrender.com
REACT_APP_NODE2_URL=https://gateway-b-west.onrender.com
REACT_APP_NODE3_URL=https://gateway-c-intl.onrender.com
REACT_APP_MODE=production
```

## 🚀 Deployment Steps

### Step 1: Test Render Services
Before updating frontend, verify your Render services are working:

```bash
# Test each service
curl https://gateway-a-east.onrender.com/api/status
curl https://gateway-b-west.onrender.com/api/status  
curl https://gateway-c-intl.onrender.com/api/status

# Expected response:
# {"status":"healthy","algorithm":"tokenbucket","timestamp":...}
```

### Step 2: Update Render URLs (if different)
If your Render service names are different, update these files:

**In `vercel.json`:**
```json
"env": {
  "REACT_APP_NODE1_URL": "https://YOUR-ACTUAL-SERVICE-1.onrender.com",
  "REACT_APP_NODE2_URL": "https://YOUR-ACTUAL-SERVICE-2.onrender.com",
  "REACT_APP_NODE3_URL": "https://YOUR-ACTUAL-SERVICE-3.onrender.com"
}
```

**In `frontend/env.production`:**
```bash
REACT_APP_NODE1_URL=https://YOUR-ACTUAL-SERVICE-1.onrender.com
REACT_APP_NODE2_URL=https://YOUR-ACTUAL-SERVICE-2.onrender.com
REACT_APP_NODE3_URL=https://YOUR-ACTUAL-SERVICE-3.onrender.com
```

### Step 3: Build and Deploy to Vercel

```bash
# Navigate to frontend directory
cd frontend

# Build with production environment
npm run build:win

# Deploy to Vercel
vercel --prod
```

### Step 4: Test Integration

After deployment, test your live system:

1. **Visit Multi-Node Dashboard:**
   `https://your-app.vercel.app/?mode=multi`

2. **Check Node Status:**
   - All 3 nodes should show "ONLINE" 
   - Each should display different algorithms
   - UDP metrics should be real (not simulated)

3. **Test API Functionality:**
   - Generate JWT token
   - Test individual gateways
   - Run distributed stress test
   - Switch algorithms

## 🎯 What Changes From Demo Mode

### Before (Demo Mode):
- ❌ Simulated API responses
- ❌ Fake UDP metrics  
- ❌ Mock rate limiting
- ❌ No real algorithm switching

### After (Real Render Backends):
- ✅ **Real API calls** to Render services
- ✅ **Actual UDP communication** between nodes
- ✅ **Real rate limiting** - requests actually get blocked
- ✅ **Live algorithm switching** - changes backend behavior
- ✅ **Genuine metrics** - real packet counts, retransmissions
- ✅ **Professional URLs** - perfect for portfolio

## 🔍 Troubleshooting

### CORS Issues
If you get CORS errors, ensure your Java services include headers:
```java
exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
```

### Service Unavailable
If services show "OFFLINE":
1. Check Render dashboard - services should be "Live"
2. Test URLs directly in browser
3. Check Render service logs for errors
4. Verify environment variables are set correctly

### Build Failures
If Vercel build fails:
1. Check build logs in Vercel dashboard
2. Ensure `NODE_OPTIONS=--openssl-legacy-provider` is set
3. Verify all environment variables are configured

### Mixed Content (HTTP/HTTPS)
Ensure all URLs use HTTPS:
- ✅ `https://gateway-a-east.onrender.com`
- ❌ `http://gateway-a-east.onrender.com`

## 🎉 Success Indicators

Your integration is successful when:

✅ **Frontend loads without errors**  
✅ **All 3 nodes show "ONLINE" status**  
✅ **JWT generation works**  
✅ **Test gateway buttons respond**  
✅ **UDP metrics show real data**  
✅ **Algorithm switching changes behavior**  
✅ **Rate limiting actually blocks requests**  
✅ **Stress tests show realistic results**  

## 🌟 Portfolio Impact

With real Render backends, you can now demonstrate:

- **Full-stack development** - React + Java
- **Distributed systems** - Multi-node coordination  
- **Cloud deployment** - Professional hosting
- **Real-time systems** - Live UDP communication
- **Security** - JWT authentication
- **Scalability** - Multiple rate limiting algorithms
- **DevOps** - CI/CD with Vercel + Render

## 📊 Performance Notes

**Render Free Tier Limitations:**
- Services may "sleep" after 15 minutes of inactivity
- First request after sleep takes ~30 seconds to wake up
- This is normal and expected for free tier

**For Demos:**
- Send a "wake-up" request before important presentations
- Consider upgrading to paid tier for always-on services
- Or mention the "cold start" behavior as a cloud hosting feature

## 🚀 Next Steps

1. **Deploy to Render** (if not done yet)
2. **Update URLs** in configuration files
3. **Build and deploy** frontend to Vercel  
4. **Test thoroughly** - all features should work
5. **Document** your system for portfolio/interviews

Your distributed rate limiting system is now production-ready! 🎯



