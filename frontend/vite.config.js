import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: true, // bind to 0.0.0.0 and :: to make the dev server reachable from localhost and 127.0.0.1
    port: 5173
  }
})