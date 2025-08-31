# 🚂 Railway Deployment Guide

## 🎯 Why Railway?
- ✅ **Free $5/month credit** for students
- ✅ **Native Java support** - no Docker needed
- ✅ **Multiple services** - can run 3 nodes
- ✅ **Real domains** - professional URLs
- ✅ **Git deployment** - push to deploy

## 🚀 Deployment Steps

### Step 1: Create Railway Account
1. Go to [railway.app](https://railway.app)
2. Sign up with GitHub (recommended)
3. Verify your account

### Step 2: Install Railway CLI
```bash
# Windows (using npm)
npm install -g @railway/cli

# Or download from: https://railway.app/cli
```

### Step 3: Login and Deploy
```bash
# Login to Railway
railway login

# Initialize project
railway init

# Deploy your first service (Gateway A)
railway up

# Your app will be available at: https://your-app.railway.app
```

### Step 4: Deploy Multiple Services
```bash
# Create additional services for multi-node setup
railway service create gateway-b-west
railway service create gateway-c-intl

# Deploy to each service with different algorithms
railway up --service gateway-a-east
railway up --service gateway-b-west  
railway up --service gateway-c-intl
```

### Step 5: Configure Environment Variables
In Railway dashboard, set these for each service:

**Gateway A (East Coast):**
```
ALGORITHM=tokenbucket
HTTP_PORT=8091
UDP_PORT=7001
NODE_ID=node1
```

**Gateway B (West Coast):**
```
ALGORITHM=leakybucket
HTTP_PORT=8092
UDP_PORT=7002
NODE_ID=node2
```

**Gateway C (International):**
```
ALGORITHM=slidingwindow
HTTP_PORT=8093
UDP_PORT=7003
NODE_ID=node3
```

### Step 6: Update Frontend
Update your Vercel frontend to use Railway URLs:

```javascript
// In frontend/src/MultiNodeApp.js
const NODES = [
  {
    id: 'node1',
    name: 'Gateway A (East Coast)',
    url: 'https://gateway-a-east.railway.app',
    color: '#00ff88'
  },
  {
    id: 'node2', 
    name: 'Gateway B (West Coast)',
    url: 'https://gateway-b-west.railway.app',
    color: '#74b9ff'
  },
  {
    id: 'node3',
    name: 'Gateway C (International)', 
    url: 'https://gateway-c-intl.railway.app',
    color: '#ff6b6b'
  }
];
```

## 🌐 Your Live System URLs

After deployment:
- **Gateway A**: `https://gateway-a-east.railway.app/api/status`
- **Gateway B**: `https://gateway-b-west.railway.app/api/status`
- **Gateway C**: `https://gateway-c-intl.railway.app/api/status`
- **Frontend**: `https://your-app.vercel.app/?mode=multi`

## 💰 Cost Estimation
- **Railway**: $0-5/month (free tier covers small apps)
- **Vercel**: $0/month (free tier)
- **Total**: **FREE** for student projects!

## 🔧 Troubleshooting

### Memory Issues
If you get memory errors, reduce heap size:
```bash
# In railway.toml
startCommand = "java -Xmx128m -jar target/distributed-api-rate-limit-system-1.0-SNAPSHOT.jar"
```

### CORS Issues
Ensure your Java server includes CORS headers:
```java
exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
```

### Port Issues
Railway automatically assigns ports via `$PORT` environment variable.

## 🎯 Success Indicators
✅ All 3 Railway services respond to `/api/status`  
✅ Vercel frontend connects to Railway backends  
✅ Real UDP communication between nodes  
✅ Actual rate limiting (not mocked)  
✅ Algorithm switching works  
✅ Professional URLs for portfolio  

## 🚨 Alternative: Render.com
If Railway doesn't work, try Render.com:
1. Connect GitHub repo
2. Create 3 web services
3. Use same environment variables
4. Deploy with Docker or native buildpack
