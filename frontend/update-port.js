const API_BASE_URL = 'http://localhost:8085';

// Update App.js to use port 8085
const originalContent = require('fs').readFileSync('src/App.js', 'utf8');
const updatedContent = originalContent.replace('localhost:8080', 'localhost:8085');
require('fs').writeFileSync('src/App.js', updatedContent);
