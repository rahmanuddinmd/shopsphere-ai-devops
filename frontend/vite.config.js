import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev server on :5173 proxies /api and /actuator to the Spring Boot backend on :8080,
// so the browser always calls same-origin /api (exactly like Nginx/Ingress in containers).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
  },
});
