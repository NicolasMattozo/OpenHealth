import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// Em desenvolvimento o Vite repassa /api para o backend Spring (porta 8080),
// entao o navegador enxerga uma unica origem e nao precisa de CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.VITE_BACKEND_URL ?? 'http://localhost:8080', changeOrigin: true },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
})
