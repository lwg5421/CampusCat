// 안드로이드 앱의 LevelSystemTest / StudyStatsTest / TimeFormatterTest 와 같은 경우를 검사한다.
// 날짜 계산은 기기 시간대를 따르므로 TZ=Asia/Seoul 로 실행한다 (package.json 의 test 스크립트).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import * as d from '../js/domain.js';

const MINUTE = 60_000;
const at = (month, day, hour, minute) => new Date(2026, month - 1, day, hour, minute, 0).getTime();
const session = (start, minutes) => ({ start, end: start + minutes * MINUTE, subject: '' });

test('레벨 기준 XP는 레벨마다 1시간씩 늘어난다', () => {
  assert.equal(d.totalXpForLevel(1), 0);
  assert.equal(d.totalXpForLevel(2), 60);
  assert.equal(d.totalXpForLevel(3), 180);
  assert.equal(d.totalXpForLevel(4), 360);
  assert.equal(d.totalXpForLevel(10), 2700);
});

test('levelForXp 는 기준값을 포함한다', () => {
  assert.equal(d.levelForXp(0), 1);
  assert.equal(d.levelForXp(59), 1);
  assert.equal(d.levelForXp(60), 2);
  assert.equal(d.levelForXp(179), 2);
  assert.equal(d.levelForXp(180), 3);
});

test('레벨 안에서의 진행도', () => {
  assert.equal(d.xpIntoLevel(90), 30);
  assert.equal(d.xpNeededForNextLevel(2), 120);
  assert.equal(d.xpRemaining(90), 90);
});

test('최대 레벨에서 멈춘다', () => {
  const huge = d.totalXpForLevel(d.MAX_LEVEL) + 100_000;
  assert.equal(d.levelForXp(huge), d.MAX_LEVEL);
  assert.equal(d.xpRemaining(huge), 0);
  assert.equal(d.xpIntoLevel(huge), d.xpNeededForNextLevel(d.MAX_LEVEL));
  assert.equal(d.isMaxLevel(d.MAX_LEVEL - 1), false);
});

test('음수 분은 XP 0', () => {
  assert.equal(d.xpForMinutes(-5), 0);
  assert.equal(d.xpForMinutes(45), 45);
});

test('진화 단계와 스킨 해금', () => {
  const ids = [1, 2, 3, 9, 10, d.MAX_LEVEL].map((l) => d.stageForLevel(l).id);
  assert.deepEqual(ids, ['baby', 'baby', 'freshman', 'honor', 'scholar', 'doctor']);
  assert.equal(d.nextStage(d.STAGES[0]).id, 'freshman');
  assert.equal(d.nextStage(d.STAGES[4]), null);
  assert.equal(d.isSkinUnlocked(d.skinById('black'), 5), false);
  assert.equal(d.isSkinUnlocked(d.skinById('black'), 6), true);
  assert.equal(d.skinById('unknown').id, 'cheese');
  assert.equal(d.skinById(undefined).id, 'cheese');
});

test('수업 길이와 XP는 분 단위로 내림', () => {
  const s = { start: at(10, 27, 9, 0), end: at(10, 27, 10, 15) + 59_000, subject: '자료구조' };
  assert.equal(d.sessionMinutes(s), 75);
  assert.equal(d.sessionXp(s), 75);
  assert.equal(d.sessionMinutes({ start: at(10, 27, 9, 0), end: at(10, 27, 8, 0) }), 0);
});

test('자정을 넘긴 수업은 날짜별로 나뉜다', () => {
  const sessions = [session(at(10, 26, 23, 30), 90)];
  assert.equal(d.minutesOnDay(sessions, at(10, 26, 12, 0)), 30);
  assert.equal(d.minutesOnDay(sessions, at(10, 27, 12, 0)), 60);
  assert.equal(d.totalMinutes(sessions), 90);
});

test('dailyMinutes 의 마지막은 오늘', () => {
  const sessions = [
    session(at(10, 27, 9, 0), 50),
    session(at(10, 27, 13, 0), 70),
    session(at(10, 25, 10, 0), 30),
    session(at(10, 18, 10, 0), 999),
  ];
  assert.deepEqual(d.dailyMinutes(sessions, at(10, 27, 20, 0), 7), [0, 0, 0, 0, 30, 0, 120]);
});

test('연속 공부 일수', () => {
  const sessions = [27, 26, 25, 23].map((day) => session(at(10, day, 9, 0), 10));
  assert.equal(d.streakDays(sessions, at(10, 27, 22, 0)), 3);
  const untilYesterday = [26, 25].map((day) => session(at(10, day, 9, 0), 10));
  assert.equal(d.streakDays(untilYesterday, at(10, 27, 8, 0)), 2);
  assert.equal(d.streakDays(untilYesterday, at(10, 28, 8, 0)), 0);
  assert.equal(d.streakDays([], at(10, 27, 8, 0)), 0);
});

test('총 XP는 기록마다 분을 내림해서 더한다', () => {
  const sessions = [{ start: 0, end: 90_000 }, { start: 0, end: 90_000 }];
  assert.equal(d.totalXp(sessions), 2);
  assert.equal(d.totalMinutes(sessions), 3);
  assert.equal(d.totalXp([]), 0);
});

test('시간 문자열', () => {
  assert.equal(d.formatDuration(0), '0분');
  assert.equal(d.formatDuration(45), '45분');
  assert.equal(d.formatDuration(60), '1시간');
  assert.equal(d.formatDuration(90), '1시간 30분');
  assert.equal(d.formatDuration(-3), '0분');
  assert.equal(d.formatClock(0), '00:00:00');
  assert.equal(d.formatClock(65_999), '00:01:05');
  assert.equal(d.formatClock((2 * 3600 + 3 * 60 + 4) * 1000), '02:03:04');
  assert.equal(d.formatClock(100 * 3600 * 1000), '100:00:00');
  assert.equal(d.formatCompact(0), '');
  assert.equal(d.formatCompact(45), '45m');
  assert.equal(d.formatCompact(90), '1.5h');
  assert.equal(d.formatCompact(120), '2h');
  assert.equal(d.formatCompact(62), '1h');
  assert.equal(d.formatHourMinute(at(10, 27, 9, 5)), '09:05');
});
