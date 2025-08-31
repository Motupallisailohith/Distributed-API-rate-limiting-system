// Configuration for different environments
export const config = {
  // Determine if we're in production (connected to real backends) or demo mode
  isProduction: process.env.REACT_APP_MODE === 'production',
  
  // API Base URL
  apiBase: process.env.REACT_APP_API_BASE || 'demo-mode',
  
  // Node URLs for multi-node setup
  nodes: {
    node1: {
      id: 'node1',
      name: 'Gateway A (East Coast)',
      url: process.env.REACT_APP_NODE1_URL || 'demo-mode',
      color: '#00ff88',
      algorithm: 'tokenbucket'
    },
    node2: {
      id: 'node2', 
      name: 'Gateway B (West Coast)',
      url: process.env.REACT_APP_NODE2_URL || 'demo-mode',
      color: '#74b9ff',
      algorithm: 'leakybucket'
    },
    node3: {
      id: 'node3',
      name: 'Gateway C (International)',
      url: process.env.REACT_APP_NODE3_URL || 'demo-mode',
      color: '#ff6b6b',
      algorithm: 'slidingwindow'
    }
  },
  
  // Convert nodes object to array for easier iteration
  get nodeArray() {
    return Object.values(this.nodes);
  },
  
  // Check if any node is in demo mode
  get isDemoMode() {
    return this.nodeArray.some(node => node.url === 'demo-mode');
  }
};

export default config;
