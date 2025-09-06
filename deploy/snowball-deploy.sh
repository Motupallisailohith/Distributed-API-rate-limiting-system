#!/bin/bash
# Snowball Server Deployment Script
# Run this script on snowball.cs.gsu.edu after uploading files

echo "🚀 Starting Distributed Rate Limiter deployment on Snowball..."

# Create necessary directories
mkdir -p ~/distributed-rate-limiter/logs
mkdir -p ~/distributed-rate-limiter/config
cd ~/distributed-rate-limiter

# Check Java version
echo "📋 Checking Java version..."
java -version

# Compile the project (if source files are uploaded)
if [ -d "src" ]; then
    echo "🔨 Compiling Java sources..."
    find src -name "*.java" > sources.txt
    javac -cp ".:lib/*" -d classes @sources.txt
    
    # Create JAR file
    echo "📦 Creating JAR file..."
    cd classes
    jar cvf ../distributed-rate-limiter.jar .
    cd ..
fi

# Set up configuration
echo "⚙️ Setting up configuration..."
cat > config/snowball-nodes.yml << 'EOF'
nodes:
  - id: node1
    name: "Gateway A (East Coast)"
    host: "snowball.cs.gsu.edu"
    httpPort: 8091
    udpPort: 7001
    algorithm: "tokenbucket"
    color: "#00ff88"
  - id: node2
    name: "Gateway B (West Coast)" 
    host: "snowball.cs.gsu.edu"
    httpPort: 8092
    udpPort: 7002
    algorithm: "leakybucket"
    color: "#74b9ff"
  - id: node3
    name: "Gateway C (International)"
    host: "snowball.cs.gsu.edu"
    httpPort: 8093
    udpPort: 7003
    algorithm: "slidingwindow"
    color: "#ff6b6b"
EOF

# Create startup scripts
echo "📝 Creating startup scripts..."

# Node 1 startup script
cat > start-node1.sh << 'EOF'
#!/bin/bash
echo "🌎 Starting Gateway A (East Coast) on port 8091..."
cd ~/distributed-rate-limiter
nohup java -cp "distributed-rate-limiter.jar:lib/*" \
    com.Motupallisailohith.ratelimit.SimpleMain \
    --algorithm=tokenbucket \
    --http.port=8091 \
    --udp.port=7001 \
    --config=config/snowball-nodes.yml \
    > logs/node1.log 2>&1 &
echo $! > logs/node1.pid
echo "✅ Node 1 started with PID: $(cat logs/node1.pid)"
EOF

# Node 2 startup script  
cat > start-node2.sh << 'EOF'
#!/bin/bash
echo "🌏 Starting Gateway B (West Coast) on port 8092..."
cd ~/distributed-rate-limiter
nohup java -cp "distributed-rate-limiter.jar:lib/*" \
    com.Motupallisailohith.ratelimit.SimpleMain \
    --algorithm=leakybucket \
    --http.port=8092 \
    --udp.port=7002 \
    --config=config/snowball-nodes.yml \
    > logs/node2.log 2>&1 &
echo $! > logs/node2.pid
echo "✅ Node 2 started with PID: $(cat logs/node2.pid)"
EOF

# Node 3 startup script
cat > start-node3.sh << 'EOF'
#!/bin/bash
echo "🌍 Starting Gateway C (International) on port 8093..."
cd ~/distributed-rate-limiter
nohup java -cp "distributed-rate-limiter.jar:lib/*" \
    com.Motupallisailohith.ratelimit.SimpleMain \
    --algorithm=slidingwindow \
    --http.port=8093 \
    --udp.port=7003 \
    --config=config/snowball-nodes.yml \
    > logs/node3.log 2>&1 &
echo $! > logs/node3.pid
echo "✅ Node 3 started with PID: $(cat logs/node3.pid)"
EOF

# Master startup script
cat > start-all.sh << 'EOF'
#!/bin/bash
echo "🚀 Starting all distributed rate limiter nodes..."

# Stop any existing processes
./stop-all.sh

# Start all nodes with delays
./start-node1.sh
sleep 3
./start-node2.sh  
sleep 3
./start-node3.sh

echo ""
echo "🎯 All nodes started! Check status with: ./status.sh"
echo "📊 View logs with: tail -f logs/node*.log"
echo "🌐 Test endpoints:"
echo "   Node 1: http://snowball.cs.gsu.edu:8091/api/status"
echo "   Node 2: http://snowball.cs.gsu.edu:8092/api/status"  
echo "   Node 3: http://snowball.cs.gsu.edu:8093/api/status"
EOF

# Stop script
cat > stop-all.sh << 'EOF'
#!/bin/bash
echo "🛑 Stopping all distributed rate limiter nodes..."

for i in 1 2 3; do
    if [ -f "logs/node$i.pid" ]; then
        PID=$(cat logs/node$i.pid)
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            echo "✅ Stopped node $i (PID: $PID)"
        else
            echo "⚠️ Node $i was not running"
        fi
        rm -f logs/node$i.pid
    fi
done

# Cleanup any remaining Java processes on our ports
pkill -f "http.port=809[1-3]" 2>/dev/null || true
echo "🧹 Cleanup complete"
EOF

# Status script
cat > status.sh << 'EOF'
#!/bin/bash
echo "📊 Distributed Rate Limiter Status"
echo "=================================="

for i in 1 2 3; do
    PORT=$((8090 + i))
    if [ -f "logs/node$i.pid" ]; then
        PID=$(cat logs/node$i.pid)
        if kill -0 $PID 2>/dev/null; then
            echo "✅ Node $i: RUNNING (PID: $PID, Port: $PORT)"
            # Test HTTP endpoint
            if curl -s "http://localhost:$PORT/api/status" > /dev/null; then
                echo "   🌐 HTTP endpoint: RESPONSIVE"
            else
                echo "   ⚠️ HTTP endpoint: NOT RESPONSIVE"
            fi
        else
            echo "❌ Node $i: STOPPED (PID file exists but process dead)"
        fi
    else
        echo "❌ Node $i: NOT STARTED"
    fi
done

echo ""
echo "🔍 Active Java processes:"
ps aux | grep java | grep -v grep || echo "No Java processes found"

echo ""
echo "📈 Port usage:"
netstat -tlnp 2>/dev/null | grep ":809[1-3]" || echo "No processes on ports 8091-8093"
EOF

# Make scripts executable
chmod +x *.sh

echo ""
echo "✅ Deployment scripts created successfully!"
echo ""
echo "📋 Next steps:"
echo "1. Upload your JAR file or source code to ~/distributed-rate-limiter/"
echo "2. Run: ./start-all.sh"
echo "3. Check status: ./status.sh"
echo "4. View logs: tail -f logs/node*.log"
echo ""
echo "🌐 Your endpoints will be:"
echo "   http://snowball.cs.gsu.edu:8091/api/status"
echo "   http://snowball.cs.gsu.edu:8092/api/status"
echo "   http://snowball.cs.gsu.edu:8093/api/status"



