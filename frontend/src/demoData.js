// Demo data for portfolio demonstration when backend is not available
let demoCounters = {
  totalRequests: 1247,
  allowedRequests: 1174,
  blockedRequests: 73,
  packetsSent: 2891,
  packetsReceived: 2847,
  acksReceived: 2801,
  retransmissions: 46,
  deltasApplied: 2801,
  pendingPackets: 44
};

export const demoNodeStatus = {
  algorithm: "TokenBucketRateLimiter",
  status: "active",
  port: 8091,
  timestamp: Date.now(),
  mode: "demo"
};

export const getDemoMetrics = () => {
  // Simulate real-time changes
  demoCounters.totalRequests += Math.floor(Math.random() * 5);
  demoCounters.allowedRequests += Math.floor(Math.random() * 4);
  demoCounters.blockedRequests += Math.floor(Math.random() * 2);
  demoCounters.packetsSent += Math.floor(Math.random() * 8);
  demoCounters.packetsReceived += Math.floor(Math.random() * 7);
  demoCounters.acksReceived += Math.floor(Math.random() * 6);
  demoCounters.retransmissions += Math.floor(Math.random() * 2);
  demoCounters.deltasApplied += Math.floor(Math.random() * 5);
  demoCounters.pendingPackets = Math.max(0, demoCounters.pendingPackets + Math.floor(Math.random() * 3) - 1);

  return {
    uptime: "2h 15m 30s",
    totalRequests: demoCounters.totalRequests,
    allowedRequests: demoCounters.allowedRequests,
    blockedRequests: demoCounters.blockedRequests,
    successRate: ((demoCounters.allowedRequests / demoCounters.totalRequests) * 100).toFixed(1),
    requestsPerSecond: (12.5 + Math.random() * 5).toFixed(1),
    udp: {
      packetsSent: demoCounters.packetsSent,
      packetsReceived: demoCounters.packetsReceived,
      acksReceived: demoCounters.acksReceived,
      retransmissions: demoCounters.retransmissions,
      deltasApplied: demoCounters.deltasApplied,
      pendingPackets: demoCounters.pendingPackets,
      peerCount: 3
    }
  };
};

export const demoAlgorithms = {
  current: "tokenbucket",
  available: ["tokenbucket", "leakybucket", "slidingwindow"]
};

// Simulate API responses
export const simulateApiCall = (endpoint, method = 'GET', data = null) => {
  return new Promise((resolve, reject) => {
    // Simulate network delay
    setTimeout(() => {
      if (Math.random() < 0.1) {
        // 10% chance of rate limiting for demo
        reject({ response: { status: 429, data: { message: "Rate limited (demo)" } } });
        return;
      }

      switch (endpoint) {
        case '/api/status':
          resolve({ data: demoNodeStatus });
          break;
        case '/api/metrics':
          resolve({ data: getDemoMetrics() });
          break;
        case '/api/algorithm':
          resolve({ data: demoAlgorithms });
          break;
        case '/api/data':
          if (method === 'POST') {
            resolve({ 
              data: { 
                status: "allowed", 
                algorithm: "TokenBucketRateLimiter",
                remaining: Math.floor(Math.random() * 100),
                timestamp: Date.now()
              } 
            });
          }
          break;
        default:
          reject({ message: "Demo endpoint not found" });
      }
    }, 200 + Math.random() * 300); // 200-500ms delay
  });
};
