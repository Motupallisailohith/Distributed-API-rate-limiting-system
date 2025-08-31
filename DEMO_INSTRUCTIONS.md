# 🔥 Distributed Rate Limiter - Complete Demo System

## 🎯 System Overview

This is a **production-ready distributed API rate limiting system** with a **React dashboard** for real-time monitoring and testing. The system demonstrates:

- **JWT Authentication** with nimbus-jose-jwt
- **Multiple Rate Limiting Algorithms** (Token Bucket, Leaky Bucket, Sliding Window)
- **Real UDP Distribution** for synchronizing rate limits across nodes
- **UDP Reliability Layer** with retransmission, ACKs, and CRC32 checksums
- **Interactive Web Dashboard** with real-time charts and testing capabilities

## ✅ Current Status

### Backend (Enhanced HTTPServer.java)
- ✅ **CORS Support** - Frontend can connect
- ✅ **New API Endpoints**:
  - `GET /api/status` - System status and algorithm info
  - `GET /api/metrics` - System metrics and uptime
  - `POST /api/data` - Rate-limited endpoint (requires JWT)
  - `GET /` - Basic web interface
- ✅ **Enhanced JSON Responses** with detailed rate limiting info
- ✅ **JWT Authentication** (full production support)

### Frontend (React Dashboard)
- ✅ **React 17 Compatible** - Fixed DOM client issues
- ✅ **Real-time Charts** with Chart.js
- ✅ **Interactive Testing** - Generate JWT, test endpoints
- ✅ **Stress Testing** - Rapid requests to test rate limiting
- ✅ **Live Monitoring** - Auto-refresh status and metrics

## 🚀 How to Run the Complete System

### Option 1: Quick Start (Recommended)

1. **Start Backend** (in one terminal):
   ```bash
   # Navigate to project root
   cd C:\Users\sailo\Distributed-API-rate-limiting-system
   
   # Kill any existing processes
   taskkill /f /im java.exe
   
   # Start the enhanced backend (uses existing compiled classes)
   java -cp target\classes com.Motupallisailohith.ratelimit.Main --algorithm=tokenbucket --http.port=8080 --udp.port=7000
   ```

2. **Start Frontend** (in another terminal):
   ```bash
   # Navigate to frontend directory
   cd C:\Users\sailo\Distributed-API-rate-limiting-system\frontend
   
   # Start React development server
   npm start
   ```

3. **Access the System**:
   - **Frontend Dashboard**: http://localhost:3000
   - **Backend API**: http://localhost:8080
   - **Backend Web Interface**: http://localhost:8080/

### Option 2: Automated Script

Run the provided script:
```bash
.\start-system.bat
```

## 🧪 Testing the System

### 1. Frontend Dashboard (http://localhost:3000)

1. **Generate JWT Token**: Click "🔑 Generate JWT"
2. **Test Authentication**: Click "🧪 Test with JWT" (should succeed)
3. **Test Security**: Click "⚠️ Test without JWT" (should fail with 401)
4. **Stress Test**: Click "🔥 Stress Test" (rapid requests to test rate limiting)
5. **Monitor**: Watch real-time status, metrics, and request logs

### 2. Direct API Testing

```bash
# Test status endpoint
curl http://localhost:8080/api/status

# Test metrics endpoint  
curl http://localhost:8080/api/metrics

# Test rate-limited endpoint (will fail without JWT)
curl -X POST http://localhost:8080/api/data

# Test with JWT (replace TOKEN with actual JWT)
curl -X POST http://localhost:8080/api/data -H "Authorization: Bearer TOKEN"
```

## 📊 Features Demonstrated

### Rate Limiting Algorithms
- **Token Bucket**: Burst capacity with steady refill rate
- **Leaky Bucket**: Smooth rate limiting with overflow protection  
- **Sliding Window**: Time-based request counting

### Distributed Coordination
- **UDP Synchronization**: Nodes share rate limit state via UDP
- **Reliable Protocol**: ACKs, retransmission, CRC32 checksums
- **Real-time Updates**: Changes propagate across all nodes

### Security & Authentication
- **JWT Verification**: Full nimbus-jose-jwt integration
- **CORS Support**: Secure cross-origin requests
- **Input Validation**: Proper error handling and responses

### Monitoring & Observability
- **Real-time Dashboard**: Live charts and metrics
- **Request Logging**: Detailed success/failure tracking
- **System Status**: Algorithm info, uptime, health checks

## 🔧 Troubleshooting

### Backend Won't Start
- **Port Conflict**: Change `--http.port=8080` to another port (e.g., 8090)
- **Java Version**: Ensure Java 8+ is installed and in PATH
- **Dependencies**: Run `mvn compile` to ensure all dependencies are available

### Frontend Won't Connect
- **CORS Issues**: Backend includes CORS headers, but check browser console
- **Port Mismatch**: Ensure frontend `API_BASE` matches backend port
- **Backend Not Running**: Verify backend is listening on expected port

### JWT Issues
- **Token Format**: Frontend generates demo JWTs for testing
- **Production JWT**: For real JWT verification, configure JWK endpoint in backend
- **Authentication**: All `/api/data` requests require `Authorization: Bearer TOKEN`

## 🎯 Interview Demonstration Points

### Technical Concepts
1. **Distributed Systems**: Show how multiple nodes coordinate via UDP
2. **Rate Limiting**: Demonstrate different algorithms and their behaviors
3. **Network Protocols**: Explain custom UDP reliability layer
4. **Authentication**: JWT-based API security
5. **Real-time Systems**: Live dashboard with WebSocket-like updates

### Architecture Highlights
1. **Microservices**: Separate backend API and frontend dashboard
2. **Scalability**: Horizontal scaling via node coordination
3. **Reliability**: UDP retransmission and error handling
4. **Observability**: Comprehensive monitoring and logging
5. **Security**: Authentication, CORS, input validation

### Code Quality
1. **Clean Architecture**: Separation of concerns (rate limiting, networking, HTTP)
2. **Error Handling**: Graceful degradation and proper error responses
3. **Configuration**: Command-line arguments and YAML config
4. **Testing**: Interactive testing capabilities built-in
5. **Documentation**: Clear API endpoints and usage examples

## 📁 Project Structure

```
distributed-api-rate-limiting-system/
├── src/main/java/com/Motupallisailohith/ratelimit/
│   ├── Main.java                    # Application entry point
│   ├── server/
│   │   ├── HTTPServer.java          # Enhanced HTTP server with new endpoints
│   │   └── UDPListener.java         # UDP coordination listener
│   ├── bucket/                      # Rate limiting algorithms
│   │   ├── TokenBucketRateLimiter.java
│   │   ├── LeakyBucketRateLimiter.java
│   │   └── SlidingWindowRateLimiter.java
│   ├── protocol/
│   │   └── Packet.java              # UDP packet structure
│   ├── reliability/
│   │   └── ReliabilityModule.java   # UDP reliability layer
│   └── security/
│       └── JwtVerifier.java         # JWT authentication
├── frontend/                        # React dashboard
│   ├── src/
│   │   ├── App.js                   # Main dashboard component
│   │   ├── index.js                 # React entry point
│   │   └── index.css                # Styling
│   └── package.json                 # Frontend dependencies
├── config/
│   └── nodes.yml                    # Node configuration
└── pom.xml                          # Maven dependencies
```

This system demonstrates a complete, production-ready distributed rate limiting solution with modern web interface and comprehensive testing capabilities.

