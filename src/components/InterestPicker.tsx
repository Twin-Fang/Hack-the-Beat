import { INTERESTS } from '../lib/character'

interface InterestPickerProps {
  selected: string[]
  onToggle: (interest: string) => void
  max?: number
}

/**
 * 12종 관심사 다중 선택 토글 칩 컴포넌트 (최대 선택 개수 제한)
 */
export default function InterestPicker({
  selected,
  onToggle,
  max = 3,
}: InterestPickerProps) {
  const isMaxReached = selected.length >= max

  return (
    <div className="w-full">
      <div className="flex justify-between items-center mb-2">
        <span className="label-text font-medium text-xs text-base-content/70">
          관심사 선택 (최대 {max}개)
        </span>
        <span className="text-xs font-semibold text-primary">
          {selected.length} / {max}
        </span>
      </div>
      <div
        data-testid="interest-picker"
        className="flex flex-wrap gap-2"
      >
        {INTERESTS.map((interest) => {
          const isSelected = selected.includes(interest)
          const isDisabled = !isSelected && isMaxReached

          return (
            <button
              key={interest}
              type="button"
              disabled={isDisabled}
              aria-pressed={isSelected}
              aria-label={interest}
              data-testid={`interest-chip-${interest}`}
              onClick={() => onToggle(interest)}
              className={`btn btn-sm rounded-full transition-all ${
                isSelected
                  ? 'btn-primary shadow-sm'
                  : isDisabled
                  ? 'btn-outline border-base-300 opacity-40 cursor-not-allowed'
                  : 'btn-outline border-base-300 hover:border-primary'
              }`}
            >
              {isSelected ? '✓ ' : null}
              {interest}
            </button>
          )
        })}
      </div>
    </div>
  )
}
