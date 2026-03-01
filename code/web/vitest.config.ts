import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: ['node_modules/', 'coverage/', '.next/', '**/*.config.ts'],
      thresholds: {
        lines: 37,
        branches: 77,
        functions: 64,
        statements: 37,
      },
    },
  },
  resolve: {
    alias: { '@': path.resolve(__dirname, './') },
  },
})
