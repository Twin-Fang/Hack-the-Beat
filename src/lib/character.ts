export const CHARACTERS = [
  { key: 'FOX', emoji: '🦊', name: '여우' },
  { key: 'FROG', emoji: '🐸', name: '개구리' },
  { key: 'PANDA', emoji: '🐼', name: '판다' },
  { key: 'CHICK', emoji: '🐥', name: '병아리' },
  { key: 'OCTOPUS', emoji: '🐙', name: '문어' },
  { key: 'LION', emoji: '🦁', name: '사자' },
  { key: 'RABBIT', emoji: '🐰', name: '토끼' },
  { key: 'KOALA', emoji: '🐨', name: '코알라' },
] as const;

export type CharacterKey = (typeof CHARACTERS)[number]['key'];
export type CharacterItem = (typeof CHARACTERS)[number];

export const INTERESTS = [
  '게임',
  '러닝',
  '영화',
  '음악',
  '여행',
  '요리',
  '독서',
  '그림',
  '축구',
  '반려동물',
  '카페',
  '개발',
] as const;

export type Interest = (typeof INTERESTS)[number];

export const LEVELS = [
  { level: 1, label: '가볍게', icon: '☕', text: '☕ 가볍게' },
  { level: 2, label: '반가웠어요', icon: '🙌', text: '🙌 반가웠어요' },
  { level: 3, label: '꼭 다시', icon: '💫', text: '💫 꼭 다시' },
] as const;

export type LevelItem = (typeof LEVELS)[number];

/**
 * 8종 캐릭터 중 1개를 무작위로 선택하여 key를 반환합니다.
 */
export function randomCharacter(): CharacterKey {
  const idx = Math.floor(Math.random() * CHARACTERS.length);
  return CHARACTERS[idx].key;
}

export interface GrowthInfo {
  stage: 1 | 2 | 3;
  emoji: string;
  label: string;
  toNext: number | null;
}

/**
 * 만난 사람 수(metCount)에 따른 3단계 성장 정보(새싹 -> 잎 -> 꽃)를 계산합니다.
 */
export function growthOf(metCount: number): GrowthInfo {
  if (metCount >= 3) {
    return { stage: 3, emoji: '🌸', label: '꽃', toNext: null };
  }
  if (metCount >= 1) {
    return { stage: 2, emoji: '🌿', label: '잎', toNext: 3 - metCount };
  }
  return { stage: 1, emoji: '🌱', label: '새싹', toNext: 1 };
}

/**
 * 캐릭터 key 또는 fallbackSeed(tagCode 등) 기반으로 캐릭터 객체를 결정론적으로 반환합니다.
 */
export function characterOf(key?: string, fallbackSeed?: string): CharacterItem {
  const found = CHARACTERS.find((c) => c.key === key);
  if (found) {
    return found;
  }
  if (!fallbackSeed) {
    return CHARACTERS[0];
  }
  let hash = 0;
  for (let i = 0; i < fallbackSeed.length; i++) {
    hash = (hash << 5) - hash + fallbackSeed.charCodeAt(i);
    hash |= 0;
  }
  const idx = Math.abs(hash) % CHARACTERS.length;
  return CHARACTERS[idx];
}

/**
 * 두 사람의 관심사 목록에서 공통 관심사(교집합)를 반환합니다.
 */
export function commonInterests(mine: string[] = [], theirs: string[] = []): string[] {
  const set = new Set(theirs);
  return mine.filter((item) => set.has(item));
}
