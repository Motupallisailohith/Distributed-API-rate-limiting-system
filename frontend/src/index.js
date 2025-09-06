import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import MultiNodeApp from './MultiNodeApp';

// Check URL parameter to determine which app to show
const urlParams = new URLSearchParams(window.location.search);
const mode = urlParams.get('mode');

const AppComponent = mode === 'multi' ? MultiNodeApp : App;

ReactDOM.render(
  <React.StrictMode>
    <AppComponent />
  </React.StrictMode>,
  document.getElementById('root')
);
