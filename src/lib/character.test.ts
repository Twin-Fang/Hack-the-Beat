import test from 'node:test';
import assert from 'node:assert/strict';
import {
  CHARACTERS,
  INTERESTS,
  LEVELS,
  growthOf,
  characterOf,
  commonInterests,
  randomCharacter,
} from './character.ts';

test('CHARACTERS 8종 상수 정의 확인', () => {
  assert.equal(CHARACTERS.length, 8);
  const keys = CHARACTERS.map((c) => c.key);
  assert.deepEqual(keys, [
    'FOX',
    'FROG',
    'PANDA',
    'CHICK',
    'OCTOPUS',
    'LION',
    'RABBIT',
    'KOALA',
  ]);
  assert.ok(CHARACTERS.some((c) => c.key === 'FOX' && c.emoji === '🦊' && c.name === '여우'));
  assert.ok(CHARACTERS.some((c) => c.key === 'FROG' && c.emoji === '🐸' && c.name === '개구리'));
  assert.ok(CHARACTERS.some((c) => c.key === 'PANDA' && c.emoji === '🐼' && c.name === '판다'));
  assert.ok(CHARACTERS.some((c) => c.key === 'CHICK' && c.emoji === '🐥' && c.name === '병아리'));
  assert.ok(CHARACTERS.some((c) => c.key === 'OCTOPUS' && c.emoji === '🐙' && c.name === '문어'));
  assert.ok(CHARACTERS.some((c) => c.key === 'LION' && c.emoji === '🦁' && c.name === '사자'));
  assert.ok(CHARACTERS.some((c) => c.key === 'RABBIT' && c.emoji === '🐰' && c.name === '토끼'));
  assert.ok(CHARACTERS.some((c) => c.key === 'KOALA' && c.emoji === '🐨' && c.name === '코알라'));
});

test('INTERESTS 12종 상수 정의 확인', () => {
  assert.equal(INTERESTS.length, 12);
  assert.deepEqual(INTERESTS, [
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
  ]);
});

test('LEVELS 3종 상수 정의 확인', () => {
  assert.equal(LEVELS.length, 3);
  assert.deepEqual(
    LEVELS.map((l) => ({ level: l.level, label: l.label, icon: l.icon })),
    [
      { level: 1, label: '가볍게', icon: '☕' },
      { level: 2, label: '반가웠어요', icon: '🙌' },
      { level: 3, label: '꼭 다시', icon: '💫' },
    ]
  );
});

test('randomCharacter 반환값 유효성 테스트', () => {
  const validKeys = CHARACTERS.map((c) => c.key);
  for (let i = 0; i < 20; i++) {
    const key = randomCharacter();
    assert.ok(validKeys.includes(key));
  }
});

test('growthOf 경계값 테스트', () => {
  // 0명 -> 새싹 (stage 1)
  const g0 = growthOf(0);
  assert.equal(g0.stage, 1);
  assert.equal(g0.emoji, '🌱');
  assert.equal(g0.label, '새싹');
  assert.equal(g0.toNext, 1);

  // 음수 -> stage 1
  const gNegative = growthOf(-1);
  assert.equal(gNegative.stage, 1);
  assert.equal(gNegative.emoji, '🌱');
  assert.equal(gNegative.label, '새싹');
  assert.equal(gNegative.toNext, 1);

  // 1명 -> 잎 (stage 2, toNext: 2)
  const g1 = growthOf(1);
  assert.equal(g1.stage, 2);
  assert.equal(g1.emoji, '🌿');
  assert.equal(g1.label, '잎');
  assert.equal(g1.toNext, 2);

  // 2명 -> 잎 (stage 2, toNext: 1)
  const g2 = growthOf(2);
  assert.equal(g2.stage, 2);
  assert.equal(g2.toNext, 1);

  // 3명 -> 꽃 (stage 3, toNext: null)
  const g3 = growthOf(3);
  assert.equal(g3.stage, 3);
  assert.equal(g3.emoji, '🌸');
  assert.equal(g3.label, '꽃');
  assert.equal(g3.toNext, null);

  // 10명 -> 꽃 (stage 3, toNext: null)
  const g10 = growthOf(10);
  assert.equal(g10.stage, 3);
  assert.equal(g10.emoji, '🌸');
  assert.equal(g10.label, '꽃');
  assert.equal(g10.toNext, null);
});

test('characterOf 명시적 키 및 결정론적 해시 폴백 테스트', () => {
  // 유효한 key
  const explicit = characterOf('FROG');
  assert.equal(explicit.key, 'FROG');
  assert.equal(explicit.emoji, '🐸');
  assert.equal(explicit.name, '개구리');

  // fallbackSeed 해시 결정론 테스트
  const c1 = characterOf(undefined, '7K9M');
  const c2 = characterOf(undefined, '7K9M');
  assert.equal(c1.key, c2.key);
  assert.ok(c1.emoji);
  assert.ok(c1.name);

  // 무효한 key + fallbackSeed
  const invalidKey = characterOf('INVALID_KEY', 'AB12');
  const expectedForSeed = characterOf(undefined, 'AB12');
  assert.equal(invalidKey.key, expectedForSeed.key);

  // key 없음 + seed 없음
  const fallbackDefault = characterOf(undefined, undefined);
  assert.equal(fallbackDefault.key, CHARACTERS[0].key);
});

test('commonInterests 교집합 및 기본값 테스트', () => {
  const common = commonInterests(['러닝', '음악', '게임'], ['영화', '러닝', '게임']);
  assert.deepEqual(common, ['러닝', '게임']);

  // 빈 배열 및 undefined
  assert.deepEqual(commonInterests([], ['러닝']), []);
  assert.deepEqual(commonInterests(['러닝'], []), []);
  assert.deepEqual(commonInterests(undefined, undefined), []);
  assert.deepEqual(commonInterests(['음악'], undefined), []);
});
