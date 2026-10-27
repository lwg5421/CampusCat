import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Store, normalize } from '../js/storage.js';

class MemoryStorage {
  constructor() { this.map = new Map(); }
  getItem(key) { return this.map.has(key) ? this.map.get(key) : null; }
  setItem(key, value) { this.map.set(key, String(value)); }
}

const MINUTE = 60_000;
const T = Date.UTC(2026, 9, 27, 0, 0); // 실제와 비슷한 시각 (0 은 '진행 중인 수업 없음' 을 뜻한다)

test('수업을 끝내면 과목과 함께 기록되고 다시 읽어도 남아 있다', () => {
  const storage = new MemoryStorage();
  const store = new Store(storage);
  store.startSession(1_000_000, '  자료구조 ');
  assert.equal(store.isActive, true);
  const saved = store.finishSession(1_000_000 + 90 * MINUTE);
  assert.deepEqual(saved, { start: 1_000_000, end: 1_000_000 + 90 * MINUTE, subject: '자료구조' });
  assert.equal(store.isActive, false);

  const reloaded = new Store(storage);
  assert.equal(reloaded.sessions.length, 1);
  assert.equal(reloaded.sessions[0].subject, '자료구조');
  assert.equal(reloaded.totalXp, 90);
  assert.equal(reloaded.level, 2);
});

test('1분 미만 수업은 저장하지 않는다', () => {
  const store = new Store(new MemoryStorage());
  store.startSession(T, '');
  assert.equal(store.isActive, true);
  assert.equal(store.finishSession(T + 59_999), null);
  assert.equal(store.sessions.length, 0);
  assert.equal(store.isActive, false);
  assert.equal(store.finishSession(T + 10 * MINUTE), null, '진행 중인 수업이 없으면 null');
});

test('잠긴 털 색은 기본 색으로 돌아간다', () => {
  const store = new Store(new MemoryStorage());
  store.setSkin('black');
  assert.equal(store.skin.id, 'cheese');
  store.startSession(T, '');
  store.finishSession(T + 900 * MINUTE); // 900 XP = Lv.6
  assert.equal(store.level, 6);
  assert.equal(store.skin.id, 'black');
  store.deleteSession(T);
  assert.equal(store.skin.id, 'cheese');
});

test('기록 초기화는 이름과 목표를 유지한다', () => {
  const store = new Store(new MemoryStorage());
  store.setCatName('나비');
  store.setDailyGoal(180);
  store.startSession(T, '');
  store.finishSession(T + 30 * MINUTE);
  store.startSession(T + 40 * MINUTE, '');
  assert.equal(store.isActive, true);
  store.clearRecords();
  assert.equal(store.sessions.length, 0);
  assert.equal(store.isActive, false);
  assert.equal(store.catName, '나비');
  assert.equal(store.dailyGoal, 180);
});

test('이름은 공백을 지우고 12자까지', () => {
  const store = new Store(new MemoryStorage());
  store.setCatName('   ');
  assert.equal(store.catName, '냥이');
  store.setCatName(' 아주아주아주아주긴이름입니다 ');
  assert.equal(store.catName, '아주아주아주아주긴이름입');
});

test('백업을 내보내고 다시 가져올 수 있다', () => {
  const a = new Store(new MemoryStorage());
  a.setCatName('치즈');
  a.startSession(T, '영어');
  a.finishSession(T + 45 * MINUTE);
  assert.equal(a.sessions.length, 1);
  const b = new Store(new MemoryStorage());
  b.importJson(a.exportJson());
  assert.equal(b.catName, '치즈');
  assert.deepEqual(b.sessions, a.sessions);
  assert.throws(() => b.importJson('{"hello": 1}'));
  assert.throws(() => b.importJson('not json'));
  assert.equal(b.catName, '치즈', '실패한 가져오기는 데이터를 바꾸지 않는다');
});

test('깨진 데이터는 걸러서 읽는다', () => {
  const state = normalize({
    catName: 42,
    skin: 'rainbow',
    dailyGoal: -10,
    sessions: [{ start: 5, end: 1 }, { start: 'x', end: 2 }, { start: 1, end: 120_001 }, null],
    activeStart: 'soon',
  });
  assert.equal(state.catName, '냥이');
  assert.equal(state.skin, 'cheese');
  assert.equal(state.dailyGoal, 120);
  assert.deepEqual(state.sessions, [{ start: 1, end: 120_001, subject: '' }]);
  assert.equal(state.activeStart, 0);

  const storage = new MemoryStorage();
  storage.setItem('classcat.v1', '{not json');
  assert.equal(new Store(storage).sessions.length, 0);
});
