import { Navigate, Outlet } from 'react-router'
import { useAuthStore } from '../stores/useAuthStore'

// 비로그인 상태면 /login으로 보내는 가드
// `user && <Outlet />` 형태는 falsy 값(null, 0, "")이 그대로 렌더링될 수 있어 삼항으로 명시 분기
export default function RequireAuth() {
  const user = useAuthStore((s) => s.user)
  return user ? <Outlet /> : <Navigate to="/login" replace />
}
