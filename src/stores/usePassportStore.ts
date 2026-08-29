import { create } from 'zustand'
import type { BadgeDto } from '../lib/api'

export interface SessionData {
  partyCode: string
  participantId: string
  tagCode: string
  name: string
  isHost: boolean
  character?: string
}

export interface SavedBadge {
  code: string
  title: string
  description: string
  achievedAt: string
  partyName: string
  character?: string
}

interface PassportStoreState {
  sessions: Record<string, SessionData>
  savedBadges: SavedBadge[]
  saveSession: (session: SessionData) => void
  getSession: (partyCode: string) => SessionData | undefined
  accumulateBadges: (partyName: string, badges: BadgeDto[], character?: string) => void
}

const SESSIONS_KEY = 'htb_passport_sessions'
const BADGES_KEY = 'htb_saved_badges'

function loadFromStorage<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

function saveToStorage<T>(key: string, data: T): void {
  try {
    localStorage.setItem(key, JSON.stringify(data))
  } catch {
    // ignore
  }
}

export const usePassportStore = create<PassportStoreState>((set, get) => ({
  sessions: loadFromStorage<Record<string, SessionData>>(SESSIONS_KEY, {}),
  savedBadges: loadFromStorage<SavedBadge[]>(BADGES_KEY, []),

  saveSession: (session: SessionData) => {
    const updated = {
      ...get().sessions,
      [session.partyCode.toUpperCase()]: session,
    }
    saveToStorage(SESSIONS_KEY, updated)
    set({ sessions: updated })
  },

  getSession: (partyCode: string) => {
    return get().sessions[partyCode.toUpperCase()]
  },

  accumulateBadges: (partyName: string, badges: BadgeDto[], character?: string) => {
    const current = get().savedBadges
    const achievedNow = badges.filter((b) => b.achieved)
    const newItems: SavedBadge[] = []

    const nowStr = new Date().toLocaleDateString('ko-KR', {
      month: 'short',
      day: 'numeric',
    })

    for (const b of achievedNow) {
      const exists = current.some(
        (c) => c.code === b.code && c.partyName === partyName
      )
      if (!exists) {
        newItems.push({
          code: b.code,
          title: b.title,
          description: b.description,
          achievedAt: nowStr,
          partyName,
          character,
        })
      }
    }

    if (newItems.length > 0) {
      const updated = [...current, ...newItems]
      saveToStorage(BADGES_KEY, updated)
      set({ savedBadges: updated })
    }
  },
}))
