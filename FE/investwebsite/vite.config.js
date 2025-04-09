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
        target: "http://localhost:8080",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
      '/users': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/accounts': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    },
  },
});
