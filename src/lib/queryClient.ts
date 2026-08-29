import { QueryClient } from '@tanstack/react-query'

// 앱 전체에서 공유하는 단일 QueryClient
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 60 * 1000, // 1분 동안은 재요청 없이 캐시 사용
    },
  },
})
