// 레벨·진화·스킨·통계 규칙. 안드로이드 앱의 domain 패키지(LevelSystem, CatStage, CatSkin,
// StudyStats, TimeFormatter)와 같은 규칙이므로 한쪽을 바꾸면 다른 쪽도 같이 바꿔야 한다.
// DOM 을 쓰지 않아서 Node 에서 그대로 테스트할 수 있다.

export const XP_PER_MINUTE = 1;
export const MAX_LEVEL = 30;
const XP_STEP = 60;
const MINUTE = 60_000;

// ---- 레벨 ----

const clampLevel = (level) => Math.max(1, Math.min(level, MAX_LEVEL));

/** 해당 레벨에 도달하기 위한 누적 XP. Lv.1 은 0 XP, 레벨 L → L+1 에 60 × L XP. */
export function totalXpForLevel(level) {
  const l = clampLevel(level);
  return (XP_STEP * l * (l - 1)) / 2;
}

export function levelForXp(totalXp) {
  let level = 1;
  while (level < MAX_LEVEL && totalXp >= totalXpForLevel(level + 1)) level++;
  return level;
}

export const isMaxLevel = (level) => level >= MAX_LEVEL;

export const xpNeededForNextLevel = (level) => XP_STEP * clampLevel(level);

export function xpIntoLevel(totalXp) {
  const level = levelForXp(totalXp);
  if (isMaxLevel(level)) return xpNeededForNextLevel(level);
  return Math.max(0, totalXp - totalXpForLevel(level));
}

export function xpRemaining(totalXp) {
  const level = levelForXp(totalXp);
  return isMaxLevel(level) ? 0 : totalXpForLevel(level + 1) - totalXp;
}

export const xpForMinutes = (minutes) => Math.max(0, Math.floor(minutes)) * XP_PER_MINUTE;

// ---- 진화 단계 ----

export const STAGES = [
  { id: 'baby', minLevel: 1, name: '아기 냥이', accessory: null,
    description: '이제 막 학교에 따라온 아기 고양이. 수업 시간엔 주로 잠을 자요.' },
  { id: 'freshman', minLevel: 3, name: '새내기 냥이', accessory: 'bow',
    description: '리본을 달고 캠퍼스를 탐험하는 새내기. 강의실 위치를 외우는 중!' },
  { id: 'honor', minLevel: 6, name: '우등생 냥이', accessory: 'glasses',
    description: '안경을 쓰고 맨 앞자리에 앉는 우등생. 필기가 아주 꼼꼼해요.' },
  { id: 'scholar', minLevel: 10, name: '장학생 냥이', accessory: 'scholar',
    description: '별 핀을 단 장학생. 교수님도 이름을 기억하는 고양이예요.' },
  { id: 'doctor', minLevel: 15, name: '박사 냥이', accessory: 'doctor',
    description: '학사모를 쓴 박사 고양이. 이제 수업을 들으러 오는 게 아니라 하러 와요.' },
];

export function stageForLevel(level) {
  let result = STAGES[0];
  for (const stage of STAGES) if (level >= stage.minLevel) result = stage;
  return result;
}

export function nextStage(stage) {
  const index = STAGES.indexOf(stage);
  return index >= 0 && index + 1 < STAGES.length ? STAGES[index + 1] : null;
}

// ---- 털 색 ----

export const SKINS = [
  { id: 'cheese', name: '치즈냥', fur: '#F2A65A', striped: true, unlockLevel: 1 },
  { id: 'white', name: '흰냥', fur: '#FBF7F2', striped: false, unlockLevel: 1 },
  { id: 'mackerel', name: '고등어냥', fur: '#A4A9AE', striped: true, unlockLevel: 3 },
  { id: 'black', name: '까망냥', fur: '#45403C', striped: false, unlockLevel: 6 },
  { id: 'cream', name: '크림냥', fur: '#F1D7B0', striped: true, unlockLevel: 10 },
  { id: 'blue', name: '러블냥', fur: '#8E9DB3', striped: false, unlockLevel: 15 },
];

