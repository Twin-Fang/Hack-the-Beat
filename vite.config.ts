import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  // Tailwind v4는 PostCSS 설정 없이 Vite 플러그인만으로 동작
  plugins: [react(), tailwindcss()],
})
