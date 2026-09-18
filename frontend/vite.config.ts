import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        manualChunks: {
          // React core — changes rarely
          'vendor-react': ['react', 'react-dom', 'react-router-dom'],
          // TanStack Query — state management
          'vendor-query': ['@tanstack/react-query'],
          // Charting library — large, isolated
          'vendor-recharts': ['recharts'],
          // Form utilities
          'vendor-forms': ['react-hook-form', '@hookform/resolvers', 'zod'],
          // Icon set
          'vendor-icons': ['lucide-react'],
          // Utility libs
          'vendor-utils': ['clsx', 'tailwind-merge'],
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
