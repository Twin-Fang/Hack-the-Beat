import { LEVELS } from '../lib/character'

interface LevelPickerProps {
  level: number
  onChange: (level: number) => void
  label?: string
}

/**
 * 다시 만나고 싶은 정도(1~3단계) 선택용 버튼 그룹 컴포넌트
 */
export default function LevelPicker({
  level,
  onChange,
  label = '다시 만나고 싶은 정도',
}: LevelPickerProps) {
  return (
    <div className="w-full">
      {label ? (
        <label className="label">
          <span className="label-text font-medium">{label}</span>
        </label>
      ) : null}
      <div
        data-testid="level-picker"
        role="radiogroup"
        aria-label={label}
        className="grid grid-cols-3 gap-2"
      >
        {LEVELS.map((item) => {
          const isSelected = level === item.level

          return (
            <button
              key={item.level}
              type="button"
              role="radio"
              aria-checked={isSelected}
              aria-label={item.text}
              data-testid={`level-${item.level}`}
              onClick={() => onChange(item.level)}
              className={`btn btn-sm sm:btn-md flex items-center justify-center transition-all ${
                isSelected
                  ? 'btn-primary shadow-sm ring-2 ring-primary ring-offset-2'
                  : 'btn-outline border-base-300 hover:bg-base-200'
              }`}
            >
              <span className="mr-1">{item.icon}</span>
              <span className="font-semibold text-xs sm:text-sm">{item.label}</span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
