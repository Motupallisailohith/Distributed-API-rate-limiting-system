# 🎨 Render.com Deployment Guide

## 🎯 Why Render?
- ✅ **750 hours/month free** (enough for 24/7 operation)
- ✅ **Docker support** - full control over environment
- ✅ **Multiple services** - perfect for distributed system
- ✅ **Auto-deploy** - GitHub integration
- ✅ **Professional URLs** - great for portfolio
- ✅ **Health checks** - automatic monitoring

## 🚀 Step-by-Step Deployment

### Step 1: Prepare Your Repository
1. **Push to GitHub** (if not already done):
```bash
git add .
git commit -m "Add Render deployment configuration"
git push origin main
```

### Step 2: Create Render Account
1. Go to [render.com](https://render.com)
2. **Sign up with GitHub** (recommended)
3. Authorize Render to access your repositories

### Step 3: Deploy Using Blueprint (Easiest Method)

**Option A: One-Click Blueprint Deploy**
1. In Render dashboard, click **"New +"**
2. Select **"Blueprint"**
3. Connect your GitHub repository
4. Render will automatically detect `render.yaml` and deploy all 3 services!

**Option B: Manual Service Creation**
If blueprint doesn't work, create services manually:

#### Service 1: Gateway A (East Coast)
1. Click **"New +"** → **"Web Service"**
2. Connect your GitHub repo
3. **Settings:**
   - **Name**: `gateway-a-east`
   - **Environment**: `Docker`
   - **Plan**: `Free`
   - **Dockerfile Path**: `./Dockerfile`
   - **Health Check Path**: `/api/status`

4. **Environment Variables:**
   ```
   ALGORITHM=tokenbucket
   UDP_PORT=7001
   NODE_ID=node1
   CONFIG_FILE=config/demo-nodes.yml
   ```

#### Service 2: Gateway B (West Coast)
Repeat above with:
- **Name**: `gateway-b-west`
- **Environment Variables:**
  ```
  ALGORITHM=leakybucket
  UDP_PORT=7002
  NODE_ID=node2
  CONFIG_FILE=config/demo-nodes.yml
  ```

#### Service 3: Gateway C (International)
Repeat above with:
- **Name**: `gateway-c-intl`
- **Environment Variables:**
  ```
  ALGORITHM=slidingwindow
  UDP_PORT=7003
  NODE_ID=node3
  CONFIG_FILE=config/demo-nodes.yml
  ```

### Step 4: Monitor Deployment
1. Watch the **build logs** in Render dashboard
2. Wait for all services to show **"Live"** status
3. Test each endpoint:
   - `https://gateway-a-east.onrender.com/api/status`
   - `https://gateway-b-west.onrender.com/api/status`
   - `https://gateway-c-intl.onrender.com/api/status`

### Step 5: Update Frontend Configuration

Update your Vercel frontend to use Render URLs:

```javascript
// In frontend/src/MultiNodeApp.js
const NODES = [
  {
    id: 'node1',
    name: 'Gateway A (East Coast)',
    url: process.env.REACT_APP_API_BASE === 'demo-mode' 
      ? 'demo-mode' 
      : 'https://gateway-a-east.onrender.com',
    color: '#00ff88'
  },
  {
    id: 'node2', 
    name: 'Gateway B (West Coast)',
    url: process.env.REACT_APP_API_BASE === 'demo-mode' 
      ? 'demo-mode' 
      : 'https://gateway-b-west.onrender.com',
    color: '#74b9ff'
  },
  {
    id: 'node3',
    name: 'Gateway C (International)', 
    url: process.env.REACT_APP_API_BASE === 'demo-mode' 
      ? 'demo-mode' 
      : 'https://gateway-c-intl.onrender.com',
    color: '#ff6b6b'
  }
];
```

### Step 6: Create Production Environment File

```bash
# Create frontend/.env.production
echo "REACT_APP_API_BASE=https://gateway-a-east.onrender.com" > frontend/.env.production
```

### Step 7: Redeploy Frontend to Vercel

```bash
cd frontend
npm run build:win
vercel --prod
```

## 🌐 Your Live System URLs

After successful deployment:

**Backend Services:**
- **Gateway A**: `https://gateway-a-east.onrender.com/api/status`
- **Gateway B**: `https://gateway-b-west.onrender.com/api/status`
- **Gateway C**: `https://gateway-c-intl.onrender.com/api/status`

**Frontend Dashboard:**
- **Single Node**: `https://your-app.vercel.app`
- **Multi Node**: `https://your-app.vercel.app/?mode=multi`

**API Endpoints:**
- Status: `/api/status`
- Metrics: `/api/metrics`
- Data: `/api/data` (POST with JWT)
- Algorithm: `/api/algorithm` (POST)

## 💰 Cost Analysis
- **Render**: FREE (750 hours/month covers 24/7 operation)
- **Vercel**: FREE (hobby plan)
- **Total**: **$0/month** 🎉

## 🔧 Troubleshooting

### Build Failures
If Docker build fails:
1. Check **build logs** in Render dashboard
2. Ensure `pom.xml` is correct
3. Verify Java 8 compatibility

### Memory Issues
If services crash due to memory:
1. Reduce heap size in `Dockerfile`:
   ```dockerfile
   java -Xmx128m -jar app.jar
   ```

### CORS Issues
Ensure your Java server includes CORS headers:
```java
exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
```

### Service Communication
For UDP communication between services, you may need to:
1. Use Render's internal networking
2. Or implement HTTP-based coordination as fallback

## 🎯 Success Indicators
✅ All 3 Render services show "Live" status  
✅ Each service responds to `/api/status`  
✅ Vercel frontend connects to Render backends  
✅ Real rate limiting (not mocked data)  
✅ Algorithm switching works  
✅ JWT authentication functions  
✅ UDP metrics are real  

## 🚀 Advanced Features

### Auto-Deploy on Git Push
Render automatically redeploys when you push to GitHub!

### Custom Domains (Optional)
You can add custom domains in Render dashboard for professional URLs.

### Monitoring
Render provides built-in monitoring, logs, and health checks.

## 📊 Expected Timeline
- **Setup**: 10 minutes
- **First deployment**: 15-20 minutes
- **Frontend update**: 5 minutes
- **Total**: ~45 minutes to live system

Ready to deploy? Let's start with pushing your code to GitHub! 🚀



