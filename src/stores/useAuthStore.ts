import { create } from 'zustand'

type User = { name: string }

type AuthState = {
  user: User | null
  login: (name: string) => void
  logout: () => void
}

// 로그인 상태 전역 스토어 — 라우트 가드(RequireAuth)에서 참조
export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  login: (name) => set({ user: { name } }),
  logout: () => set({ user: null }),
}))
