import React from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import ErrorBoundary from './ErrorBoundary'
import './styles.css'

// Global error reporter (shows overlay on uncaught errors)
function showErrorOverlay(msg) {
  let el = document.getElementById('error-overlay');
  if (!el) {
    el = document.createElement('div');
    el.id = 'error-overlay';
    el.style.position = 'fixed';
    el.style.left = '10px';
    el.style.right = '10px';
    el.style.top = '80px';
    el.style.padding = '12px';
    el.style.background = 'rgba(255,230,230,0.95)';
    el.style.border = '1px solid #ff8888';
    el.style.zIndex = 9999;
    el.style.whiteSpace = 'pre-wrap';
    document.body.appendChild(el);
  }
  el.textContent = 'ERROR: ' + msg;
}

window.addEventListener('error', (e) => { showErrorOverlay(e.message + '\n' + (e.filename || '')); console.error(e) })
window.addEventListener('unhandledrejection', (e) => { showErrorOverlay(String(e.reason)); console.error(e) })

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </React.StrictMode>
)
