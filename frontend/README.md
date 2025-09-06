# 🔥 Distributed Rate Limiter Dashboard

A **real-time React dashboard** for monitoring and testing the distributed rate limiting system.

## ✨ Features

### 🌐 **Real-time Monitoring**
- **Node Status**: Live status of backend nodes
- **System Metrics**: Performance and health metrics
- **Algorithm Display**: Current rate limiting algorithm

### 🧪 **Interactive Testing**
- **JWT Generation**: Create test tokens for API authentication
- **API Testing**: Test rate limiting with/without authentication
- **Stress Testing**: Send multiple requests to test rate limiting
- **Real-time Logs**: See all API responses and system events

### 📊 **Data Visualization**
- **Request Rate Charts**: Real-time line charts using Chart.js
- **Success/Failure Metrics**: Visual feedback on API calls
- **Performance Tracking**: Monitor system behavior over time

### 🔒 **Security Features**
- **JWT Authentication**: Demonstrates secure API access
- **CORS Support**: Proper cross-origin resource sharing
- **Error Handling**: Graceful handling of authentication failures

## 🚀 Quick Start

### Prerequisites
- Node.js 16+ installed
- Java backend running on port 8080

### Installation
```bash
# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build
```

### Usage
1. **Start the backend** first (port 8080)
2. **Open** http://localhost:3000
3. **Generate JWT** token using the dashboard
4. **Test API calls** and watch real-time responses
5. **Monitor metrics** and system performance

## 🛠️ Technology Stack

- **React 18** - Modern UI framework
- **Chart.js** - Beautiful real-time charts
- **Axios** - HTTP client for API calls
- **CSS3** - Modern styling with gradients and animations

## 📱 Responsive Design

The dashboard works perfectly on:
- 💻 **Desktop** - Full feature set
- 📱 **Mobile** - Optimized layout
- 📟 **Tablet** - Adaptive grid system

## 🎯 Demo Scenarios

### 1. **Basic Rate Limiting Demo**
1. Generate JWT token
2. Send single request → Success
3. Send multiple rapid requests → Some blocked
4. Watch real-time logs and charts

### 2. **Security Demo**
1. Test without JWT → Blocked (401)
2. Test with JWT → Success (200)
3. Demonstrates authentication layer

### 3. **Algorithm Performance**
1. Monitor request patterns
2. Observe rate limiting behavior
3. View success/failure rates

## 🔧 Configuration

### API Endpoints
```javascript
const API_BASE = 'http://localhost:8080';

// Available endpoints:
// POST /api/data     - Main rate limiting endpoint
// GET  /api/status   - Node status information
// GET  /api/metrics  - System metrics
```

### Customization
- **Colors**: Modify CSS variables in `index.css`
- **API Base**: Change `API_BASE` in `App.js`
- **Chart Settings**: Customize Chart.js options
- **Polling Interval**: Adjust refresh rate (default: 2 seconds)

## 📊 Dashboard Sections

### 🎯 **Node Status Card**
- Online/Offline indicator
- Current algorithm (Token Bucket, Leaky Bucket, etc.)
- Port information

### 📊 **System Metrics Card**
- System health status
- Uptime information
- Performance indicators

### 🔒 **Security Features Card**
- JWT authentication status
- CORS configuration
- Rate limiting status

### 📈 **Request Rate Chart**
- Real-time line chart
- Request frequency visualization
- Interactive Chart.js implementation

### 🧪 **Interactive Testing Panel**
- JWT token generation
- API testing buttons
- Real-time response logs
- Stress testing capabilities

## 🎨 UI/UX Features

### **Modern Design**
- Gradient backgrounds
- Glass-morphism effects
- Smooth animations
- Professional color scheme

### **Interactive Elements**
- Hover effects on cards
- Button animations
- Real-time status indicators
- Responsive feedback

### **Accessibility**
- High contrast colors
- Clear typography
- Keyboard navigation
- Screen reader friendly

## 🚀 Production Deployment

### Build for Production
```bash
npm run build
```

### Serve Static Files
```bash
# Using serve
npx serve -s build

# Using nginx
# Copy build/ contents to nginx html directory
```

### Environment Variables
```bash
REACT_APP_API_BASE=http://your-backend-url:8080
```

## 🎯 Interview Highlights

This dashboard demonstrates:

### **Technical Skills**
- ✅ **React Development** - Modern hooks and state management
- ✅ **API Integration** - RESTful API consumption
- ✅ **Real-time Updates** - Polling and live data
- ✅ **Data Visualization** - Chart.js integration
- ✅ **Responsive Design** - Mobile-first approach

### **System Understanding**
- ✅ **Distributed Systems** - Multi-node awareness
- ✅ **Rate Limiting** - Algorithm comprehension
- ✅ **Security** - JWT authentication flow
- ✅ **Monitoring** - Real-time system observation

### **Production Readiness**
- ✅ **Error Handling** - Graceful failure management
- ✅ **User Experience** - Intuitive interface design
- ✅ **Performance** - Optimized rendering
- ✅ **Scalability** - Component-based architecture

## 🎉 Result

A **professional-grade monitoring dashboard** that transforms your distributed rate limiting backend into an **interactive, visual demonstration** of advanced distributed systems concepts!

Perfect for technical interviews and system demonstrations! 🚀

