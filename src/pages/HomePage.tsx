import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '../stores/useAuthStore'

// 예시 쿼리 — 실제 API 연결 시 이 함수만 fetch/axios 호출로 교체
async function fetchGreeting(): Promise<{ message: string }> {
  await new Promise((resolve) => setTimeout(resolve, 500))
  return { message: 'TanStack Query 동작 확인' }
}

export default function HomePage() {
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)
  const greeting = useQuery({ queryKey: ['greeting'], queryFn: fetchGreeting })

  // 로딩/에러/성공을 삼항으로 분기 — && 체인은 falsy 값이 새어 나올 수 있어 사용하지 않음
  const greetingContent = greeting.isPending ? (
    <span className="loading loading-spinner loading-sm" />
  ) : greeting.isError ? (
    <p className="text-error">불러오기 실패</p>
  ) : (
    <p>{greeting.data.message}</p>
  )

  return (
    <div className="min-h-screen bg-base-200 flex items-center justify-center p-8">
      <div className="card w-96 bg-base-100 shadow-xl">
        <div className="card-body">
          <h1 className="card-title">Hack the Beat</h1>
          <p>안녕하세요, {user ? user.name : '게스트'}님</p>
          {greetingContent}
          <div className="card-actions justify-end mt-4">
            <button className="btn btn-outline" onClick={logout}>
              로그아웃
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
