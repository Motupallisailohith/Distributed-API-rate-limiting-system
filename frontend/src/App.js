import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Line } from 'react-chartjs-2';
import { simulateApiCall, getDemoMetrics } from './demoData';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

// Demo mode - simulate backend responses for portfolio demonstration
const API_BASE = process.env.REACT_APP_API_BASE || 'demo-mode';

function App() {
  const [nodeStatus, setNodeStatus] = useState(null);
  const [metrics, setMetrics] = useState(null);
  const [logs, setLogs] = useState([]);
  const [currentJWT, setCurrentJWT] = useState(null);
  const [algorithms, setAlgorithms] = useState({ current: '', available: [] });
  const [udpMetrics, setUdpMetrics] = useState(null);
  const [requestData, setRequestData] = useState({
    labels: [],
    datasets: [{
      label: 'Requests per Second',
      data: [],
      borderColor: '#00ff88',
      backgroundColor: 'rgba(0, 255, 136, 0.1)',
      tension: 0.4,
    }]
  });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchNodeStatus();
    fetchMetrics();
    fetchAlgorithms();
    const interval = setInterval(() => {
      fetchNodeStatus();
      fetchMetrics();
      fetchAlgorithms();
    }, 2000);
    return () => clearInterval(interval);
  }, []);

  const addLog = (message, type = 'info') => {
    const timestamp = new Date().toLocaleTimeString();
    const newLog = {
      id: Date.now(),
      timestamp,
      message,
      type
    };
    setLogs(prev => [...prev.slice(-19), newLog]);
  };

  const fetchNodeStatus = async () => {
    try {
      let response;
      if (API_BASE === 'demo-mode') {
        response = await simulateApiCall('/api/status');
      } else {
        response = await axios.get(`${API_BASE}/api/status`);
      }
      setNodeStatus(response.data);
      setIsLoading(false);
    } catch (error) {
      addLog(`Failed to fetch node status: ${error.message}`, 'error');
      setIsLoading(false);
    }
  };

  const fetchMetrics = async () => {
    try {
      let response;
      if (API_BASE === 'demo-mode') {
        response = await simulateApiCall('/api/metrics');
      } else {
        response = await axios.get(`${API_BASE}/api/metrics`);
      }
      setMetrics(response.data);
      // Extract UDP metrics if available
      if (response.data.udp) {
        setUdpMetrics(response.data.udp);
      }
    } catch (error) {
      addLog(`Failed to fetch metrics: ${error.message}`, 'error');
    }
  };

  const fetchAlgorithms = async () => {
    try {
      let response;
      if (API_BASE === 'demo-mode') {
        response = await simulateApiCall('/api/algorithm');
      } else {
        response = await axios.get(`${API_BASE}/api/algorithm`);
      }
      setAlgorithms(response.data);
    } catch (error) {
      addLog(`Failed to fetch algorithms: ${error.message}`, 'error');
    }
  };

  const switchAlgorithm = async (algorithmName) => {
    try {
      addLog(`🔄 Switching to ${algorithmName} algorithm...`, 'info');
      const response = await axios.post(`${API_BASE}/api/algorithm`, {
        algorithm: algorithmName
      }, {
        headers: { 'Content-Type': 'application/json' }
      });
      
      if (response.data.status === 'success') {
        addLog(`✅ Algorithm switched to ${algorithmName}`, 'success');
        fetchAlgorithms(); // Refresh algorithm status
      }
    } catch (error) {
      addLog(`❌ Failed to switch algorithm: ${error.message}`, 'error');
    }
  };

  const generateJWT = () => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({
      sub: `api-key-${Math.random().toString(36).substr(2, 9)}`,
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 3600,
      purpose: 'demo'
    }));
    const signature = `demo-signature-${Math.random().toString(36).substr(2, 16)}`;
    const jwt = `${header}.${payload}.${signature}`;
    
    setCurrentJWT(jwt);
    addLog('🔑 JWT token generated for testing', 'success');
  };

  const testWithJWT = async () => {
    if (!currentJWT) {
      addLog('❌ Please generate JWT token first', 'error');
      return;
    }

    try {
      addLog('🧪 Testing /api/data with JWT...', 'info');
      const response = await axios.post(`${API_BASE}/api/data`, {}, {
        headers: {
          'Authorization': `Bearer ${currentJWT}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.status === 200) {
        addLog(`✅ Request ALLOWED - ${JSON.stringify(response.data)}`, 'success');
        updateChart();
      }
    } catch (error) {
      if (error.response?.status === 429) {
        addLog(`🚫 Request BLOCKED - Rate limit exceeded`, 'error');
      } else if (error.response?.status === 401) {
        addLog(`🔒 Authentication FAILED - Invalid JWT`, 'error');
      } else {
        addLog(`❌ Request failed: ${error.message}`, 'error');
      }
    }
  };

  const testWithoutJWT = async () => {
    try {
      addLog('🔒 Testing /api/data without JWT (should fail)...', 'info');
      await axios.post(`${API_BASE}/api/data`);
    } catch (error) {
      if (error.response?.status === 401) {
        addLog('✅ Security working - JWT required', 'success');
      } else {
        addLog(`⚠️ Unexpected response: ${error.message}`, 'error');
      }
    }
  };

  const stressTest = async () => {
    if (!currentJWT) {
      addLog('❌ Please generate JWT token first', 'error');
      return;
    }

    addLog('🔥 Starting stress test (10 rapid requests)...', 'info');
    let successCount = 0;
    let blockedCount = 0;

    for (let i = 0; i < 10; i++) {
      try {
        const response = await axios.post(`${API_BASE}/api/data`, {}, {
          headers: { 'Authorization': `Bearer ${currentJWT}` }
        });
        if (response.status === 200) {
          successCount++;
        }
      } catch (error) {
        if (error.response?.status === 429) {
          blockedCount++;
        }
      }
      await new Promise(resolve => setTimeout(resolve, 100));
    }

    addLog(`🔥 Stress test complete - Allowed: ${successCount}, Blocked: ${blockedCount}`, 'info');
    updateChart();
  };

  const updateChart = () => {
    const now = new Date().toLocaleTimeString();
    setRequestData(prev => ({
      ...prev,
      labels: [...prev.labels.slice(-9), now],
      datasets: [{
        ...prev.datasets[0],
        data: [...prev.datasets[0].data.slice(-9), Math.floor(Math.random() * 50) + 10]
      }]
    }));
  };

  if (isLoading) {
    return (
      <div className="container">
        <div className="loading">
          <h2>🔄 Loading Distributed Rate Limiter Dashboard...</h2>
        </div>
      </div>
    );
  }

  return (
    <div className="container">
      <div className="header">
        <h1>🔥 Distributed Rate Limiter Dashboard</h1>
        <p><strong>Real-time Monitoring & Testing Interface</strong></p>
        <p>Showcasing distributed systems, rate limiting algorithms, and UDP synchronization</p>
      </div>

      <div className="status-grid">
        <div className="status-card">
          <h3>🎯 Node Status</h3>
          <div className="metric">
            <span className="status-indicator status-online"></span>
            Status: <strong>ONLINE</strong>
          </div>
          <div className="metric">
            Algorithm: <strong>{algorithms?.current || nodeStatus?.algorithm || 'Loading...'}</strong>
          </div>
          <div className="metric">
            Port: <strong>{nodeStatus?.port || 'Loading...'}</strong>
          </div>
        </div>

        <div className="status-card">
          <h3>📊 System Metrics</h3>
          <div className="metric">
            Status: <strong>{metrics?.status || 'Loading...'}</strong>
          </div>
          <div className="metric">
            Uptime: <strong>{metrics?.uptime ? new Date(metrics.uptime).toLocaleString() : 'Loading...'}</strong>
          </div>
        </div>

        <div className="status-card">
          <h3>🔒 Security Features</h3>
          <div className="metric">JWT Auth: <span style={{color: '#00ff88'}}>🔒 ENABLED</span></div>
          <div className="metric">CORS: <span style={{color: '#00ff88'}}>✅ CONFIGURED</span></div>
          <div className="metric">Rate Limiting: <span style={{color: '#00ff88'}}>🛡️ ACTIVE</span></div>
        </div>

        <div className="status-card">
          <h3>🌐 UDP Coordination</h3>
          <div className="metric">
            Packets Sent: <strong>{udpMetrics?.packetsSent || 0}</strong>
          </div>
          <div className="metric">
            Packets Received: <strong>{udpMetrics?.packetsReceived || 0}</strong>
          </div>
          <div className="metric">
            ACKs Received: <strong>{udpMetrics?.acksReceived || 0}</strong>
          </div>
          <div className="metric">
            Retransmissions: <strong>{udpMetrics?.retransmissions || 0}</strong>
          </div>
          <div className="metric">
            Deltas Applied: <strong>{udpMetrics?.deltasApplied || 0}</strong>
          </div>
          <div className="metric">
            Pending Packets: <strong>{udpMetrics?.pendingPackets || 0}</strong>
          </div>
        </div>

        <div className="status-card">
          <h3>⚙️ Algorithm Control</h3>
          <div className="metric">
            Current: <strong>{algorithms?.current || 'Loading...'}</strong>
          </div>
          <div className="algorithm-buttons">
            {algorithms?.available?.map(algo => (
              <button 
                key={algo}
                className={`algorithm-btn ${algorithms.current === algo ? 'active' : ''}`}
                onClick={() => switchAlgorithm(algo)}
                disabled={algorithms.current === algo}
              >
                {algo === 'tokenbucket' ? '🪣 Token Bucket' : 
                 algo === 'leakybucket' ? '💧 Leaky Bucket' : 
                 algo === 'slidingwindow' ? '🪟 Sliding Window' : algo}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="chart-container">
        <h3>📈 Request Rate Visualization</h3>
        <Line 
          data={requestData}
          options={{
            responsive: true,
            plugins: {
              legend: {
                labels: { color: 'white' }
              }
            },
            scales: {
              x: {
                ticks: { color: 'white' },
                grid: { color: 'rgba(255,255,255,0.1)' }
              },
              y: {
                ticks: { color: 'white' },
                grid: { color: 'rgba(255,255,255,0.1)' }
              }
            }
          }}
        />
      </div>

      <div className="test-section">
        <h3>🧪 Interactive API Testing</h3>
        <p><strong>Endpoint:</strong> <code>POST {API_BASE}/api/data</code></p>
        <p><strong>Authentication:</strong> Requires Bearer JWT token</p>

        <div className="button-group">
          <button className="btn" onClick={generateJWT}>
            🔑 Generate JWT
          </button>
          <button className="btn" onClick={testWithJWT} disabled={!currentJWT}>
            🧪 Test with JWT
          </button>
          <button className="btn warning" onClick={testWithoutJWT}>
            ⚠️ Test without JWT
          </button>
          <button className="btn info" onClick={fetchNodeStatus}>
            📊 Refresh Status
          </button>
          <button className="btn" onClick={stressTest} disabled={!currentJWT}>
            🔥 Stress Test
          </button>
        </div>

        {currentJWT && (
          <div className="jwt-display">
            <strong>🔑 Current JWT Token:</strong><br />
            {currentJWT}
          </div>
        )}

        <div className="log-container">
          {logs.length === 0 ? (
            <div className="log-entry log-info">
              <strong>[System]</strong> Dashboard ready - Generate JWT and start testing!
            </div>
          ) : (
            logs.map(log => (
              <div key={log.id} className={`log-entry log-${log.type}`}>
                <strong>[{log.timestamp}]</strong> {log.message}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

export default App;
