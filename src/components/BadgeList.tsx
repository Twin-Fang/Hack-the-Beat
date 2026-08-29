import type { BadgeDto } from '../lib/api'

interface BadgeListProps {
  badges: BadgeDto[]
}

const BADGE_ICONS: Record<string, string> = {
  FIRST_MEET: '👋',
  ICE_BREAKER: '🧊',
  PARTY_PEOPLE: '🎉',
  PARTY_MASTER: '👑',
  MISSION_CLEAR: '🎯',
  REUNION: '💫',
}

export default function BadgeList({ badges }: BadgeListProps) {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
      {badges.map((badge) => {
        const isAchieved = badge.achieved
        const icon = BADGE_ICONS[badge.code] || '🏅'

        return (
          <div
            key={badge.code}
            data-testid={`badge-${badge.code}`}
            className={`card border p-3.5 transition-all text-left ${
              isAchieved
                ? 'bg-primary/10 border-primary text-base-content shadow-sm'
                : 'bg-base-200/50 border-base-300 opacity-50 grayscale'
            }`}
          >
            <div className="flex items-center gap-2 mb-1">
              <span className="text-2xl" role="img" aria-label={badge.title}>
                {icon}
              </span>
              <div>
                <h4 className="font-bold text-sm tracking-tight">{badge.title}</h4>
                <span
                  className={`badge badge-xs font-semibold ${
                    isAchieved ? 'badge-primary' : 'badge-ghost'
                  }`}
                >
                  {isAchieved ? '획득 완료' : '미획득'}
                </span>
              </div>
            </div>
            <p className="text-xs text-base-content/70 mt-1 line-clamp-2">
              {badge.description}
            </p>
          </div>
        )
      })}
    </div>
  )
}
