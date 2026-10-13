import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:80',
        changeOrigin: true,
        headers: {
          Origin: 'http://localhost:80'
        },
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes, req) => {
            proxyRes.headers['access-control-allow-origin'] = req.headers.origin || 'http://localhost:5173';
          });
        }
      },
      '/uploads': {
        target: 'http://localhost:80',
        changeOrigin: true,
        headers: {
          Origin: 'http://localhost:80'
        },
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes, req) => {
            proxyRes.headers['access-control-allow-origin'] = req.headers.origin || 'http://localhost:5173';
          });
        }
      }
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})
