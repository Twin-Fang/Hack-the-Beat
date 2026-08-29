import { characterOf, growthOf } from '../lib/character'

interface CharacterAvatarProps {
  characterKey?: string
  fallbackSeed?: string
  size?: 'sm' | 'md' | 'lg'
  metCount?: number
  showGrowth?: boolean
  showLabel?: boolean
}

const SIZE_STYLES = {
  sm: {
    container: 'w-10 h-10 text-xl',
    growthBadge: 'text-xs -bottom-1 -right-1 w-4 h-4',
    stageBadge: 'badge-xs',
  },
  md: {
    container: 'w-14 h-14 text-3xl',
    growthBadge: 'text-sm -bottom-1 -right-1 w-6 h-6',
    stageBadge: 'badge-sm',
  },
  lg: {
    container: 'w-20 h-20 text-5xl',
    growthBadge: 'text-base -bottom-1.5 -right-1.5 w-7 h-7',
    stageBadge: 'badge-md',
  },
} as const

/**
 * 만난 사람 수에 따라 진화하는 파티 캐릭터 아바타 컴포넌트
 */
export default function CharacterAvatar({
  characterKey,
  fallbackSeed,
  size = 'md',
  metCount = 0,
  showGrowth = false,
  showLabel = false,
}: CharacterAvatarProps) {
  const character = characterOf(characterKey, fallbackSeed)
  const growth = growthOf(metCount)
  const style = SIZE_STYLES[size] ?? SIZE_STYLES.md

  return (
    <div className="inline-flex flex-col items-center gap-1">
      <div className="relative inline-block">
        {/* 캐릭터 메인 원형 아바타 */}
        <div
          className={`rounded-full bg-primary/10 border-2 border-primary/20 flex items-center justify-center shadow-inner select-none ${style.container}`}
          aria-label={character.name}
          role="img"
        >
          <span>{character.emoji}</span>
        </div>

        {/* 우하단 성장 이모지 (🌱 / 🌿 / 🌸) */}
        {showGrowth ? (
          <div
            className={`absolute flex items-center justify-center bg-base-100 rounded-full shadow-sm border border-base-300 select-none ${style.growthBadge}`}
            role="img"
            aria-label={`성장 단계: ${growth.label}`}
          >
            <span>{growth.emoji}</span>
          </div>
        ) : null}
      </div>

      {/* 성장 단계 라벨 배지 */}
      {showLabel ? (
        <span
          data-testid="growth-stage"
          className={`badge badge-primary badge-outline font-semibold ${style.stageBadge}`}
        >
          {growth.label} ({growth.stage}단계)
        </span>
      ) : null}
    </div>
  )
}
