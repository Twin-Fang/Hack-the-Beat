import test from 'node:test';
import assert from 'node:assert/strict';
import { usePassportStore } from './usePassportStore.ts';

test('usePassportStore - saveSession 및 getSession 캐릭터 포함 테스트', () => {
  const store = usePassportStore.getState();

  store.saveSession({
    partyCode: 'TST123',
    participantId: 'p-1',
    tagCode: '7K2M',
    name: '서준',
    isHost: true,
    character: 'FOX',
  });

  const session = usePassportStore.getState().getSession('tst123');
  assert.ok(session);
  assert.equal(session.partyCode, 'TST123');
  assert.equal(session.name, '서준');
  assert.equal(session.character, 'FOX');
  assert.equal(session.isHost, true);
});

test('usePassportStore - accumulateBadges 캐릭터 포함 누적 테스트', () => {
  const store = usePassportStore.getState();

  store.accumulateBadges(
    '여름 파티',
    [
      {
        code: 'FIRST_MEET',
        title: '첫 만남',
        description: '처음으로 대화했습니다',
        achieved: true,
      },
      {
        code: 'PARTY_MAKER',
        title: '분위기 메이커',
        description: '3명 이상 만났습니다',
        achieved: false,
      },
    ],
    'FROG'
  );

  const badges = usePassportStore.getState().savedBadges;
  const saved = badges.find((b) => b.code === 'FIRST_MEET' && b.partyName === '여름 파티');
  assert.ok(saved);
  assert.equal(saved.title, '첫 만남');
  assert.equal(saved.character, 'FROG');
});
