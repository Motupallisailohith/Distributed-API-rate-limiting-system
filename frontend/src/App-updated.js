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

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

// API Base URL - connects to real Render backend
const API_BASE = process.env.REACT_APP_API_BASE || 'https://gateway-a-east.onrender.com';

function App() {
  const [nodeStatus, setNodeStatus] = useState(null);
  const [metrics, setMetrics] = useState(null);
  const [logs, setLogs] = useState([]);
  const [currentJWT, setCurrentJWT] = useState(null);
  const [algorithms, setAlgorithms] = useState({ current: '', available: [] });
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
      const response = await axios.get(`${API_BASE}/api/status`, {
        timeout: 15000, // 15 second timeout for Render cold starts
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      });
      setNodeStatus(response.data);
      setIsLoading(false);
    } catch (error) {
      addLog(`Failed to fetch node status: ${error.message}`, 'error');
      console.error('Node status fetch error:', error);
      setIsLoading(false);
    }
  };

  const fetchMetrics = async () => {
    try {
      const response = await axios.get(`${API_BASE}/api/metrics`, {
        timeout: 15000,
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      });
      setMetrics(response.data);
    } catch (error) {
      addLog(`Failed to fetch metrics: ${error.message}`, 'error');
      console.error('Metrics fetch error:', error);
    }
  };

  const fetchAlgorithms = async () => {
    try {
      const response = await axios.get(`${API_BASE}/api/algorithm`, {
        timeout: 15000,
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      });
      setAlgorithms(response.data);
    } catch (error) {
      addLog(`Failed to fetch algorithms: ${error.message}`, 'error');
      console.error('Algorithms fetch error:', error);
    }
  };

  const switchAlgorithm = async (algorithm) => {
    try {
      await axios.post(`${API_BASE}/api/algorithm`, {
        algorithm: algorithm
      }, {
        timeout: 15000,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      });
      addLog(`✅ Algorithm switched to ${algorithm}`, 'success');
      fetchAlgorithms(); // Refresh algorithm status
    } catch (error) {
      addLog(`❌ Failed to switch algorithm: ${error.message}`, 'error');
      console.error('Algorithm switch error:', error);
    }
  };

  const generateJWT = () => {
    // Generate a simple JWT for demo purposes
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({
      sub: 'demo-user',
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 3600,
      role: 'user'
    }));
    const signature = btoa('demo-signature');
    const jwt = `${header}.${payload}.${signature}`;
    
    setCurrentJWT(jwt);
    addLog('🔑 JWT token generated', 'success');
  };

  const testWithJWT = async () => {
    if (!currentJWT) {
      addLog('❌ Please generate JWT token first', 'error');
      return;
    }

    try {
      const response = await axios.post(`${API_BASE}/api/data`, {}, {
        timeout: 15000,
        headers: {
          'Authorization': `Bearer ${currentJWT}`,
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      });
      
      if (response.status === 200) {
        addLog(`✅ Request ALLOWED - ${JSON.stringify(response.data)}`, 'success');
      }
    } catch (error) {
      if (error.response?.status === 429) {
        addLog('🚫 Request BLOCKED - Rate limit exceeded', 'error');
      } else {
        addLog(`❌ Request failed: ${error.message}`, 'error');
      }
    }
  };

  const testWithoutJWT = async () => {
    try {
      const response = await axios.post(`${API_BASE}/api/data`, {}, {
        timeout: 15000,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      });
      
      if (response.status === 200) {
        addLog(`✅ Request ALLOWED - ${JSON.stringify(response.data)}`, 'success');
      }
    } catch (error) {
      if (error.response?.status === 401) {
        addLog('🔒 Request REJECTED - Authentication required', 'error');
      } else if (error.response?.status === 429) {
        addLog('🚫 Request BLOCKED - Rate limit exceeded', 'error');
      } else {
        addLog(`❌ Request failed: ${error.message}`, 'error');
      }
    }
  };

  const stressTest = async () => {
    if (!currentJWT) {
      addLog('❌ Please generate JWT token first', 'error');
      return;
    }

    addLog('🔥 Starting stress test (10 rapid requests)...', 'info');
    
    const promises = [];
    for (let i = 0; i < 10; i++) {
      promises.push(
        axios.post(`${API_BASE}/api/data`, {}, {
          timeout: 15000,
          headers: {
            'Authorization': `Bearer ${currentJWT}`,
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }
        }).catch(e => ({ error: true, status: e.response?.status, message: e.message }))
      );
    }

    const results = await Promise.all(promises);
    const successful = results.filter(r => !r.error).length;
    const blocked = results.filter(r => r.error && r.status === 429).length;
    
    addLog(`🔥 Stress test complete - Success: ${successful}, Blocked: ${blocked}`, 'info');
  };

  const chartData = {
    labels: metrics?.history?.map((_, index) => `${index + 1}`) || [],
    datasets: [
      {
        label: 'Total Requests',
        data: metrics?.history?.map(h => h.totalRequests) || [],
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.4,
      },
      {
        label: 'Allowed Requests',
        data: metrics?.history?.map(h => h.allowedRequests) || [],
        borderColor: 'rgb(54, 162, 235)',
        backgroundColor: 'rgba(54, 162, 235, 0.2)',
        tension: 0.4,
      },
      {
        label: 'Blocked Requests',
        data: metrics?.history?.map(h => h.blockedRequests) || [],
        borderColor: 'rgb(255, 99, 132)',
        backgroundColor: 'rgba(255, 99, 132, 0.2)',
        tension: 0.4,
      }
    ]
  };

  if (isLoading) {
    return (
      <div className="container">
        <div className="header">
          <h1>🛡️ Distributed Rate Limiter</h1>
          <p>Connecting to Render backend...</p>
        </div>
        <div className="loading">
          <div className="spinner"></div>
          <p>Loading real-time data from {API_BASE}...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container">
      <div className="header">
        <h1>🛡️ Distributed Rate Limiter - Single Node</h1>
        <p>Real-time monitoring of distributed rate limiting system</p>
        <div className="mode-switcher">
          <a href="?mode=multi" className="btn info">
            🌐 Switch to Multi-Node View
          </a>
        </div>
      </div>

      <div className="dashboard">
        <div className="status-section">
          <div className="status-card">
            <h3>📊 Node Status</h3>
            {nodeStatus ? (
              <div className="status-grid">
                <div className="status-item">
                  <span className="label">Status:</span>
                  <span className={`value ${nodeStatus.status === 'healthy' ? 'success' : 'error'}`}>
                    {nodeStatus.status?.toUpperCase() || 'UNKNOWN'}
                  </span>
                </div>
                <div className="status-item">
                  <span className="label">Algorithm:</span>
                  <span className="value">{nodeStatus.algorithm || 'N/A'}</span>
                </div>
                <div className="status-item">
                  <span className="label">Uptime:</span>
                  <span className="value">{nodeStatus.uptime || 'N/A'}</span>
                </div>
                <div className="status-item">
                  <span className="label">Backend:</span>
                  <span className="value success">Render.com (Live)</span>
                </div>
              </div>
            ) : (
              <div className="error">Unable to connect to backend</div>
            )}
          </div>

          <div className="status-card">
            <h3>🔧 Algorithm Control</h3>
            <div className="algorithm-section">
              <p>Current: <strong>{algorithms.current || 'Loading...'}</strong></p>
              <div className="algorithm-buttons">
                <button 
                  className="algorithm-btn" 
                  onClick={() => switchAlgorithm('tokenbucket')}
                  disabled={algorithms.current === 'tokenbucket'}
                >
                  🪣 Token Bucket
                </button>
                <button 
                  className="algorithm-btn" 
                  onClick={() => switchAlgorithm('leakybucket')}
                  disabled={algorithms.current === 'leakybucket'}
                >
                  💧 Leaky Bucket
                </button>
                <button 
                  className="algorithm-btn" 
                  onClick={() => switchAlgorithm('slidingwindow')}
                  disabled={algorithms.current === 'slidingwindow'}
                >
                  🪟 Sliding Window
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="metrics-section">
          <div className="status-card">
            <h3>📈 Real-Time Metrics</h3>
            {metrics ? (
              <div className="metrics-grid">
                <div className="metric">
                  <span className="metric-label">Total Requests:</span>
                  <span className="metric-value">{metrics.totalRequests || 0}</span>
                </div>
                <div className="metric success">
                  <span className="metric-label">Allowed:</span>
                  <span className="metric-value">{metrics.allowedRequests || 0}</span>
                </div>
                <div className="metric error">
                  <span className="metric-label">Blocked:</span>
                  <span className="metric-value">{metrics.blockedRequests || 0}</span>
                </div>
                <div className="metric">
                  <span className="metric-label">Success Rate:</span>
                  <span className="metric-value">
                    {metrics.totalRequests > 0 
                      ? Math.round((metrics.allowedRequests / metrics.totalRequests) * 100) 
                      : 0}%
                  </span>
                </div>
              </div>
            ) : (
              <div className="loading">Loading metrics...</div>
            )}
          </div>

          {metrics?.udpMetrics && (
            <div className="status-card">
              <h3>📡 UDP Communication</h3>
              <div className="metrics-grid">
                <div className="metric">
                  <span className="metric-label">Packets Sent:</span>
                  <span className="metric-value">{metrics.udpMetrics.packetsSent || 0}</span>
                </div>
                <div className="metric">
                  <span className="metric-label">Packets Received:</span>
                  <span className="metric-value">{metrics.udpMetrics.packetsReceived || 0}</span>
                </div>
                <div className="metric success">
                  <span className="metric-label">ACKs Received:</span>
                  <span className="metric-value">{metrics.udpMetrics.acksReceived || 0}</span>
                </div>
                <div className="metric warning">
                  <span className="metric-label">Retransmissions:</span>
                  <span className="metric-value">{metrics.udpMetrics.retransmissions || 0}</span>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="chart-section">
          <div className="status-card">
            <h3>📊 Request History</h3>
            {metrics?.history && metrics.history.length > 0 ? (
              <Line data={chartData} options={{
                responsive: true,
                plugins: {
                  legend: {
                    position: 'top',
                  },
                  title: {
                    display: true,
                    text: 'Request Rate Over Time'
                  }
                },
                scales: {
                  y: {
                    beginAtZero: true
                  }
                }
              }} />
            ) : (
              <div className="no-data">No historical data available yet</div>
            )}
          </div>
        </div>

        <div className="controls-section">
          <h3>🧪 API Testing</h3>
          <div className="button-group">
            <button className="btn" onClick={generateJWT}>
              🔑 Generate JWT
            </button>
            <button className="btn" onClick={testWithJWT} disabled={!currentJWT}>
              ✅ Test with JWT
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
              <code>{currentJWT}</code>
            </div>
          )}
        </div>

        <div className="logs-section">
          <h3>📝 Activity Logs</h3>
          <div className="logs">
            {logs.map(log => (
              <div key={log.id} className={`log-entry ${log.type}`}>
                <span className="timestamp">{log.timestamp}</span>
                <span className="message">{log.message}</span>
              </div>
            ))}
            {logs.length === 0 && (
              <div className="no-logs">No activity yet. Try testing the API!</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
