import { useState } from 'react'
import { useParams, useNavigate } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { characterOf, LEVELS } from '../lib/character'
import { usePassportStore } from '../stores/usePassportStore'
import LevelPicker from '../components/LevelPicker'
import MyVaultModal from '../components/MyVaultModal'

export default function PartyResultPage() {
  const { code } = useParams<{ code: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const partyCode = (code || '').toUpperCase()

  const session = usePassportStore((s) => s.getSession(partyCode))
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [selectedLevels, setSelectedLevels] = useState<Record<string, number>>({})
  const [toastMessage, setToastMessage] = useState<string | null>(null)
  const [isVaultOpen, setIsVaultOpen] = useState(false)

  const showToast = (msg: string) => {
    setToastMessage(msg)
    setTimeout(() => setToastMessage(null), 3000)
  }

  // 매칭 결과 쿼리
  const matchQuery = useQuery({
    queryKey: ['matches', partyCode, session?.participantId],
    queryFn: () => api.getMatches(partyCode, session!.participantId),
    enabled: Boolean(partyCode && session?.participantId),
  })

  // 픽 제출 뮤테이션
  const submitMutation = useMutation({
    mutationFn: () =>
      api.submitPicks(partyCode, {
        participantId: session!.participantId,
        picks: selectedIds.map((tagCode) => ({
          targetTagCode: tagCode,
          level: selectedLevels[tagCode] ?? 2,
        })),
        targetParticipantIds: selectedIds,
      }),
    onSuccess: (data) => {
      queryClient.setQueryData(
        ['matches', partyCode, session?.participantId],
        data
      )
      showToast('선택이 완료되었습니다.')
    },
  })

  if (!session) {
    return (
      <div className="min-h-screen bg-base-200 flex flex-col items-center justify-center p-4">
        <div className="alert alert-warning max-w-sm">
          <span>참여 정보를 찾을 수 없습니다. 파티 페이지로 먼저 입장해주세요.</span>
        </div>
        <button
          type="button"
          className="btn btn-primary mt-4"
          onClick={() => navigate(`/party/${partyCode}`)}
        >
          파티 입장하기
        </button>
      </div>
    )
  }

  if (matchQuery.isPending) {
    return (
      <div className="min-h-screen bg-base-200 flex items-center justify-center">
        <span className="loading loading-spinner loading-lg text-primary" />
      </div>
    )
  }

  if (matchQuery.isError) {
    return (
      <div className="min-h-screen bg-base-200 flex flex-col items-center justify-center p-4 gap-4">
        <div className="alert alert-error max-w-sm">
          <span>{matchQuery.error.message}</span>
        </div>
        <div className="flex flex-col gap-2 w-full max-w-sm">
          <button
            type="button"
            className="btn btn-outline"
            onClick={() => matchQuery.refetch()}
          >
            다시 시도
          </button>
          <button
            type="button"
            className="btn btn-ghost"
            onClick={() => setIsVaultOpen(true)}
          >
            🏆 내 증표함
          </button>
          <button
            type="button"
            className="btn btn-primary font-bold"
            onClick={() => navigate('/')}
          >
            다음 파티 만들기
          </button>
        </div>
        <MyVaultModal isOpen={isVaultOpen} onClose={() => setIsVaultOpen(false)} />
      </div>
    )
  }

  const result = matchQuery.data
  const allMet = result.allMetPersons || []
  const mutualMatches = result.mutualMatches || []

  const toggleSelect = (tagCode: string) => {
    if (selectedIds.includes(tagCode)) {
      setSelectedIds(selectedIds.filter((item) => item !== tagCode))
    } else {
      setSelectedIds([...selectedIds, tagCode])
      if (!selectedLevels[tagCode]) {
        setSelectedLevels((prev) => ({ ...prev, [tagCode]: 2 }))
      }
    }
  }

  const handleLevelChange = (tagCode: string, level: number) => {
    setSelectedLevels((prev) => ({ ...prev, [tagCode]: level }))
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    submitMutation.mutate()
  }

  const getLevelText = (level?: number) => {
    const found = LEVELS.find((l) => l.level === level)
    return found ? found.text : '🙌 반가웠어요'
  }

  return (
    <div className="min-h-screen bg-base-200 py-8 px-4 sm:px-6 flex flex-col items-center">
      {toastMessage ? (
        <div className="toast toast-top toast-center z-50">
          <div className="alert alert-success shadow-lg">
            <span>{toastMessage}</span>
          </div>
        </div>
      ) : null}

      <div className="w-full max-w-lg space-y-6">
        {/* 헤더 */}
        <div className="text-center space-y-1">
          <span className="badge badge-accent badge-outline font-mono">
            PARTY #{partyCode}
          </span>
          <h1 className="text-2xl sm:text-3xl font-black">파티 회고 & 매칭</h1>
          <p className="text-sm text-base-content/70">
            파티에서 대화한 분들 중 다시 만나고 싶은 사람을 비밀리에 선택하세요.
            <br />
            서로를 선택한 경우에만 결과가 공개됩니다.
          </p>
        </div>

        {/* 재선택 마감 안내 (B4 리텐션 트리거: 종료 24시간 이내) */}
        {result.picksDeadline ? (
          <div className="alert alert-info text-xs py-2.5 px-3">
            <span>
              ⏰ 재선택 마감까지 <strong>24시간</strong> — {result.picksDeadline}까지 선택하면
              상호 매칭 결과를 확인할 수 있어요.
            </span>
          </div>
        ) : null}

        {/* 상호 선택 폼 */}
        <div className="card bg-base-100 shadow-xl border border-base-300">
          <div className="card-body p-6">
            <h2 className="text-lg font-bold flex items-center gap-2 mb-3">
              <span>💌</span> 다시 만나고 싶은 사람
            </h2>

            {allMet.length === 0 ? (
              <div className="text-center py-6 text-base-content/50 text-xs">
                파티 중에 만난 사람이 없습니다.
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-3">
                <div className="space-y-3 max-h-72 overflow-y-auto pr-1">
                  {allMet.map((person) => {
                    const isChecked = selectedIds.includes(person.tagCode)
                    const char = characterOf(person.character, person.tagCode)

                    return (
                      <div
                        key={person.tagCode}
                        className={`p-3 rounded-box border transition-all ${
                          isChecked
                            ? 'bg-primary/10 border-primary shadow-sm'
                            : 'bg-base-200/60 border-base-300 hover:bg-base-200'
                        }`}
                      >
                        <label
                          htmlFor={`pick-${person.tagCode}`}
                          className="flex items-center justify-between cursor-pointer select-none"
                        >
                          <div className="flex items-center gap-3">
                            <input
                              id={`pick-${person.tagCode}`}
                              type="checkbox"
                              className="checkbox checkbox-primary"
                              checked={isChecked}
                              onChange={() => toggleSelect(person.tagCode)}
                              aria-label={`${person.name} 선택`}
                            />
                            <div className="flex items-center gap-2">
                              <span
                                className="text-xl select-none"
                                role="img"
                                aria-label={char.name}
                              >
                                {char.emoji}
                              </span>
                              <div>
                                <span className="font-bold text-sm">
                                  {person.name}
                                </span>
                                <span className="text-xs text-base-content/50 ml-2 font-mono">
                                  #{person.tagCode}
                                </span>
                              </div>
                            </div>
                          </div>
                        </label>

                        {/* 선택 시 다시 만나고 싶은 정도 선택기 노출 */}
                        {isChecked ? (
                          <div className="mt-3 pt-2.5 border-t border-primary/20">
                            <LevelPicker
                              level={selectedLevels[person.tagCode] ?? 2}
                              onChange={(lvl) =>
                                handleLevelChange(person.tagCode, lvl)
                              }
                              label="다시 만나고 싶은 정도"
                            />
                          </div>
                        ) : null}
                      </div>
                    )
                  })}
                </div>

                {submitMutation.isError ? (
                  <p className="text-error text-xs mt-3">
                    {submitMutation.error instanceof Error
                      ? submitMutation.error.message
                      : '선택 제출에 실패했습니다. 잠시 후 다시 시도해주세요.'}
                  </p>
                ) : null}

                <button
                  type="submit"
                  data-testid="submit-picks-btn"
                  className="btn btn-primary btn-block mt-4 font-bold shadow"
                  disabled={submitMutation.isPending}
                >
                  {submitMutation.isPending ? (
                    <span className="loading loading-spinner loading-sm" />
                  ) : (
                    '선택 제출'
                  )}
                </button>
              </form>
            )}
          </div>
        </div>

        {/* 상호 매칭 결과 카드 */}
        <div className="card bg-base-100 shadow-xl border border-base-300">
          <div className="card-body p-6">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-lg font-bold flex items-center gap-2">
                <span>✨</span> 서로 선택된 사람
              </h2>
              <span className="badge badge-primary font-bold">
                {mutualMatches.length}명 매칭
              </span>
            </div>

            {mutualMatches.length === 0 ? (
              <div className="text-center py-6 bg-base-200 rounded-box text-xs text-base-content/60">
                아직 서로를 선택한 매칭 결과가 없습니다.
                <br />
                상대방도 선택을 제출하면 이곳에 서로의 이름이 나타납니다.
              </div>
            ) : (
              <div className="space-y-3">
                {mutualMatches.map((m) => {
                  const myLevelText = getLevelText(m.myLevel)
                  const theirLevelText = getLevelText(m.theirLevel)
                  const char = characterOf(m.character, m.tagCode)

                  return (
                    <div
                      key={m.tagCode}
                      className="p-3.5 bg-gradient-to-r from-primary/15 to-secondary/15 rounded-box border border-primary/30 space-y-2.5"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <span
                            className="text-2xl select-none"
                            role="img"
                            aria-label={char.name}
                          >
                            {char.emoji}
                          </span>
                          <div>
                            <h4 className="font-bold text-sm">{m.name}</h4>
                            <p className="text-xs text-primary font-medium">
                              서로를 다시 만나고 싶어 합니다!
                            </p>
                          </div>
                        </div>
                        <span className="text-2xl select-none">💖</span>
                      </div>

                      {/* 양쪽의 다시 만나고 싶은 정도 */}
                      <div className="bg-base-100/90 rounded-box p-2.5 text-xs flex items-center justify-between border border-base-300">
                        <span className="font-semibold text-primary">
                          나 {myLevelText}
                        </span>
                        <span className="text-base-content/40">·</span>
                        <span className="font-semibold text-secondary">
                          상대 {theirLevelText}
                        </span>
                      </div>

                      {/* 관심사 */}
                      {m.interests && m.interests.length > 0 ? (
                        <div className="flex flex-wrap gap-1 pt-0.5">
                          {m.interests.map((it) => (
                            <span
                              key={it}
                              className="badge badge-xs badge-outline badge-primary font-medium"
                            >
                              {it}
                            </span>
                          ))}
                        </div>
                      ) : null}
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </div>

        {/* 하단 액션: 내 증표함 및 다음 파티 만들기 (B4 리텐션 완벽 대응) */}
        <div className="flex flex-col sm:flex-row gap-3 pt-2">
          <button
            type="button"
            className="btn btn-outline flex-1"
            onClick={() => setIsVaultOpen(true)}
          >
            🏆 내 증표함
          </button>
          <button
            type="button"
            className="btn btn-primary flex-1 font-bold shadow"
            onClick={() => navigate('/')}
          >
            다음 파티 만들기
          </button>
        </div>
      </div>

      {/* 내 증표함 모달 */}
      <MyVaultModal
        isOpen={isVaultOpen}
        onClose={() => setIsVaultOpen(false)}
      />
    </div>
  )
}
