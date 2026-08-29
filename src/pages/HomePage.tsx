import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useMutation } from '@tanstack/react-query'
import { api } from '../lib/api'
import { randomCharacter } from '../lib/character'
import { usePassportStore } from '../stores/usePassportStore'
import CharacterPicker from '../components/CharacterPicker'
import InterestPicker from '../components/InterestPicker'
import MyVaultModal from '../components/MyVaultModal'

export default function HomePage() {
  const navigate = useNavigate()
  const saveSession = usePassportStore((s) => s.saveSession)

  const [isVaultOpen, setIsVaultOpen] = useState(false)
  const [partyName, setPartyName] = useState('')
  const [character, setCharacter] = useState<string>(() => randomCharacter())
  const [interests, setInterests] = useState<string[]>([])
  const [joinCode, setJoinCode] = useState('')
  const [toastMessage, setToastMessage] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  // 정원이 20명을 넘으면 결제 확인 단계를 한 번 거친다 — 요금제가 화면에서 실제로 동작해야 한다
  const [capacity, setCapacity] = useState(20)
  const [isPaymentStep, setIsPaymentStep] = useState(false)
  const [isPaid, setIsPaid] = useState(false)

  const handleToggleInterest = (interest: string) => {
    setInterests((prev) =>
      prev.includes(interest)
        ? prev.filter((item) => item !== interest)
        : prev.length < 3
        ? [...prev, interest]
        : prev
    )
  }

  const createMutation = useMutation({
    mutationFn: (name: string) =>
      api.createParty({
        name,
        hostName: '호스트',
        capacity,
        hostCharacter: character,
        hostInterests: interests,
      }),
    onSuccess: (data) => {
      saveSession({
        partyCode: data.partyCode,
        participantId: data.participantId,
        tagCode: data.tagCode,
        name: data.name,
        isHost: data.isHost,
        character: data.character || character,
      })
      setToastMessage('초대 링크가 생성되었습니다')
      setTimeout(() => {
        navigate(`/party/${data.partyCode}`, { state: { justCreated: true } })
      }, 300)
    },
  })

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // 버튼을 비활성화해 막지 않고 눌린 뒤 이유를 알려준다 — 입력이 어긋나도 플로우가 멈추지 않는다
    if (!partyName.trim()) {
      setFormError('파티 이름을 입력해주세요.')
      return
    }
    setFormError(null)
    if (capacity > 20 && !isPaid) {
      setIsPaymentStep(true)
      return
    }
    createMutation.mutate(partyName.trim())
  }

  // 20명 초과 파티는 결제를 마쳐야 생성된다
  const handlePay = () => {
    setIsPaid(true)
    setIsPaymentStep(false)
    createMutation.mutate(partyName.trim())
  }

  const handleJoinSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!joinCode.trim()) return
    navigate(`/party/${joinCode.trim().toUpperCase()}`)
  }

  return (
    <div className="min-h-screen bg-base-200 flex flex-col items-center justify-center p-4 sm:p-6">
      {toastMessage ? (
        <div className="toast toast-top toast-center z-50">
          <div className="alert alert-success shadow-lg">
            <span>{toastMessage}</span>
          </div>
        </div>
      ) : null}

      <div className="card w-full max-w-md bg-base-100 shadow-2xl border border-base-300">
        <div className="card-body text-center p-6 sm:p-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-primary/10 text-primary mx-auto mb-2 text-3xl">
            🛂
          </div>
          <h1 className="card-title text-2xl sm:text-3xl font-extrabold justify-center tracking-tight">
            파티 패스포트
          </h1>
          <p className="text-base-content/70 text-sm mt-1 mb-5">
            파티에서 사람들을 만나 QR 코드를 태그하고,
            <br />
            증표를 모아 특별한 연결을 만들어보세요.
          </p>

          {/* 파티 생성 — 모달 없이 첫 화면에서 바로 완결시킨다 (클릭 한 번이면 1단계 끝) */}
          <form onSubmit={handleCreateSubmit} className="space-y-4 text-left">
            <div>
              <label className="label" htmlFor="partyName">
                <span className="label-text font-medium">파티 이름</span>
              </label>
              <input
                id="partyName"
                name="partyName"
                type="text"
                aria-label="파티 이름"
                data-testid="party-name-input"
                placeholder="예: 금요일 파티"
                className="input input-bordered w-full"
                aria-required="true"
                value={partyName}
                onChange={(e) => setPartyName(e.target.value)}
              />
            </div>

            {/* 내 캐릭터 선택 */}
            <CharacterPicker
              selected={character}
              onSelect={setCharacter}
              label="내 캐릭터 선택"
            />

            {/* 관심사 선택 */}
            <InterestPicker
              selected={interests}
              onToggle={handleToggleInterest}
              max={3}
            />

            <div>
              <label className="label" htmlFor="partyCapacity">
                <span className="label-text font-medium">예상 인원</span>
                <span className="label-text-alt text-xs text-base-content/60">
                  20명까지 무료
                </span>
              </label>
              <input
                id="partyCapacity"
                name="partyCapacity"
                type="number"
                min={1}
                max={200}
                aria-label="예상 인원"
                data-testid="party-capacity-input"
                className="input input-bordered w-full"
                value={capacity}
                onChange={(e) => {
                  setCapacity(Number(e.target.value) || 0)
                  setIsPaid(false)
                  setIsPaymentStep(false)
                }}
              />
              {capacity > 20 ? (
                <p className="text-xs text-warning mt-1.5" data-testid="paid-notice">
                  20명을 넘는 파티입니다. 파티당 9,900원이 부과됩니다.
                </p>
              ) : null}
            </div>

            {isPaymentStep ? (
              <div className="rounded-box border border-warning/40 bg-warning/10 p-4 space-y-2">
                <p className="text-sm font-bold">결제 확인</p>
                <p className="text-xs text-base-content/70">
                  예상 인원 {capacity}명 · 20명 초과 파티 이용료 9,900원
                </p>
                <button
                  type="button"
                  data-testid="pay-btn"
                  className="btn btn-warning btn-sm w-full font-bold"
                  onClick={handlePay}
                >
                  9,900원 결제하기
                </button>
              </div>
            ) : null}

            {formError ? <p className="text-error text-xs">{formError}</p> : null}

            {createMutation.isError ? (
              <p className="text-error text-xs">
                {createMutation.error instanceof Error
                  ? createMutation.error.message
                  : '파티 생성에 실패했습니다.'}
              </p>
            ) : null}

            <button
              type="submit"
              data-testid="create-party-btn"
              className="btn btn-primary btn-lg w-full text-base font-bold shadow-md"
              disabled={createMutation.isPending}
            >
              {createMutation.isPending ? (
                <span className="loading loading-spinner loading-sm" />
              ) : (
                '파티 만들기'
              )}
            </button>
          </form>

          <div className="divider text-xs text-base-content/40">또는</div>

          <form onSubmit={handleJoinSubmit}>
            <div className="join w-full">
              <input
                type="text"
                maxLength={6}
                aria-label="6자리 파티 코드"
                placeholder="6자리 파티 코드 입력"
                className="input input-bordered join-item w-full uppercase font-mono tracking-widest text-center"
                value={joinCode}
                onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
              />
              <button
                type="submit"
                className="btn btn-neutral join-item"
                disabled={joinCode.length !== 6}
              >
                입장
              </button>
            </div>
          </form>

          <div className="card-actions justify-center mt-5 pt-4 border-t border-base-200">
            <button
              type="button"
              className="btn btn-ghost btn-sm text-base-content/80 gap-1.5"
              onClick={() => setIsVaultOpen(true)}
            >
              <span>🏆</span>
              <span>내 증표함</span>
            </button>
          </div>

          <div className="text-xs text-base-content/50 mt-2">
            20명까지 무료 / 초과 시 9,900원
          </div>
        </div>
      </div>

      <MyVaultModal isOpen={isVaultOpen} onClose={() => setIsVaultOpen(false)} />
    </div>
  )
}
