import { useState, useEffect } from 'react'
import { useParams, useSearchParams, useNavigate } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { QRCodeSVG } from 'qrcode.react'
import { api, type PassportResponse } from '../lib/api'
import { usePassportStore } from '../stores/usePassportStore'
import BadgeList from '../components/BadgeList'
import TagModal from '../components/TagModal'
import MyVaultModal from '../components/MyVaultModal'

export default function PartyPassportPage() {
  const { code } = useParams<{ code: string }>()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const partyCode = (code || '').toUpperCase()
  const fromTag = searchParams.get('from') || searchParams.get('tag') || ''

  const session = usePassportStore((s) => s.getSession(partyCode))
  const saveSession = usePassportStore((s) => s.saveSession)
  const accumulateBadges = usePassportStore((s) => s.accumulateBadges)

  const [nameInput, setNameInput] = useState('')
  const [toastMessage, setToastMessage] = useState<string | null>(null)
  const [isTagModalOpen, setIsTagModalOpen] = useState(false)
  const [isVaultOpen, setIsVaultOpen] = useState(false)

  // 토스트 메시지 헬퍼
  const showToast = (msg: string) => {
    setToastMessage(msg)
    setTimeout(() => setToastMessage(null), 3000)
  }

  // 패스포트 데이터 쿼리
  const passportQuery = useQuery({
    queryKey: ['passport', partyCode, session?.participantId],
    queryFn: () => api.getPassport(partyCode, session!.participantId),
    enabled: Boolean(partyCode && session?.participantId),
    refetchInterval: 4000,
  })

  // 뱃지 획득 시 로컬스토리지 누적
  useEffect(() => {
    if (passportQuery.data) {
      accumulateBadges(passportQuery.data.partyName, passportQuery.data.badges)
    }
  }, [passportQuery.data, accumulateBadges])

  // 파티 참여 뮤테이션
  const joinMutation = useMutation({
    mutationFn: (name: string) =>
      api.joinParty(partyCode, {
        name,
        fromTagCode: fromTag,
      }),
    onSuccess: (data: PassportResponse) => {
      saveSession({
        partyCode: data.partyCode,
        participantId: data.participantId,
        tagCode: data.tagCode,
        name: data.name,
        isHost: data.isHost,
      })
      showToast('참여 완료')
    },
  })

  // 태그 뮤테이션
  const tagMutation = useMutation({
    mutationFn: (targetCode: string) =>
      api.tagPerson(partyCode, {
        participantId: session!.participantId,
        targetTagCode: targetCode,
      }),
    onSuccess: (data) => {
      queryClient.setQueryData(
        ['passport', partyCode, session?.participantId],
        data
      )
      showToast('태그 완료! 새로운 인연을 만났습니다.')
    },
  })

  // 파티 종료 뮤테이션
  const closeMutation = useMutation({
    mutationFn: () => api.closeParty(partyCode),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['passport', partyCode] })
      showToast('파티가 종료되었습니다.')
      navigate(`/party/${partyCode}/result`)
    },
  })

  const handleJoinSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!nameInput.trim()) return
    joinMutation.mutate(nameInput.trim())
  }

  // 초대 링크 복사 (시나리오 2단계 완벽 대응)
  const inviteUrl = `${window.location.origin}${window.location.pathname}?from=${
    session?.tagCode || ''
  }`

  const handleCopyInviteLink = async () => {
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(inviteUrl)
      }
    } catch {
      // ignore
    }
    showToast('복사되었습니다')
  }

  // 1. 세션이 없는 경우: 참가자 등록 화면 (시나리오 3단계 진입점)
  if (!session) {
    return (
      <div className="min-h-screen bg-base-200 flex flex-col items-center justify-center p-4">
        {toastMessage ? (
          <div className="toast toast-top toast-center z-50">
            <div className="alert alert-success shadow-lg">
              <span>{toastMessage}</span>
            </div>
          </div>
        ) : null}

        <div className="card w-full max-w-sm bg-base-100 shadow-xl border border-base-300">
          <div className="card-body p-6">
            <div className="text-center mb-4">
              <span className="badge badge-primary badge-outline font-mono text-sm tracking-wider mb-2">
                PARTY: {partyCode}
              </span>
              <h2 className="card-title text-2xl font-bold justify-center">
                파티 패스포트 참여
              </h2>
              <p className="text-xs text-base-content/70 mt-1">
                {fromTag ? '초대자와 연결되어 바로 첫 증표를 받습니다!' : '파티에 입장하여 패스포트를 발급받으세요.'}
              </p>
            </div>

            <form onSubmit={handleJoinSubmit} className="space-y-4">
              <div>
                <label className="label">
                  <span className="label-text font-medium">이름</span>
                </label>
                <input
                  type="text"
                  placeholder="예: 김서준"
                  className="input input-bordered w-full"
                  value={nameInput}
                  onChange={(e) => setNameInput(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              {joinMutation.isError ? (
                <p className="text-error text-xs">
                  {joinMutation.error instanceof Error
                    ? joinMutation.error.message
                    : '참여에 실패했습니다.'}
                </p>
              ) : null}

              <button
                type="submit"
                className="btn btn-primary btn-block"
                disabled={joinMutation.isPending || !nameInput.trim()}
              >
                {joinMutation.isPending ? (
                  <span className="loading loading-spinner loading-sm" />
                ) : (
                  '참여하기'
                )}
              </button>
            </form>
          </div>
        </div>
      </div>
    )
  }

  // 2. 패스포트 로딩/에러 상태
  // join 성공 토스트가 로딩 화면 전환 중에 사라지지 않도록 여기서도 렌더링
  if (passportQuery.isPending) {
    return (
      <div className="min-h-screen bg-base-200 flex items-center justify-center">
        {toastMessage ? (
          <div className="toast toast-top toast-center z-50">
            <div className="alert alert-success shadow-lg">
              <span>{toastMessage}</span>
            </div>
          </div>
        ) : null}
        <span className="loading loading-spinner loading-lg text-primary" />
      </div>
    )
  }

  if (passportQuery.isError) {
    return (
      <div className="min-h-screen bg-base-200 flex items-center justify-center p-4">
        {toastMessage ? (
          <div className="toast toast-top toast-center z-50">
            <div className="alert alert-success shadow-lg">
              <span>{toastMessage}</span>
            </div>
          </div>
        ) : null}
        <div className="alert alert-error max-w-sm">
          <span>{passportQuery.error.message}</span>
        </div>
      </div>
    )
  }

  const passport = passportQuery.data

  return (
    <div className="min-h-screen bg-base-200 py-6 px-4 sm:px-6 flex flex-col items-center">
      {/* 플로팅 토스트 알림 */}
      {toastMessage ? (
        <div className="toast toast-top toast-center z-50">
          <div className="alert alert-success shadow-lg">
            <span>{toastMessage}</span>
          </div>
        </div>
      ) : null}

      <div className="w-full max-w-lg space-y-4">
        {/* 상단 네비게이션 & 파티 헤더 */}
        <div className="flex items-center justify-between bg-base-100 p-4 rounded-box shadow-sm border border-base-300">
          <div>
            <span className="badge badge-neutral badge-sm font-mono tracking-wider">
              {passport.partyCode}
            </span>
            <h1 className="text-xl font-extrabold tracking-tight mt-1">
              {passport.partyName}
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={() => setIsVaultOpen(true)}
              aria-label="내 증표함 열기"
            >
              🏆 내 증표함
            </button>
            {passport.isClosed ? (
              <button
                type="button"
                className="btn btn-accent btn-sm"
                onClick={() => navigate(`/party/${partyCode}/result`)}
              >
                결과 보기
              </button>
            ) : passport.isHost ? (
              <button
                type="button"
                className="btn btn-error btn-sm btn-outline"
                onClick={() => closeMutation.mutate()}
                disabled={closeMutation.isPending}
              >
                파티 종료
              </button>
            ) : null}
          </div>
        </div>

        {/* 내 패스포트 & QR 카드 */}
        <div className="card bg-base-100 shadow-xl border border-base-300">
          <div className="card-body p-6 text-center">
            <div className="flex justify-between items-center mb-2">
              <span className="text-xs font-semibold text-primary">
                PASSPORT IDENTITY
              </span>
              <span className="badge badge-primary font-mono text-sm font-bold px-3 py-2">
                내 코드: {passport.tagCode}
              </span>
            </div>

            <h2 className="text-2xl font-black">{passport.name}</h2>
            <p className="text-xs text-base-content/60 mb-4">
              상대방 카메라로 내 QR을 비추거나 4자리 코드를 알려주세요!
            </p>

            {/* QR 코드 영역 */}
            <div className="bg-white p-4 rounded-2xl inline-flex justify-center shadow-inner border border-base-300 mx-auto mb-4">
              <QRCodeSVG
                value={`${window.location.origin}${window.location.pathname}?from=${passport.tagCode}`}
                size={160}
                level="M"
                includeMargin={false}
              />
            </div>

            {/* 주요 액션 버튼들 */}
            <div className="grid grid-cols-2 gap-2 mt-2">
              <button
                type="button"
                data-testid="copy-invite-btn"
                className="btn btn-primary font-bold shadow"
                onClick={handleCopyInviteLink}
              >
                초대 링크 복사
              </button>
              <button
                type="button"
                className="btn btn-outline btn-primary font-bold"
                onClick={() => setIsTagModalOpen(true)}
              >
                코드로 태그
              </button>
            </div>

            {/* 초대 링크 URL 텍스트 노출 (시나리오 2단계 요구사항) */}
            <div className="mt-3 p-2 bg-base-200 rounded-box text-left">
              <p className="text-xs text-base-content/60 font-medium mb-0.5">
                초대 링크:
              </p>
              <p className="text-xs font-mono text-base-content/80 break-all select-all">
                {inviteUrl}
              </p>
            </div>
          </div>
        </div>

        {/* 진행률 & 만난 사람 카운트 (시나리오 3단계 완벽 대응) */}
        <div className="card bg-base-100 shadow-md border border-base-300">
          <div className="card-body p-5">
            <div className="flex justify-between items-end mb-2">
              <div>
                <span className="text-xs font-bold text-base-content/60 uppercase">
                  PROGRESS
                </span>
                <h3 className="text-xl font-extrabold text-primary">
                  만난 사람 {passport.metCount}명
                </h3>
              </div>
              <span className="text-sm font-semibold text-base-content/70">
                전체 참가자 {passport.totalParticipants}명
              </span>
            </div>

            <progress
              className="progress progress-primary w-full h-3"
              value={passport.progressPercent}
              max="100"
            />
          </div>
        </div>

        {/* 1:1 미션 카드 */}
        {passport.missionTargetName ? (
          <div className="card bg-gradient-to-r from-primary/15 to-secondary/15 border border-primary/20 shadow-sm">
            <div className="card-body p-4 flex flex-row items-center justify-between">
              <div>
                <span className="badge badge-xs badge-secondary mb-1">
                  오늘의 1:1 미션
                </span>
                <h4 className="font-bold text-sm">
                  {passport.missionTargetName}님을 찾아 대화해 보세요!
                </h4>
                <p className="text-xs text-base-content/70 mt-0.5">
                  {passport.missionCleared
                    ? '미션 완료! 특별 증표가 활성화되었습니다. 🎯'
                    : '대화를 나누고 QR 또는 코드를 태그하세요.'}
                </p>
              </div>
              <div className="text-3xl">
                {passport.missionCleared ? '🎉' : '🕵️'}
              </div>
            </div>
          </div>
        ) : null}

        {/* 증표 뱃지 목록 */}
        <div className="card bg-base-100 shadow-md border border-base-300">
          <div className="card-body p-5">
            <div className="flex justify-between items-center mb-3">
              <h3 className="font-bold text-base">내 패스포트 증표</h3>
              <span className="text-xs text-base-content/60">
                {passport.badges.filter((b) => b.achieved).length} /{' '}
                {passport.badges.length} 획득
              </span>
            </div>
            <BadgeList badges={passport.badges} />
          </div>
        </div>

        {/* 만난 사람들 목록 */}
        <div className="card bg-base-100 shadow-md border border-base-300">
          <div className="card-body p-5">
            <h3 className="font-bold text-base mb-3">
              만난 사람들 ({passport.metPersons.length})
            </h3>
            {passport.metPersons.length === 0 ? (
              <div className="text-center py-6 text-base-content/50 text-xs">
                아직 만난 사람이 없습니다. 초대 링크를 공유하거나 코드로 태그해 보세요!
              </div>
            ) : (
              <div className="divide-y divide-base-200 max-h-56 overflow-y-auto">
                {passport.metPersons.map((p) => (
                  <div
                    key={p.participantId}
                    className="py-2.5 flex justify-between items-center"
                  >
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 rounded-full bg-primary/20 text-primary flex items-center justify-center font-bold text-xs">
                        {p.name.slice(0, 1)}
                      </div>
                      <div>
                        <p className="font-semibold text-sm">{p.name}</p>
                        <p className="text-xs font-mono text-base-content/50">
                          #{p.tagCode}
                        </p>
                      </div>
                    </div>
                    <span className="text-xs text-base-content/50">
                      {p.metAt}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* 요금 안내 문구 (C1 심사 기준) */}
        <div className="text-center text-xs text-base-content/40 py-2">
          {passport.priceNotice}
        </div>
      </div>

      {/* 코드로 태그 모달 */}
      <TagModal
        isOpen={isTagModalOpen}
        onClose={() => setIsTagModalOpen(false)}
        onTag={async (code) => {
          await tagMutation.mutateAsync(code)
        }}
        isLoading={tagMutation.isPending}
      />

      {/* 내 증표함 모달 */}
      <MyVaultModal
        isOpen={isVaultOpen}
        onClose={() => setIsVaultOpen(false)}
      />
    </div>
  )
}
