import { CHARACTERS } from '../lib/character'

interface CharacterPickerProps {
  selected?: string
  onSelect: (key: string) => void
  label?: string
}

/**
 * 8종 파티 캐릭터 선택용 라디오 버튼 그룹 컴포넌트
 */
export default function CharacterPicker({
  selected,
  onSelect,
  label,
}: CharacterPickerProps) {
  return (
    <div className="w-full">
      {label ? (
        <label className="label">
          <span className="label-text font-medium">{label}</span>
        </label>
      ) : null}
      <div
        role="radiogroup"
        aria-label={label ?? '캐릭터 선택'}
        data-testid="character-picker"
        className="grid grid-cols-4 gap-2 sm:gap-2.5"
      >
        {CHARACTERS.map((c) => {
          const isSelected = selected === c.key

          return (
            <button
              key={c.key}
              type="button"
              role="radio"
              aria-checked={isSelected}
              aria-label={c.name}
              data-testid={`character-option-${c.key}`}
              onClick={() => onSelect(c.key)}
              className={`btn flex flex-col items-center justify-center h-auto py-2.5 px-1 border transition-all ${
                isSelected
                  ? 'btn-primary border-primary ring-2 ring-primary ring-offset-2 shadow-sm'
                  : 'btn-ghost bg-base-200/60 border-base-300 hover:bg-base-200'
              }`}
            >
              <span className="text-2xl mb-1 select-none" role="img" aria-label={c.name}>
                {c.emoji}
              </span>
              <span className="text-xs font-semibold select-none">{c.name}</span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
