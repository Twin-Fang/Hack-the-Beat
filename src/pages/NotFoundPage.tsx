import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <div className="min-h-screen bg-base-200 flex flex-col items-center justify-center gap-4">
      <h1 className="text-4xl font-bold">404</h1>
      <Link to="/" className="btn btn-ghost">
        홈으로
      </Link>
    </div>
  )
}
