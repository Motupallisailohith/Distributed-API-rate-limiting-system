# 🌐 Multi-Node Distributed Rate Limiter Demo

## Overview
This demo showcases a **distributed rate limiting system** with real UDP coordination between multiple gateway nodes. Each node can independently receive API requests while synchronizing rate limit state via UDP packets.

## 🎯 Demo Features

### ✅ **Multi-Node Architecture**
- **3 Gateway Nodes** running simultaneously
- **Real UDP Coordination** between nodes
- **Distributed State Synchronization** via reliable UDP protocol
- **Independent Algorithm Switching** per node or globally

### ✅ **Supported Rate Limiting Algorithms**
- 🪣 **Token Bucket** - Fixed capacity with periodic refill
- 💧 **Leaky Bucket** - Constant rate processing with overflow protection  
- 🪟 **Sliding Window** - Time-based request counting with rolling window

### ✅ **Real-time Visualization**
- **UDP Packet Metrics** (sent, received, retransmissions, ACKs)
- **Live Algorithm Status** across all nodes
- **Distributed Coordination Charts** showing inter-node communication
- **Request Success/Block Rates** per node

## 🚀 Quick Start

### 1. **Start Multi-Node Demo**
```bash
# Start all 3 nodes with Token Bucket algorithm
.\start-distributed-demo.bat tokenbucket

# Or start with different algorithms:
.\start-distributed-demo.bat leakybucket
.\start-distributed-demo.bat slidingwindow
```

### 2. **Access Multi-Node Dashboard**
```
Single Node View: http://localhost:3000
Multi-Node View:  http://localhost:3000?mode=multi
```

### 3. **Node Endpoints**
- **Node 1 (Gateway A)**: http://localhost:8091 (UDP: 7001)
- **Node 2 (Gateway B)**: http://localhost:8092 (UDP: 7002)  
- **Node 3 (Gateway C)**: http://localhost:8093 (UDP: 7003)

## 🧪 Testing Scenarios

### **Scenario 1: Basic Distributed Rate Limiting**
```bash
# Generate JWT in frontend, then test each node
curl -Method POST -Uri http://localhost:8091/api/data -Headers @{"Authorization"="Bearer <JWT>"}
curl -Method POST -Uri http://localhost:8092/api/data -Headers @{"Authorization"="Bearer <JWT>"}
curl -Method POST -Uri http://localhost:8093/api/data -Headers @{"Authorization"="Bearer <JWT>"}
```

### **Scenario 2: Algorithm Switching**
```bash
# Switch Node 1 to Leaky Bucket
curl -Method POST -Uri http://localhost:8091/api/algorithm -Body '{"algorithm":"leakybucket"}' -ContentType "application/json"

# Check all nodes' algorithms
curl http://localhost:8091/api/algorithm
curl http://localhost:8092/api/algorithm  
curl http://localhost:8093/api/algorithm
```

### **Scenario 3: UDP Coordination Monitoring**
```bash
# View UDP metrics for each node
curl http://localhost:8091/api/metrics | ConvertFrom-Json | Select-Object -ExpandProperty udp
curl http://localhost:8092/api/metrics | ConvertFrom-Json | Select-Object -ExpandProperty udp
curl http://localhost:8093/api/metrics | ConvertFrom-Json | Select-Object -ExpandProperty udp
```

### **Scenario 4: Distributed Stress Testing**
Use the frontend "Distributed Stress Test" button to send rapid requests across all nodes and observe:
- **Rate limit coordination** between nodes
- **UDP packet exchange** for state synchronization
- **Retransmission behavior** when packets are lost
- **Algorithm-specific behavior** differences

## 📊 What to Observe

### **UDP Coordination Metrics**
- **Packets Sent**: Outgoing UDP coordination messages
- **Packets Received**: Incoming UDP messages from peer nodes
- **ACKs Received**: Acknowledgments for reliable delivery
- **Retransmissions**: Retry attempts for lost packets
- **Deltas Applied**: Rate limit state updates from peers
- **Pending Packets**: Unacknowledged outgoing messages

### **Algorithm Behaviors**
- **Token Bucket**: Burst handling with refill rate
- **Leaky Bucket**: Smooth rate processing with queue overflow
- **Sliding Window**: Time-based request counting with window sliding

### **Distributed Coordination**
- Watch how rate limit state synchronizes across nodes
- Observe UDP retransmissions when network is congested
- See how different algorithms handle distributed load

## 🎮 Interactive Demo Flow

1. **Start Demo**: Launch all 3 nodes with `start-distributed-demo.bat`
2. **Open Dashboard**: Visit http://localhost:3000?mode=multi
3. **Generate JWT**: Click "Generate JWT" in the frontend
4. **Test Individual Nodes**: Click "Test Gateway A/B/C" buttons
5. **Switch Algorithms**: Use "Algorithm Control" buttons to switch all nodes
6. **Run Stress Test**: Click "Distributed Stress Test" to see coordination
7. **Monitor UDP**: Watch real-time UDP metrics and charts
8. **Experiment**: Try different algorithms and observe behavior differences

## 🔧 Configuration

### **Node Configuration** (`config/demo-nodes.yml`)
```yaml
127.0.0.1:7001  # Node 1 - Gateway A
127.0.0.1:7002  # Node 2 - Gateway B  
127.0.0.1:7003  # Node 3 - Gateway C
```

### **Individual Node Startup**
```bash
# Manual node startup (if needed)
.\run-node1.bat tokenbucket  # Node 1 on port 8091
.\run-node2.bat leakybucket  # Node 2 on port 8092  
.\run-node3.bat slidingwindow # Node 3 on port 8093
```

## 🎯 Interview Demonstration Points

### **Technical Concepts Demonstrated**
1. **Distributed Systems**: Multiple nodes coordinating via UDP
2. **Rate Limiting Algorithms**: Three different approaches with real implementations
3. **Network Protocols**: Custom reliable UDP with ACKs, retransmissions, CRC32
4. **Real-time Monitoring**: Live metrics and visualization
5. **Fault Tolerance**: Retransmission and error handling
6. **Algorithm Comparison**: Side-by-side behavior analysis

### **System Architecture Highlights**
- **Microservices Pattern**: Independent gateway nodes
- **Event-Driven Architecture**: UDP-based state synchronization
- **Pluggable Algorithms**: Runtime algorithm switching
- **Observability**: Comprehensive metrics and logging
- **Scalability**: Easy to add more nodes

This demo provides a comprehensive view of distributed rate limiting with real UDP coordination, perfect for demonstrating advanced system design concepts in interviews or technical presentations.

