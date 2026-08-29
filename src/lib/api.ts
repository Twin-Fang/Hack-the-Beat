export const API_BASE = import.meta.env.VITE_API_URL || 'https://api.hack-the-beat.suhsaechan.kr'

export interface BadgeDto {
  code: string
  title: string
  description: string
  achieved: boolean
}

export interface MetPersonDto {
  name: string
  tagCode: string
  metAt?: string
  character?: string
  interests?: string[]
  myLevel?: number
  theirLevel?: number
}

export interface PassportResponse {
  partyCode: string
  partyName: string
  participantId: string
  name: string
  tagCode: string
  isHost: boolean
  isClosed: boolean
  metCount: number
  totalParticipants: number
  progressPercent: number
  badges: BadgeDto[]
  metPersons: MetPersonDto[]
  missionTargetName?: string
  missionCleared: boolean
  priceNotice: string
  character?: string
  interests?: string[]
  growthStage?: number
  missionTargetCharacter?: string
  missionTargetInterests?: string[]
}

export interface PartyStatus {
  code: string
  name: string
  capacity: number
  participantCount: number
  meetCount: number
  closed: boolean
  priceNotice: string
}

export interface PickItem {
  targetTagCode: string
  level: number
}

export interface MatchResponse {
  participantId: string
  name: string
  matchedCount: number
  mutualMatches: MetPersonDto[]
  allMetPersons: MetPersonDto[]
  reunionBadgeAchieved: boolean
  picksDeadline?: string
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })

  if (!res.ok) {
    // 서버 원문(JSON·프록시 HTML)을 그대로 띄우지 않는다
    const raw = await res.text().catch(() => '')
    let message = ''
    try {
      const parsed = JSON.parse(raw)
      message = typeof parsed?.message === 'string' ? parsed.message : ''
    } catch {
      message = raw.startsWith('<') ? '' : raw
    }
    throw new Error(
      message.trim() ||
        `요청을 처리하지 못했습니다 (${res.status}). 잠시 후 다시 시도해주세요.`
    )
  }

  return res.json()
}

export const api = {
  createParty: (data: {
    name: string
    hostName?: string
    capacity?: number
    hostCharacter?: string
    hostInterests?: string[]
    paid?: boolean
  }) =>
    request<PassportResponse>('/api/parties', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getPartyStatus: (code: string) =>
    request<PartyStatus>(`/api/parties/${encodeURIComponent(code)}`),

  joinParty: (
    code: string,
    data: {
      name: string
      fromTagCode?: string
      character?: string
      interests?: string[]
    }
  ) =>
    request<PassportResponse>(`/api/parties/${encodeURIComponent(code)}/join`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getPassport: (code: string, participantId: string) =>
    request<PassportResponse>(
      `/api/parties/${encodeURIComponent(code)}/passport/${encodeURIComponent(participantId)}`
    ),

  tagPerson: (code: string, data: { participantId: string; targetTagCode: string }) =>
    request<PassportResponse>(`/api/parties/${encodeURIComponent(code)}/tag`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  closeParty: (code: string, participantId?: string) =>
    request<PartyStatus>(`/api/parties/${encodeURIComponent(code)}/close`, {
      method: 'POST',
      body: participantId ? JSON.stringify({ participantId }) : undefined,
    }),

  submitPicks: (
    code: string,
    data: {
      participantId: string
      picks?: PickItem[]
      targetParticipantIds?: string[]
    }
  ) =>
    request<MatchResponse>(`/api/parties/${encodeURIComponent(code)}/picks`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getMatches: (code: string, participantId: string) =>
    request<MatchResponse>(
      `/api/parties/${encodeURIComponent(code)}/matches/${encodeURIComponent(participantId)}`
    ),
}
