import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: "127.0.0.1",
    port: 5173,
    proxy: {
      "/api": {
        target: "https://good-musical-joey.ngrok-free.app",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
        logLevel: "debug",
      },
      "/users": {
        target: "https://good-musical-joey.ngrok-free.app",
        changeOrigin: true,
        logLevel: "debug",
      },
      "/accounts": {
        target: "https://good-musical-joey.ngrok-free.app",
        changeOrigin: true,
        logLevel: "debug",
        configure: (proxy) => {
          // Prevent from ERR_NGROK_6024 - NGROK INTERSTITIAL PAGE
          proxy.on("proxyReq", (proxyReq) => {
            // Set the header Ngrok uses to skip the interstitial page
            proxyReq.setHeader("ngrok-skip-browser-warning", "true"); // Value can be anything, 'true' is clear
            console.log(
              "[vite:proxy:configure] Added ngrok-skip-browser-warning header"
            );
          });
        },
      },
    },
  },
});
