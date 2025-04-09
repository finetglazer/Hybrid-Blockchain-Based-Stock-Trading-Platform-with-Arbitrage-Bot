import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      "/api": {
        target: "https://good-musical-joey.ngrok-free.app",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
      '/users': {
        target: 'https://good-musical-joey.ngrok-free.app',
        changeOrigin: true,
        secure: false
      },
      '/accounts': {
        target: 'https://good-musical-joey.ngrok-free.app',
        changeOrigin: true,
        secure: false
      }
    },
  },
});
