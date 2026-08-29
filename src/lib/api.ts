export const API_BASE = import.meta.env.VITE_API_URL || 'https://api.hack-the-beat.suhsaechan.kr'

export interface BadgeDto {
  code: string
  title: string
  description: string
  achieved: boolean
}

export interface MetPersonDto {
  participantId: string
  name: string
  tagCode: string
  metAt?: string
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

export interface MatchResponse {
  participantId: string
  name: string
  matchedCount: number
  mutualMatches: MetPersonDto[]
  allMetPersons: MetPersonDto[]
  reunionBadgeAchieved: boolean
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
    const errorText = await res.text().catch(() => '')
    throw new Error(errorText || `요청 실패 (${res.status})`)
  }

  return res.json()
}

export const api = {
  createParty: (data: { name: string; hostName?: string; capacity?: number }) =>
    request<PassportResponse>('/api/parties', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getPartyStatus: (code: string) =>
    request<PartyStatus>(`/api/parties/${encodeURIComponent(code)}`),

  joinParty: (code: string, data: { name: string; fromTagCode?: string }) =>
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

  closeParty: (code: string) =>
    request<PartyStatus>(`/api/parties/${encodeURIComponent(code)}/close`, {
      method: 'POST',
    }),

  submitPicks: (
    code: string,
    data: { participantId: string; targetParticipantIds: string[] }
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
