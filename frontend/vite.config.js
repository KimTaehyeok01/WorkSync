import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  // sockjs-client가 참조하는 Node 전역 객체 global을 브라우저 globalThis로 치환
  define: {
    global: "globalThis",
  },
  server: {
    port: 3000,
    // api 요청을 백엔드(8080)로 프록시 — CORS 우회 및 개발 환경 연동
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});

