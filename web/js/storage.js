// 고양이 상태와 수업 기록 저장소. 안드로이드 앱의 CatRepository 와 같은 규칙을 따른다.
// 경험치는 따로 저장하지 않고 수업 기록에서 계산하므로, 기록을 지우면 경험치도 함께 줄어든다.
// 데이터는 이 기기 브라우저(localStorage)에만 저장되고 안드로이드 앱과 동기화되지 않는다.

import { isSkinUnlocked, levelForXp, skinById, totalXp } from './domain.js';

const KEY = 'classcat.v1';
export const DEFAULT_CAT_NAME = '냥이';
export const DEFAULT_DAILY_GOAL = 120;
export const MIN_SESSION_MS = 60_000;

function defaults() {
  return {
    catName: DEFAULT_CAT_NAME,
    skin: 'cheese',
    dailyGoal: DEFAULT_DAILY_GOAL,
    sessions: [], // 최신 기록이 앞
    activeStart: 0,
    activeSubject: '',
  };
}

function isValidSession(s) {
  return s && Number.isFinite(s.start) && Number.isFinite(s.end) && s.end >= s.start;
}

/** 저장된 JSON 을 검사해서 상태로 만든다. 이상한 값은 기본값으로 바꾼다. */
export function normalize(raw) {
  const state = defaults();
  if (!raw || typeof raw !== 'object') return state;
  if (typeof raw.catName === 'string' && raw.catName.trim()) state.catName = raw.catName.trim().slice(0, 12);
  if (typeof raw.skin === 'string') state.skin = skinById(raw.skin).id;
  if (Number.isFinite(raw.dailyGoal) && raw.dailyGoal > 0) state.dailyGoal = raw.dailyGoal;
  if (Array.isArray(raw.sessions)) {
    state.sessions = raw.sessions
      .filter(isValidSession)
      .map((s) => ({ start: s.start, end: s.end, subject: typeof s.subject === 'string' ? s.subject : '' }))
      .sort((a, b) => b.start - a.start);
  }
  if (Number.isFinite(raw.activeStart) && raw.activeStart > 0) {
    state.activeStart = raw.activeStart;
    state.activeSubject = typeof raw.activeSubject === 'string' ? raw.activeSubject : '';
  }
  return state;
}

export class Store {
  constructor(storage = globalThis.localStorage) {
    this.storage = storage;
    this.state = this.load();
  }

  load() {
    try {
      const json = this.storage && this.storage.getItem(KEY);
      return normalize(json ? JSON.parse(json) : null);
    } catch (e) {
      console.error('저장된 데이터를 읽지 못했어요', e);
      return defaults();
    }
  }

  save() {
    try {
      this.storage && this.storage.setItem(KEY, JSON.stringify(this.state));
    } catch (e) {
      console.error('데이터를 저장하지 못했어요', e);
    }
  }

  // ---- 고양이 ----

  get catName() {
    return this.state.catName;
  }

  setCatName(name) {
    const trimmed = String(name || '').trim().slice(0, 12);
    if (!trimmed) return;
    this.state.catName = trimmed;
    this.save();
  }

  get totalXp() {
    return totalXp(this.state.sessions);
  }

  get level() {
    return levelForXp(this.totalXp);
  }

  /** 선택한 털 색. 기록 삭제 등으로 잠긴 색이 되면 기본 색으로 돌아간다. */
  get skin() {
    const skin = skinById(this.state.skin);
    return isSkinUnlocked(skin, this.level) ? skin : skinById('cheese');
  }

  setSkin(id) {
    this.state.skin = skinById(id).id;
    this.save();
  }

  // ---- 수업 ----

  get sessions() {
    return this.state.sessions;
  }

  get isActive() {
    return this.state.activeStart > 0;
  }

  get activeStart() {
    return this.state.activeStart;
  }

  get activeSubject() {
    return this.state.activeSubject;
  }

  startSession(now, subject) {
    this.state.activeStart = now;
    this.state.activeSubject = String(subject || '').trim().slice(0, 30);
    this.save();
  }

  /** 진행 중인 수업을 끝내고 기록한다. 1분 미만이면 저장하지 않고 null. */
  finishSession(now) {
    const start = this.state.activeStart;
    if (!(start > 0)) return null;
    const subject = this.state.activeSubject;
    this.state.activeStart = 0;
    this.state.activeSubject = '';
    let session = null;
    if (now - start >= MIN_SESSION_MS) {
      session = { start, end: now, subject };
      this.state.sessions.unshift(session);
    }
    this.save();
    return session;
  }

  cancelSession() {
    this.state.activeStart = 0;
    this.state.activeSubject = '';
    this.save();
  }

  deleteSession(start) {
    this.state.sessions = this.state.sessions.filter((s) => s.start !== start);
    this.save();
  }

  /** 수업 기록과 진행 중인 수업을 모두 지운다. 이름과 설정은 유지한다. */
  clearRecords() {
    this.state.sessions = [];
    this.state.activeStart = 0;
    this.state.activeSubject = '';
    this.save();
  }

  // ---- 설정 ----

  get dailyGoal() {
    return this.state.dailyGoal;
  }

  setDailyGoal(minutes) {
    this.state.dailyGoal = minutes;
    this.save();
  }

  // ---- 백업 ----

  exportJson() {
    return JSON.stringify({ app: 'classcat', version: 1, exportedAt: new Date().toISOString(), ...this.state }, null, 2);
  }

  /** 백업 파일 내용으로 바꾼다. 형식이 틀리면 예외. */
  importJson(text) {
    const raw = JSON.parse(text);
    if (!raw || raw.app !== 'classcat' || !Array.isArray(raw.sessions)) {
      throw new Error('클래스 냥이 백업 파일이 아니에요');
    }
    this.state = normalize(raw);
    this.save();
  }
}
