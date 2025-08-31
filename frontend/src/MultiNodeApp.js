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
// Removed demo data imports - using real Render backends only

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

const API_BASE = process.env.REACT_APP_API_BASE || 'demo-mode';

// Real Render.com backend URLs - no demo mode
const NODES = [
  { 
    id: 'node1', 
    name: 'Gateway A (East Coast)', 
    url: process.env.REACT_APP_NODE1_URL || 'https://gateway-a-east.onrender.com',
    color: '#00ff88',
    algorithm: 'tokenbucket'
  },
  { 
    id: 'node2', 
    name: 'Gateway B (West Coast)', 
    url: process.env.REACT_APP_NODE2_URL || 'https://gateway-b-west.onrender.com',
    color: '#74b9ff',
    algorithm: 'leakybucket'
  },
  { 
    id: 'node3', 
    name: 'Gateway C (International)', 
    url: process.env.REACT_APP_NODE3_URL || 'https://gateway-c-intl.onrender.com',
    color: '#ff6b6b',
    algorithm: 'slidingwindow'
  }
];

function MultiNodeApp() {
  const [nodes, setNodes] = useState({});
  const [logs, setLogs] = useState([]);
  const [currentJWT, setCurrentJWT] = useState(null);
  const [selectedNode, setSelectedNode] = useState(1);
  const [clickedButtons, setClickedButtons] = useState({});
  const [udpChartData, setUdpChartData] = useState({
    labels: [],
    datasets: NODES.map(node => ({
      label: `${node.name} - Packets Sent`,
      data: [],
      borderColor: node.color,
      backgroundColor: `${node.color}20`,
      tension: 0.4,
    }))
  });

  useEffect(() => {
    fetchAllNodes();
    const interval = setInterval(fetchAllNodes, 2000);
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

    const fetchAllNodes = async () => {
    const nodeData = {};
    
    for (const node of NODES) {
      try {
        // Always use real API calls to Render backends
        const [statusRes, metricsRes, algorithmRes] = await Promise.all([
          axios.get(`${node.url}/api/status`, {
            timeout: 15000, // 15 second timeout for Render cold starts
            headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/json'
            }
          }),
          axios.get(`${node.url}/api/metrics`, {
            timeout: 15000,
            headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/json'
            }
          }),
          axios.get(`${node.url}/api/algorithm`, {
            timeout: 15000,
            headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/json'
            }
          })
        ]);
        
        nodeData[node.id] = {
          ...node,
          status: statusRes.data,
          metrics: metricsRes.data,
          algorithm: algorithmRes.data,
          online: true
        };
      } catch (error) {
        nodeData[node.id] = {
          ...node,
          online: false,
          error: error.message
        };
      }
    }
    
    setNodes(nodeData);
    updateUdpChart(nodeData);
  };

  const updateUdpChart = (nodeData) => {
    const now = new Date().toLocaleTimeString();
    
    setUdpChartData(prev => ({
      ...prev,
      labels: [...prev.labels.slice(-9), now],
      datasets: prev.datasets.map((dataset, index) => {
        const nodeId = index + 1;
        const node = nodeData[nodeId];
        const packetsSent = node?.metrics?.udp?.packetsSent || 0;
        
        return {
          ...dataset,
          data: [...dataset.data.slice(-9), packetsSent]
        };
      })
    }));
  };

  const generateJWT = () => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({
      sub: `api-key-${Math.random().toString(36).substr(2, 9)}`,
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 3600,
      purpose: 'multi-node-demo'
    }));
    const signature = `demo-signature-${Math.random().toString(36).substr(2, 16)}`;
    const jwt = `${header}.${payload}.${signature}`;
    
    setCurrentJWT(jwt);
    addLog('🔑 JWT token generated for multi-node testing', 'success');
  };

  const testNode = async (nodeId) => {
    if (!currentJWT) {
      addLog('❌ Please generate JWT token first', 'error');
      return;
    }

    const node = nodes[nodeId];
    if (!node?.online) {
      addLog(`❌ Node ${nodeId} (${node?.name}) is offline or unreachable`, 'error');
      return;
    }

    // Add visual feedback - highlight the button
    setClickedButtons(prev => ({ ...prev, [nodeId]: true }));
    setTimeout(() => {
      setClickedButtons(prev => ({ ...prev, [nodeId]: false }));
    }, 1000);

    try {
      addLog(`🧪 Testing ${node.name} (${node.url})...`, 'info');
      
      // Always use real API call to Render backend
      const response = await axios.post(`${node.url}/api/data`, {}, {
        timeout: 15000, // 15 second timeout for Render cold starts
        headers: {
          'Authorization': `Bearer ${currentJWT}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.status === 200) {
        addLog(`✅ ${node.name} - Request ALLOWED - ${JSON.stringify(response.data)}`, 'success');
      }
    } catch (error) {
      if (error.response?.status === 429) {
        addLog(`🚫 ${node.name} - Request BLOCKED - Rate limit exceeded`, 'error');
      } else {
        addLog(`❌ ${node.name} - Request failed: ${error.message}`, 'error');
      }
    }
  };

  const switchAlgorithmAllNodes = async (algorithm) => {
    addLog(`🔄 Switching all nodes to ${algorithm} algorithm...`, 'info');
    
    const promises = NODES.map(async (node) => {
      try {
        // Always use real API call to Render backend
        await axios.post(`${node.url}/api/algorithm`, {
          algorithm: algorithm
        }, {
          timeout: 15000, // 15 second timeout for Render cold starts
          headers: { 
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }
        });
        return { nodeId: node.id, success: true };
      } catch (error) {
        console.error(`Failed to switch algorithm on ${node.name}:`, error.message);
        return { nodeId: node.id, success: false, error: error.message };
      }
    });

    const results = await Promise.all(promises);
    const successCount = results.filter(r => r.success).length;
    
    if (successCount === NODES.length) {
      addLog(`✅ All ${NODES.length} nodes switched to ${algorithm}`, 'success');
    } else {
      addLog(`⚠️ ${successCount}/${NODES.length} nodes switched to ${algorithm}`, 'error');
    }
    
    fetchAllNodes(); // Refresh node status
  };

  const stressTestAllNodes = async () => {
    if (!currentJWT) {
      addLog('❌ Please generate JWT token first', 'error');
      return;
    }

    addLog('🔥 Starting distributed stress test (5 requests per node)...', 'info');
    
    const promises = [];
    for (let i = 0; i < 5; i++) {
      for (const node of NODES) {
        if (nodes[node.id]?.online) {
          // Always use real API call to Render backend
          promises.push(
            axios.post(`${node.url}/api/data`, {}, {
              timeout: 15000, // 15 second timeout for Render cold starts
              headers: { 
                'Authorization': `Bearer ${currentJWT}`,
                'Content-Type': 'application/json',
                'Accept': 'application/json'
              }
            }).catch(e => ({ 
              error: true, 
              node: node.name, 
              status: e.response?.status,
              message: e.message 
            }))
          );
        }
      }
      await new Promise(resolve => setTimeout(resolve, 500)); // Slightly longer delay for real API calls
    }

    const results = await Promise.all(promises);
    const successful = results.filter(r => !r.error).length;
    const blocked = results.filter(r => r.error && r.status === 429).length;
    
    addLog(`🔥 Distributed stress test complete - Success: ${successful}, Blocked: ${blocked}`, 'info');
  };

  return (
    <div className="container">
      <div className="header">
        <h1>🌐 Distributed Rate Limiter - Multi-Node Demo</h1>
        <p><strong>Real-time UDP Coordination Across Multiple Gateway Nodes</strong></p>
        <p>Watch how rate limiting state synchronizes across distributed nodes via UDP</p>
      </div>

      <div className="nodes-grid">
        {NODES.map(node => {
          const nodeData = nodes[node.id];
          return (
            <div key={node.id} className={`node-card ${nodeData?.online ? 'online' : 'offline'}`}>
              <h3 style={{color: node.color}}>
                {nodeData?.online ? '🟢' : '🔴'} {node.name}
              </h3>
              <div className="node-info">
                <div className="metric">Port: <strong>{node.url.split(':')[2]}</strong></div>
                <div className="metric">
                  Algorithm: <strong>{nodeData?.algorithm?.current || 'N/A'}</strong>
                </div>
                {nodeData?.online && nodeData.metrics?.udp && (
                  <>
                    <div className="metric">Packets Sent: <strong>{nodeData.metrics.udp.packetsSent}</strong></div>
                    <div className="metric">Packets Received: <strong>{nodeData.metrics.udp.packetsReceived}</strong></div>
                    <div className="metric">ACKs Received: <strong>{nodeData.metrics.udp.acksReceived}</strong></div>
                    <div className="metric">Retransmissions: <strong>{nodeData.metrics.udp.retransmissions}</strong></div>
                    <div className="metric">Deltas Applied: <strong>{nodeData.metrics.udp.deltasApplied}</strong></div>
                    <div className="metric">Pending Packets: <strong>{nodeData.metrics.udp.pendingPackets}</strong></div>
                    <div className="metric">Peer Count: <strong>{nodeData.metrics.udp.peerCount}</strong></div>
                  </>
                )}
                {!nodeData?.online && (
                  <div className="metric error">Status: <strong>OFFLINE</strong></div>
                )}
              </div>
              <button 
                className="btn node-test-btn" 
                onClick={() => testNode(node.id)}
                disabled={!nodeData?.online || !currentJWT}
                style={{backgroundColor: node.color}}
              >
                🧪 Test {node.name}
              </button>
            </div>
          );
        })}
      </div>

      <div className="chart-container">
        <h3>📊 Real-time UDP Coordination</h3>
        <Line 
          data={udpChartData}
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

      <div className="udp-summary">
        <h3>🌐 UDP Coordination Summary</h3>
        <div className="udp-metrics-grid">
          {NODES.map(node => {
            const nodeData = nodes[node.id];
            const udp = nodeData?.metrics?.udp;
            if (!nodeData?.online || !udp) return null;
            
            return (
              <div key={node.id} className="udp-summary-card" style={{borderColor: node.color}}>
                <h4 style={{color: node.color}}>{node.name}</h4>
                <div className="udp-stats">
                  <div className="udp-stat">
                    <span className="label">Packets Sent:</span>
                    <span className="value">{udp.packetsSent?.toLocaleString()}</span>
                  </div>
                  <div className="udp-stat">
                    <span className="label">Packets Received:</span>
                    <span className="value">{udp.packetsReceived?.toLocaleString()}</span>
                  </div>
                  <div className="udp-stat">
                    <span className="label">ACKs Received:</span>
                    <span className="value">{udp.acksReceived?.toLocaleString()}</span>
                  </div>
                  <div className="udp-stat">
                    <span className="label">Retransmissions:</span>
                    <span className="value">{udp.retransmissions?.toLocaleString()}</span>
                  </div>
                  <div className="udp-stat">
                    <span className="label">Deltas Applied:</span>
                    <span className="value">{udp.deltasApplied?.toLocaleString()}</span>
                  </div>
                  <div className="udp-stat">
                    <span className="label">Pending Packets:</span>
                    <span className="value">{udp.pendingPackets}</span>
                  </div>
                  <div className="udp-stat">
                    <span className="label">Peer Count:</span>
                    <span className="value">{udp.peerCount}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="control-section">
        <h3>⚙️ Intelligent Algorithm Selection</h3>
        <p>Choose the optimal algorithm based on traffic patterns and requirements</p>
        
        <div className="algorithm-info-grid">
          <div className="algorithm-info-card tokenbucket">
            <h4>🪣 Token Bucket</h4>
            <div className="algorithm-details">
              <p><strong>Best for:</strong> Bursty traffic patterns</p>
              <p><strong>Use case:</strong> API gateways, user-facing services</p>
              <p><strong>Behavior:</strong> Allows traffic bursts up to capacity</p>
              <p><strong>Good when:</strong> Traffic has natural peaks (rush hours)</p>
            </div>
            <button 
              className="algorithm-btn-large tokenbucket" 
              onClick={() => switchAlgorithmAllNodes('tokenbucket')}
            >
              🪣 Apply Token Bucket
            </button>
          </div>
          
          <div className="algorithm-info-card leakybucket">
            <h4>💧 Leaky Bucket</h4>
            <div className="algorithm-details">
              <p><strong>Best for:</strong> Smooth, consistent rate control</p>
              <p><strong>Use case:</strong> Backend services, database connections</p>
              <p><strong>Behavior:</strong> Enforces steady rate regardless of input</p>
              <p><strong>Good when:</strong> Need predictable, constant load</p>
            </div>
            <button 
              className="algorithm-btn-large leakybucket" 
              onClick={() => switchAlgorithmAllNodes('leakybucket')}
            >
              💧 Apply Leaky Bucket
            </button>
          </div>
          
          <div className="algorithm-info-card slidingwindow">
            <h4>🪟 Sliding Window</h4>
            <div className="algorithm-details">
              <p><strong>Best for:</strong> Time-based fairness and quotas</p>
              <p><strong>Use case:</strong> Per-user limits, billing systems</p>
              <p><strong>Behavior:</strong> Precise request counting over time windows</p>
              <p><strong>Good when:</strong> Need exact quota enforcement</p>
            </div>
            <button 
              className="algorithm-btn-large slidingwindow" 
              onClick={() => switchAlgorithmAllNodes('slidingwindow')}
            >
              🪟 Apply Sliding Window
            </button>
          </div>
        </div>

        <div className="load-analysis">
          <h4>📊 Current Load Analysis & Recommendations</h4>
          <div className="load-metrics">
            {Object.values(nodes).map(node => {
              if (!node?.online || !node.metrics?.udp) return null;
              
              const udp = node.metrics.udp;
              const retransmissionRate = udp.packetsSent > 0 ? (udp.retransmissions / udp.packetsSent * 100).toFixed(1) : 0;
              const reliability = udp.packetsReceived > 0 ? ((udp.deltasApplied / udp.packetsReceived) * 100).toFixed(1) : 0;
              
              let recommendation = "🪣 Token Bucket"; // Default
              let reason = "Balanced approach for mixed traffic";
              
              if (retransmissionRate > 50) {
                recommendation = "💧 Leaky Bucket";
                reason = "High retransmission rate - need steady flow control";
              } else if (udp.pendingPackets > 10) {
                recommendation = "🪟 Sliding Window";
                reason = "High pending packets - need precise time-based control";
              } else if (udp.packetsSent > 30000) {
                recommendation = "🪣 Token Bucket";
                reason = "High traffic volume - allow bursts for efficiency";
              }
              
              return (
                <div key={node.id} className="load-analysis-card" style={{borderColor: node.color}}>
                  <h5 style={{color: node.color}}>{node.name} Analysis</h5>
                  <div className="load-stats">
                    <div className="load-stat">
                      <span>Traffic Volume:</span>
                      <span className={udp.packetsSent > 30000 ? 'high' : udp.packetsSent > 10000 ? 'medium' : 'low'}>
                        {udp.packetsSent?.toLocaleString()} packets
                      </span>
                    </div>
                    <div className="load-stat">
                      <span>Retransmission Rate:</span>
                      <span className={retransmissionRate > 50 ? 'high' : retransmissionRate > 25 ? 'medium' : 'low'}>
                        {retransmissionRate}%
                      </span>
                    </div>
                    <div className="load-stat">
                      <span>Reliability:</span>
                      <span className={reliability > 90 ? 'high' : reliability > 70 ? 'medium' : 'low'}>
                        {reliability}%
                      </span>
                    </div>
                    <div className="load-stat">
                      <span>Pending Packets:</span>
                      <span className={udp.pendingPackets > 10 ? 'high' : udp.pendingPackets > 5 ? 'medium' : 'low'}>
                        {udp.pendingPackets}
                      </span>
                    </div>
                  </div>
                  <div className="recommendation">
                    <strong>💡 Recommended:</strong> {recommendation}
                    <br />
                    <em>{reason}</em>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      <div className="test-section">
        <h3>🧪 Distributed Load Testing & UDP Coordination</h3>
        <p className="test-explanation">
          <strong>Why test individual gateways?</strong> In production, different gateways serve different regions/users. 
          Testing individual nodes shows how UDP coordinates rate limits across the distributed system.
        </p>
        
        <div className="test-scenarios">
          <div className="test-scenario">
            <h4>🌍 Regional Load Distribution</h4>
            <p>Simulate different regions hitting different gateways - watch UDP coordination!</p>
            
            {!currentJWT && (
              <div className="jwt-required-notice">
                <strong>⚠️ JWT Token Required:</strong> Generate a JWT token first to enable individual gateway testing
              </div>
            )}
            
            <div className="button-group">
              <button 
                onClick={() => testNode('node1')} 
                className={`test-btn-regional ${clickedButtons.node1 ? 'clicked' : ''}`}
                style={{borderColor: '#00ff88'}} 
                disabled={!currentJWT}
              >
                🌎 Test East Coast (Gateway A)
                {!currentJWT && <span className="disabled-hint"> (JWT Required)</span>}
              </button>
              <button 
                onClick={() => testNode('node2')} 
                className={`test-btn-regional ${clickedButtons.node2 ? 'clicked' : ''}`}
                style={{borderColor: '#74b9ff'}} 
                disabled={!currentJWT}
              >
                🌏 Test West Coast (Gateway B)
                {!currentJWT && <span className="disabled-hint"> (JWT Required)</span>}
              </button>
              <button 
                onClick={() => testNode('node3')} 
                className={`test-btn-regional ${clickedButtons.node3 ? 'clicked' : ''}`}
                style={{borderColor: '#ff6b6b'}} 
                disabled={!currentJWT}
              >
                🌍 Test International (Gateway C)
                {!currentJWT && <span className="disabled-hint"> (JWT Required)</span>}
              </button>
            </div>
            <p className="scenario-note">
              <strong>Watch UDP metrics:</strong> Each test triggers UDP packets to synchronize rate limits across all gateways
            </p>
          </div>

          <div className="test-scenario">
            <h4>🚀 Full System Stress Test</h4>
            <p>Test all gateways simultaneously to see distributed coordination under load</p>
            <div className="button-group">
              <button className="btn" onClick={generateJWT}>
                🔑 Generate JWT
              </button>
              <button className="btn info" onClick={fetchAllNodes}>
                📊 Refresh All Nodes
              </button>
              <button className="btn" onClick={stressTestAllNodes} disabled={!currentJWT}>
                🔥 Distributed Stress Test
              </button>
            </div>
          </div>
        </div>

        <div className="chart-section">
          <div className="status-card">
            <h3>📊 Real-Time Node Comparison</h3>
            {Object.keys(nodes).length > 0 ? (
              <Line 
                data={{
                  labels: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10'],
                  datasets: NODES.map(node => {
                    const nodeData = nodes[node.id];
                    return {
                      label: `${node.name} (${node.algorithm})`,
                      data: nodeData?.metrics?.history?.map(h => h.totalRequests) || Array(10).fill(0),
                      borderColor: node.color,
                      backgroundColor: `${node.color}20`,
                      tension: 0.4,
                    };
                  })
                }}
                options={{
                  responsive: true,
                  plugins: {
                    legend: {
                      position: 'top',
                    },
                    title: {
                      display: true,
                      text: 'Request Rate Comparison Across Nodes'
                    }
                  },
                  scales: {
                    y: {
                      beginAtZero: true,
                      title: {
                        display: true,
                        text: 'Requests per Second'
                      }
                    },
                    x: {
                      title: {
                        display: true,
                        text: 'Time Points'
                      }
                    }
                  }
                }}
              />
            ) : (
              <div className="no-data">No node data available for comparison</div>
            )}
          </div>
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
              <strong>[System]</strong> Multi-node dashboard ready - Generate JWT and start testing distributed coordination!
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

export default MultiNodeApp;
