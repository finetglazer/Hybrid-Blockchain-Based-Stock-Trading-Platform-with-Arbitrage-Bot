import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const isDocker = process.env.DOCKER_ENV === 'true';
// Define what apiTarget should be if NOT in Docker
const localApiTarget = 'http://localhost:8080'; // Assuming your API gateway runs on 8080 locally when not in Docker
const apiTarget = isDocker ? 'http://api-gateway:8080' : localApiTarget;

export default defineConfig({
  plugins: [react()],
  server: {
    host: "0.0.0.0",  // Correct: Listen on all interfaces
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: apiTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
        logLevel: 'debug',
      },
      '/users': {
        target: apiTarget,
        changeOrigin: true,
        logLevel: 'debug',
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('ngrok-skip-browser-warning', 'true');
            console.log('[vite:proxy:configure] Added ngrok-skip-browser-warning header for /users');
          });
        }
      },
      '/accounts': {
        target: apiTarget,
        changeOrigin: true,
        logLevel: 'debug',
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('ngrok-skip-browser-warning', 'true');
            console.log('[vite:proxy:configure] Added ngrok-skip-browser-warning header for /accounts');
          });
        }
      },
      '/market-data': { // HTTP endpoint
        target: apiTarget,
        changeOrigin: true,
        logLevel: 'debug',
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('ngrok-skip-browser-warning', 'true');
            console.log('[vite:proxy:configure] Added ngrok-skip-browser-warning header for /market-data');
          });
        }
      },
      '/sagas': {
        target: apiTarget,
        changeOrigin: true,
        logLevel: 'debug',
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('ngrok-skip-browser-warning', 'true');
            console.log('[vite:proxy:configure] Added ngrok-skip-browser-warning header for /sagas');
          });
        }
      },
      '/orders': { // HTTP endpoint
        target: apiTarget,
        changeOrigin: true,
        logLevel: 'debug',
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('ngrok-skip-browser-warning', 'true');
            console.log('[vite:proxy:configure] Added ngrok-skip-browser-warning header for /orders');
          });
        }
      },
      '/ws/market-data': { // WebSocket endpoint
        target: apiTarget,      // Use the dynamic apiTarget
        changeOrigin: true,
        ws: true,               // Crucial for WebSocket proxying
        logLevel: 'debug',
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('ngrok-skip-browser-warning', 'true');
            console.log('[vite:proxy:configure] Added ngrok-skip-browser-warning header for /ws/market-data');
          });
        }
      },
      '/ws/orders': { // WebSocket endpoint
        target: apiTarget,      // Use the dynamic apiTarget
        changeOrigin: true,
        ws: true,               // Crucial for WebSocket proxying
        logLevel: 'debug',
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('ngrok-skip-browser-warning', 'true');
            console.log('[vite:proxy:configure] Added ngrok-skip-browser-warning header for /ws/orders');
          });
        }
      },
    },
  },
});