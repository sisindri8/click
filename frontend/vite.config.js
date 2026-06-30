import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      // Proxy API calls to the Gateway so CORS isn't an issue in development.
      // Change the target port if your Gateway runs on something other than 9090.
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true,
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  }
})
