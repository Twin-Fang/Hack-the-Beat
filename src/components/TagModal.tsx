import { useState } from 'react'

interface TagModalProps {
  isOpen: boolean
  onClose: () => void
  onTag: (code: string) => Promise<void>
  isLoading: boolean
}

export default function TagModal({
  isOpen,
  onClose,
  onTag,
  isLoading,
}: TagModalProps) {
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!code.trim()) {
      setError('4자리 코드를 입력해주세요.')
      return
    }
    try {
      setError(null)
      await onTag(code.trim().toUpperCase())
      setCode('')
      onClose()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '태그에 실패했습니다.')
    }
  }

  return (
    <div className="modal modal-open">
      <div className="modal-box max-w-sm">
        <h3 className="font-bold text-lg mb-2">코드로 태그</h3>
        <p className="text-sm text-base-content/70 mb-4">
          대화한 상대방의 4자리 코드를 입력하면 즉시 만난 사람으로 등록됩니다.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label" htmlFor="targetTagCode">
              <span className="label-text font-medium">상대방 4자리 코드</span>
            </label>
            <input
              id="targetTagCode"
              name="targetTagCode"
              type="text"
              maxLength={4}
              aria-label="상대방 4자리 코드"
              placeholder="예: 7K2M"
              className="input input-bordered w-full text-center text-2xl font-mono tracking-widest uppercase"
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
              autoFocus
            />
            {error ? <p className="text-error text-xs mt-1.5">{error}</p> : null}
          </div>

          <div className="modal-action flex gap-2">
            <button
              type="button"
              className="btn btn-ghost flex-1"
              onClick={onClose}
              disabled={isLoading}
            >
              취소
            </button>
            <button
              type="submit"
              className="btn btn-primary flex-1"
              disabled={isLoading || code.length !== 4}
            >
              {isLoading ? (
                <span className="loading loading-spinner loading-sm" />
              ) : (
                '태그하기'
              )}
            </button>
          </div>
        </form>
      </div>
      <button
        type="button"
        className="modal-backdrop"
        aria-label="코드로 태그 창 닫기"
        onClick={onClose}
      />
    </div>
  )
}
