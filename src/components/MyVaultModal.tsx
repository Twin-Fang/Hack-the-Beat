import { usePassportStore } from '../stores/usePassportStore'
import { characterOf } from '../lib/character'

interface MyVaultModalProps {
  isOpen: boolean
  onClose: () => void
}

export default function MyVaultModal({ isOpen, onClose }: MyVaultModalProps) {
  const savedBadges = usePassportStore((s) => s.savedBadges)

  if (!isOpen) return null

  return (
    <div className="modal modal-open">
      <div className="modal-box max-w-lg">
        <div className="flex justify-between items-center mb-4">
          <h3 className="font-bold text-lg flex items-center gap-2">
            🏆 <span>내 증표함</span>
          </h3>
          <button
            type="button"
            className="btn btn-sm btn-circle btn-ghost"
            onClick={onClose}
            aria-label="닫기"
          >
            ✕
          </button>
        </div>

        <p className="text-sm text-base-content/70 mb-4">
          참여했던 파티들에서 획득한 증표가 브라우저에 누적 보관됩니다.
        </p>

        {savedBadges.length === 0 ? (
          <div className="text-center py-8 bg-base-200 rounded-box">
            <p className="text-base-content/60 text-sm">
              아직 획득한 증표가 없습니다.
            </p>
            <p className="text-xs text-base-content/40 mt-1">
              파티에 참여해 대화를 나누고 증표를 수집해 보세요!
            </p>
          </div>
        ) : (
          <div className="space-y-3 max-h-80 overflow-y-auto pr-1">
            {savedBadges.map((badge, idx) => (
              <div
                key={`${badge.code}-${badge.partyName}-${idx}`}
                className="flex items-center gap-3 p-3 bg-base-200 rounded-box border border-base-300"
              >
                <div className="text-2xl select-none" role="img" aria-label={characterOf(badge.character).name}>
                  {characterOf(badge.character).emoji}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <h4 className="font-bold text-sm truncate">{badge.title}</h4>
                    <span className="text-xs text-base-content/50">
                      {badge.achievedAt}
                    </span>
                  </div>
                  <p className="text-xs text-base-content/70 mt-0.5">
                    {badge.partyName}
                  </p>
                  <p className="text-xs text-primary/80 mt-0.5">
                    {badge.description}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="modal-action">
          <button type="button" className="btn btn-primary btn-block" onClick={onClose}>
            확인
          </button>
        </div>
      </div>
      <button
        type="button"
        className="modal-backdrop"
        aria-label="내 증표함 닫기"
        onClick={onClose}
      />
    </div>
  )
}