export const skinById = (id) => SKINS.find((skin) => skin.id === id) || SKINS[0];
export const isSkinUnlocked = (skin, level) => level >= skin.unlockLevel;

// ---- 수업 기록 ----
// 기록은 { start, end, subject } (밀리초). start 가 기록의 고유 id 다.

export const sessionMinutes = (s) => Math.floor(Math.max(0, s.end - s.start) / MINUTE);
export const sessionXp = (s) => xpForMinutes(sessionMinutes(s));

export function startOfDay(ms) {
  const d = new Date(ms);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

/** dayStart 에서 days 일 이동한 날의 시작 시각 (기기 시간대 기준). */
export function addDays(dayStart, days) {
  const d = new Date(dayStart);
  d.setDate(d.getDate() + days);
  return startOfDay(d.getTime());
}

/** [from, to) 구간과 겹치는 수업 시간의 합(분). 자정을 넘긴 수업은 날짜별로 나뉜다. */
export function minutesInRange(sessions, from, to) {
  let total = 0;
  for (const s of sessions) {
    const start = Math.max(s.start, from);
    const end = Math.min(s.end, to);
    if (end > start) total += end - start;
  }
  return Math.floor(total / MINUTE);
}

export function minutesOnDay(sessions, anyMsOfDay) {
  const day = startOfDay(anyMsOfDay);
  return minutesInRange(sessions, day, addDays(day, 1));
}

/** 오늘을 포함한 최근 days 일의 날짜별 수업 시간(분). 마지막 원소가 오늘. */
export function dailyMinutes(sessions, now, days) {
  const today = startOfDay(now);
  const result = [];
  for (let i = 0; i < days; i++) {
    const day = addDays(today, i - (days - 1));
    result.push(minutesInRange(sessions, day, addDays(day, 1)));
  }
  return result;
}

export function totalMinutes(sessions) {
  let total = 0;
  for (const s of sessions) total += Math.max(0, s.end - s.start);
  return Math.floor(total / MINUTE);
}

export const totalXp = (sessions) => sessions.reduce((sum, s) => sum + sessionXp(s), 0);

/** 연속으로 공부한 날 수. 오늘 아직 기록이 없으면 어제부터 센다. */
export function streakDays(sessions, now) {
  let day = startOfDay(now);
  if (minutesInRange(sessions, day, addDays(day, 1)) === 0) day = addDays(day, -1);
  let streak = 0;
  while (minutesInRange(sessions, day, addDays(day, 1)) > 0) {
    streak++;
    day = addDays(day, -1);
  }
  return streak;
}

// ---- 표시용 문자열 ----

/** 90 → "1시간 30분", 60 → "1시간", 5 → "5분". */
export function formatDuration(minutes) {
  const safe = Math.max(0, Math.floor(minutes));
  const hours = Math.floor(safe / 60);
  const rest = safe % 60;
  if (hours === 0) return `${rest}분`;
  if (rest === 0) return `${hours}시간`;
  return `${hours}시간 ${rest}분`;
}

const pad2 = (n) => String(n).padStart(2, '0');

/** 경과 시간을 "HH:MM:SS" 로. */
export function formatClock(elapsedMs) {
  const totalSeconds = Math.floor(Math.max(0, elapsedMs) / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  return `${pad2(hours)}:${pad2(minutes)}:${pad2(totalSeconds % 60)}`;
}

/** 차트 라벨용: 0 → "", 45 → "45m", 90 → "1.5h", 120 → "2h". */
export function formatCompact(minutes) {
  if (minutes <= 0) return '';
  if (minutes < 60) return `${minutes}m`;
  const tenths = Math.round(minutes / 6);
  return tenths % 10 === 0 ? `${tenths / 10}h` : `${(tenths / 10).toFixed(1)}h`;
}

/** 타임스탬프를 "HH:MM" 으로. */
export const formatHourMinute = (ms) => {
  const d = new Date(ms);
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
};
