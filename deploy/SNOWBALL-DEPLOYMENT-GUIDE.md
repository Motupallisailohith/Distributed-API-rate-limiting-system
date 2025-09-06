# 🏔️ Snowball Server Deployment Guide

## 📋 Prerequisites
- GSU account with Snowball access
- WinSCP or FileZilla for file transfer
- SSH client (PuTTY on Windows)

## 🚀 Step-by-Step Deployment

### Step 1: Prepare Local Files
```bash
# Build the JAR file locally
mvn clean package

# Or compile manually
javac -cp ".:lib/*" -d target/classes src/main/java/com/Motupallisailohith/ratelimit/**/*.java
cd target/classes
jar cvf ../../distributed-rate-limiter.jar .
cd ../..
```

### Step 2: Upload Files to Snowball

**Using WinSCP:**
1. Connect to `snowball.cs.gsu.edu:22` with your GSU credentials
2. Create directory: `/home/[username]/distributed-rate-limiter/`
3. Upload these files:
   - `distributed-rate-limiter.jar` (or entire `src/` folder)
   - `deploy/snowball-deploy.sh`
   - `config/demo-nodes.yml` → rename to `snowball-nodes.yml`

**Using SCP (Linux/Mac):**
```bash
scp distributed-rate-limiter.jar [username]@snowball.cs.gsu.edu:~/distributed-rate-limiter/
scp deploy/snowball-deploy.sh [username]@snowball.cs.gsu.edu:~/distributed-rate-limiter/
scp config/demo-nodes.yml [username]@snowball.cs.gsu.edu:~/distributed-rate-limiter/config/snowball-nodes.yml
```

### Step 3: SSH into Snowball and Deploy
```bash
# SSH into the server
ssh [username]@snowball.cs.gsu.edu

# Navigate to your directory
cd ~/distributed-rate-limiter

# Make deployment script executable
chmod +x snowball-deploy.sh

# Run deployment
./snowball-deploy.sh

# Start all nodes
./start-all.sh

# Check status
./status.sh
```

### Step 4: Test the Deployment
```bash
# Test each node
curl http://snowball.cs.gsu.edu:8091/api/status
curl http://snowball.cs.gsu.edu:8092/api/status  
curl http://snowball.cs.gsu.edu:8093/api/status

# Test with authentication
curl -H "Authorization: Bearer [JWT_TOKEN]" \
     -X POST http://snowball.cs.gsu.edu:8091/api/data

# View real-time logs
tail -f logs/node*.log
```

### Step 5: Update Vercel Frontend

Update your frontend environment variables:
```bash
# In frontend/.env.production
REACT_APP_API_BASE=http://snowball.cs.gsu.edu:8091
REACT_APP_NODE1_URL=http://snowball.cs.gsu.edu:8091
REACT_APP_NODE2_URL=http://snowball.cs.gsu.edu:8092
REACT_APP_NODE3_URL=http://snowball.cs.gsu.edu:8093
```

Then redeploy to Vercel:
```bash
cd frontend
npm run build:win
vercel --prod
```

## 🛠️ Management Commands

### Start/Stop Services
```bash
./start-all.sh      # Start all nodes
./stop-all.sh       # Stop all nodes
./status.sh         # Check status
```

### Individual Node Control
```bash
./start-node1.sh    # Start only Gateway A
./start-node2.sh    # Start only Gateway B  
./start-node3.sh    # Start only Gateway C
```

### Monitoring
```bash
# View logs
tail -f logs/node1.log
tail -f logs/node2.log
tail -f logs/node3.log

# Check processes
ps aux | grep java

# Check ports
netstat -tlnp | grep ":809[1-3]"
```

## 🌐 Your Live URLs
After deployment, your system will be accessible at:

**Backend APIs:**
- Node 1: `http://snowball.cs.gsu.edu:8091/api/`
- Node 2: `http://snowball.cs.gsu.edu:8092/api/`  
- Node 3: `http://snowball.cs.gsu.edu:8093/api/`

**Frontend Dashboard:**
- Vercel: `https://[your-app].vercel.app/?mode=multi`

## 🔧 Troubleshooting

### Port Issues
If ports 8091-8093 are blocked, try different ports:
```bash
# Edit the startup scripts to use different ports
# Common alternatives: 8080, 8888, 9000-9003
```

### CORS Issues
If Vercel can't connect to Snowball, add CORS headers in your Java HTTP server:
```java
exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
```

### Process Limits
If processes get killed automatically:
```bash
# Use screen or tmux to keep processes alive
screen -S rate-limiter
./start-all.sh
# Press Ctrl+A, then D to detach
```

## 🎯 Success Indicators
✅ All 3 nodes respond to `/api/status`  
✅ UDP communication between nodes works  
✅ Vercel frontend connects to Snowball backend  
✅ Real-time metrics display correctly  
✅ Algorithm switching works across nodes  
✅ Rate limiting actually blocks requests  

## 🚨 Emergency Commands
```bash
# Kill all Java processes (nuclear option)
pkill -f java

# Clean up everything
./stop-all.sh
rm -rf logs/*
rm -f *.pid
```



