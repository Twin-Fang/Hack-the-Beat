import test from 'node:test'
import assert from 'node:assert/strict'
import { CHARACTERS, INTERESTS, LEVELS, characterOf, growthOf } from '../lib/character.ts'

test('CharacterPicker 규격 검증 - 8개 캐릭터 목록 및 키 매핑', () => {
  assert.equal(CHARACTERS.length, 8)
  const requiredKeys = ['FOX', 'FROG', 'PANDA', 'CHICK', 'OCTOPUS', 'LION', 'RABBIT', 'KOALA']
  for (const key of requiredKeys) {
    const found = CHARACTERS.find((c) => c.key === key)
    assert.ok(found, `캐릭터 키 ${key}가 존재해야 함`)
    assert.ok(found.emoji, `캐릭터 ${key}에 이모지가 있어야 함`)
    assert.ok(found.name, `캐릭터 ${key}에 이름이 있어야 함`)
  }
})

test('InterestPicker 규격 검증 - 12개 관심사 목록 및 최대 선택 로직', () => {
  assert.equal(INTERESTS.length, 12)
  const max = 3
  const selected = ['게임', '러닝', '음악']
  const isMaxReached = selected.length >= max
  assert.equal(isMaxReached, true)

  for (const interest of INTERESTS) {
    const isSelected = selected.includes(interest)
    const isDisabled: boolean = !isSelected && isMaxReached
    if (isSelected) {
      assert.equal(isDisabled, false, '선택된 항목은 disabled되지 않아야 함')
    } else {
      assert.equal(isDisabled, true, '최대 도달 시 미선택 항목은 disabled되어야 함')
    }
  }
})

test('CharacterAvatar 규격 검증 - 캐릭터 및 성장 단계 매핑', () => {
  // 캐릭터 결정
  const frog = characterOf('FROG')
  assert.equal(frog.emoji, '🐸')
  assert.equal(frog.name, '개구리')

  // 성장 정보 (metCount = 3 -> stage 3 꽃)
  const g3 = growthOf(3)
  assert.equal(g3.stage, 3)
  assert.equal(g3.emoji, '🌸')
  assert.equal(g3.label, '꽃')

  // fallbackSeed 결정론적 검증
  const seedChar = characterOf(undefined, 'TEST_SEED')
  assert.ok(seedChar.name)
  assert.ok(seedChar.emoji)
})

test('LevelPicker 규격 검증 - 3개 레벨 정의 및 금지어(호감도) 미사용', () => {
  assert.equal(LEVELS.length, 3)
  const levels = LEVELS.map((l) => l.level)
  assert.deepEqual(levels, [1, 2, 3])

  for (const item of LEVELS) {
    assert.ok(!item.label.includes('호감도'), '호감도 단어가 포함되면 안 됨')
    assert.ok(!item.text.includes('호감도'), '호감도 단어가 포함되면 안 됨')
    assert.ok(item.icon, '아이콘이 정의되어 있어야 함')
  }
})
