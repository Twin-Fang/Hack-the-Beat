import { useNavigate } from 'react-router'
import { useAuthStore } from '../stores/useAuthStore'

export default function LoginPage() {
  const login = useAuthStore((s) => s.login)
  const navigate = useNavigate()

  // 예시 로그인 — 실제 인증 붙이면 여기서 API 호출 후 login() 호출
  const handleLogin = () => {
    login('hacker')
    navigate('/', { replace: true })
  }

  return (
    <div className="min-h-screen bg-base-200 flex items-center justify-center p-8">
      <div className="card w-96 bg-base-100 shadow-xl">
        <div className="card-body">
          <h1 className="card-title">로그인</h1>
          <button className="btn btn-primary mt-4" onClick={handleLogin}>
            로그인
          </button>
        </div>
      </div>
    </div>
  )
}
