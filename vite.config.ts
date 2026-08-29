import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  // GitHub Pages는 /<레포명>/ 하위 경로로 서빙되므로 빌드 시에만 base 지정 (dev는 / 유지)
  base: command === 'build' ? '/Hack-the-Beat/' : '/',
  // Tailwind v4는 PostCSS 설정 없이 Vite 플러그인만으로 동작
  plugins: [react(), tailwindcss()],
}))
