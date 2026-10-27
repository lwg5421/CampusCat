// 클래스 냥이 웹앱 (아이폰용 PWA). 화면 구성과 동작은 안드로이드 앱과 같다.

import * as d from './domain.js';
import { Store } from './storage.js';
import { bounce, catSvg } from './cat.js';
import { $, escapeHtml, pick } from './util.js';

const APP_VERSION = '1.0 (웹)';
/** 이보다 긴 수업은 종료 버튼을 깜빡한 것일 수 있어 한 번 더 확인한다. */
const LONG_SESSION_MS = 6 * 60 * 60 * 1000;
const BUBBLE_HOLD_MS = 3000;
const MEOW_VISIBLE_MS = 2200;
const DAY_NAMES = '일월화수목금토';
const TITLES = { home: '클래스 냥이', records: '수업 기록', cat: '내 고양이', settings: '설정' };
const MEOWS = [
  '냥!',
  '오늘 수업 재밌었냥?',
  '졸리다냥… 5분만…',
  '간식은 공부 끝나고 먹자냥',
  '과제는 미리미리 하자냥!',
  '골골골골…',
  '출석 체크 했냥?',
  '같이 있어서 좋다냥 💛',
];
const INSTALL_HINT_KEY = 'classcat.installHintDismissed';

const store = new Store();
let currentTab = 'home';
let ticker = 0;
let bubbleHoldUntil = 0;
let meowTimer = 0;
let toastTimer = 0;

// ---- 공통 ----

function setProgress(bar, value, max) {
  const percent = max > 0 ? Math.max(0, Math.min(100, (value / max) * 100)) : 0;
  bar.style.width = `${percent}%`;
  bar.parentElement.setAttribute('aria-valuenow', String(Math.round(percent)));
}

function toast(text) {
  const el = $('toast');
  el.textContent = text;
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.hidden = true; }, 2500);
}

/**
 * 대화상자를 띄우고 누른 버튼의 id 를 돌려준다. 바깥을 누르거나 ESC 면 'cancel'.
 * actions 의 마지막 항목이 기본 동작(입력창에서 엔터)이다.
 */
function openDialog({ title, message = '', input = null, actions }) {
  const dialog = $('dialog');
  const primary = actions[actions.length - 1].id;

  if (typeof dialog.showModal !== 'function') {
    // <dialog> 를 지원하지 않는 오래된 Safari (iOS 15.3 이하)
    if (input) {
      const value = window.prompt(title, input.value || '');
      return Promise.resolve({ action: value === null ? 'cancel' : primary, value: value || '' });
    }
    const ok = actions.length === 1 || window.confirm(`${title}\n\n${message}`);
    return Promise.resolve({ action: ok ? primary : actions[0].id, value: '' });
  }

  $('dialog-title').textContent = title;
  $('dialog-message').textContent = message;
  $('dialog-message').hidden = !message;
  const inputEl = $('dialog-input');
  $('dialog-field').hidden = !input;
  if (input) {
    $('dialog-field-label').textContent = input.label;
    inputEl.value = input.value || '';
    inputEl.maxLength = input.maxLength || 100;
  }
  const box = $('dialog-actions');
  box.textContent = '';
  for (const action of actions) {
    const button = document.createElement('button');
    button.type = 'submit';
    button.value = action.id;
    button.textContent = action.label;
    if (action.danger) button.className = 'danger';
    box.appendChild(button);
  }
  inputEl.onkeydown = (event) => {
    if (event.key === 'Enter' && !event.isComposing) {
      event.preventDefault();
      dialog.close(primary);
    }
  };
  return new Promise((resolve) => {
    dialog.addEventListener('close', () => {
      resolve({ action: dialog.returnValue || 'cancel', value: inputEl.value });
    }, { once: true });
    dialog.returnValue = '';
    dialog.showModal();
  });
}

// ---- 탭 ----

function tabFromHash() {
  const tab = location.hash.slice(1);
  return Object.prototype.hasOwnProperty.call(TITLES, tab) ? tab : 'home';
}

function showTab(tab) {
  currentTab = tab;
  for (const name of Object.keys(TITLES)) $(`view-${name}`).hidden = name !== tab;
  for (const link of document.querySelectorAll('.tab')) {
    if (link.dataset.tab === tab) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  }
  $('title').textContent = TITLES[tab];
  document.title = tab === 'home' ? TITLES.home : `${TITLES[tab]} · 클래스 냥이`;
  render();
  window.scrollTo(0, 0);
}

function render() {
  clearInterval(ticker);
  ticker = 0;
  if (currentTab === 'records') renderRecords();
  else if (currentTab === 'cat') renderCat();
  else if (currentTab === 'settings') renderSettings();
  else renderHome();
}

// ---- 홈 ----

function renderHome() {
  const xp = store.totalXp;
  const level = d.levelForXp(xp);
  const stage = d.stageForLevel(level);

  $('install-hint').hidden = !shouldShowInstallHint();
  $('home-cat').innerHTML = catSvg(stage, store.skin);
  $('home-greeting').textContent = `안녕, ${store.catName}!`;
  $('home-level').textContent = d.isMaxLevel(level)
    ? `Lv.${level} ${stage.name} · 최고 레벨 달성!`
    : `Lv.${level} ${stage.name} · 다음 레벨까지 ${d.xpRemaining(xp)} XP`;
  setProgress($('home-xp-bar'), d.xpIntoLevel(xp), d.xpNeededForNextLevel(level));

  const active = store.isActive;
  const subjectInput = $('home-subject');
  $('home-start').hidden = active;
  $('home-stop').hidden = !active;
  $('home-started').hidden = !active;
  subjectInput.disabled = active;
  if (active) {
    subjectInput.value = store.activeSubject;
    $('home-status').textContent = store.activeSubject ? `수업 중 · ${store.activeSubject}` : '수업 중';
  } else {
    $('home-status').textContent = '수업 대기 중';
  }
  $('home-goal').textContent = `목표 ${d.formatDuration(store.dailyGoal)}`;

  updateLive();
  if (active) ticker = setInterval(updateLive, 1000);
}

/** 1초마다 바뀌는 부분: 타이머, 쌓이는 XP, 오늘 공부 시간. */
function updateLive() {
  const now = Date.now();
  const active = store.isActive;
  let today = d.minutesOnDay(store.sessions, now);
  if (active) {
    const start = store.activeStart;
    const elapsed = Math.max(0, now - start);
    $('home-clock').textContent = d.formatClock(elapsed);
    $('home-started').textContent = `${d.formatHourMinute(start)} 시작 · +${d.xpForMinutes(elapsed / 60_000)} XP 쌓는 중`;
    today += d.minutesOnDay([{ start, end: now }], now);
  } else {
    $('home-clock').textContent = d.formatClock(0);
  }

  const goal = store.dailyGoal;
  const done = today >= goal;
  $('home-today').textContent = d.formatDuration(today);
  setProgress($('home-today-bar'), Math.min(goal, today), goal);
  $('home-today-hint').textContent = done
    ? '오늘 목표를 달성했어요 🎉'
    : `목표까지 ${d.formatDuration(goal - today)} 남았어요`;

  if (now < bubbleHoldUntil) return;
  if (active) $('home-bubble').textContent = '열심히 듣는 중이다냥…';
  else if (done) $('home-bubble').textContent = '오늘 목표 달성! 최고다냥!';
  else $('home-bubble').textContent = '오늘도 같이 공부하자냥!';
}

/** 고양이가 잠깐 동안 하는 말. */
function say(text) {
  $('home-bubble').textContent = text;
  bubbleHoldUntil = Date.now() + BUBBLE_HOLD_MS;
  setTimeout(() => {
    if (currentTab === 'home') updateLive();
  }, BUBBLE_HOLD_MS + 50);
}

function startSession() {
  const input = $('home-subject');
  store.startSession(Date.now(), input.value);
  input.blur();
  renderHome();
  say('수업 시작! 집중하자냥 🐾');
  bounce($('home-cat'));
}

async function stopSession() {
  if (!store.isActive) {
    renderHome();
    return;
  }
  const now = Date.now();
  const elapsed = now - store.activeStart;
  if (elapsed >= LONG_SESSION_MS) {
    const { action } = await openDialog({
      title: '수업이 너무 길어요',
      message: `수업이 ${d.formatDuration(elapsed / 60_000)} 동안 이어지고 있어요. 종료 버튼을 깜빡하셨나요?`,
      actions: [
        { id: 'continue', label: '계속하기' },
        { id: 'discard', label: '기록 버리기', danger: true },
        { id: 'save', label: '그대로 저장' },
      ],
    });
    if (action === 'discard') {
      store.cancelSession();
      renderHome();
      toast('수업 기록을 버렸어요');
      return;
    }
    if (action !== 'save') return;
  }
  finishSession(now);
}

function finishSession(now) {
  const levelBefore = store.level;
  const session = store.finishSession(now);
  $('home-subject').value = '';
  renderHome();
  if (!session) {
    toast('1분 미만의 수업은 기록되지 않아요');
    return;
  }
  const levelAfter = store.level;
  if (levelAfter > levelBefore) {
    showLevelUp(levelBefore, levelAfter);
  } else {
    toast(`+${d.sessionXp(session)} XP! ${d.formatDuration(d.sessionMinutes(session))} 동안 수고했어요`);
  }
}

async function showLevelUp(levelBefore, levelAfter) {
  let message = `${store.catName}의 레벨이 올랐어요! Lv.${levelBefore} → Lv.${levelAfter}`;
  const stageAfter = d.stageForLevel(levelAfter);
  if (stageAfter !== d.stageForLevel(levelBefore)) message += `\n\n${stageAfter.name}(으)로 진화했어요!`;
  const newSkins = d.SKINS
    .filter((skin) => !d.isSkinUnlocked(skin, levelBefore) && d.isSkinUnlocked(skin, levelAfter))
    .map((skin) => skin.name);
  if (newSkins.length) message += `\n새 털 색 해금: ${newSkins.join(', ')}`;
  const { action } = await openDialog({
    title: '레벨 업! 🎉',
    message,
    actions: [{ id: 'close', label: '닫기' }, { id: 'cat', label: '고양이 보러 가기' }],
  });
  if (action === 'cat') location.hash = '#cat';
}

// ---- 홈 화면에 추가 안내 (아이폰 Safari) ----

const isIos = /iphone|ipad|ipod/i.test(navigator.userAgent)
  || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
const isStandalone = window.matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;

function shouldShowInstallHint() {
  if (!isIos || isStandalone) return false;
  try {
    return localStorage.getItem(INSTALL_HINT_KEY) !== '1';
  } catch (e) {
    return true;
  }
}

function dismissInstallHint() {
  try {
    localStorage.setItem(INSTALL_HINT_KEY, '1');
  } catch (e) {
    // 저장이 안 되면 이번에만 숨긴다
  }
  $('install-hint').hidden = true;
}

// ---- 기록 ----

function dayLabels(now) {
  const today = d.startOfDay(now);
  const labels = [];
  for (let i = 0; i < 7; i++) {
    labels.push(i === 6 ? '오늘' : DAY_NAMES[new Date(d.addDays(today, i - 6)).getDay()]);
  }
  return labels;
}

function chartSvg(minutes, labels, goal) {
  const width = 320;
  const top = 16;
  const labelBaseline = 146;
  const bottom = 128;
  const chartHeight = bottom - top;
  const max = Math.max(goal, 60, ...minutes);
  const slot = width / minutes.length;
  const barWidth = Math.min(slot * 0.55, 28);
  const radius = barWidth / 2;
  const parts = [];
  minutes.forEach((value, i) => {
    const today = i === minutes.length - 1;
    const cx = slot * i + slot / 2;
    let height = (chartHeight * value) / max;
    if (value > 0) height = Math.max(height, radius * 2);
    if (height > 0) {
      const y = bottom - height;
      parts.push(`<rect class="${today ? 'bar-today' : 'bar-day'}" x="${(cx - barWidth / 2).toFixed(1)}" y="${y.toFixed(1)}" width="${barWidth.toFixed(1)}" height="${height.toFixed(1)}" rx="${radius.toFixed(1)}"/>`);
      parts.push(`<text class="value" x="${cx.toFixed(1)}" y="${(y - 4).toFixed(1)}" text-anchor="middle">${d.formatCompact(value)}</text>`);
    }
    parts.push(`<text class="day${today ? ' today' : ''}" x="${cx.toFixed(1)}" y="${labelBaseline}" text-anchor="middle">${labels[i]}</text>`);
  });
  if (goal > 0) {
    const goalY = (bottom - (chartHeight * goal) / max).toFixed(1);
    parts.push(`<line class="goal" x1="0" x2="${width}" y1="${goalY}" y2="${goalY}"/>`);
  }
  return `<svg viewBox="0 0 ${width} 150" aria-hidden="true">${parts.join('')}</svg>`;
}

function describeSession(s) {
  const start = new Date(s.start);
  const date = `${start.getMonth() + 1}월 ${start.getDate()}일 (${DAY_NAMES[start.getDay()]})`;
  return `${date} ${d.formatHourMinute(s.start)} ~ ${d.formatHourMinute(s.end)}`;
}

function renderRecords() {
  const sessions = store.sessions;
  const now = Date.now();
  const daily = d.dailyMinutes(sessions, now, 7);
  $('stat-week').textContent = d.formatDuration(daily.reduce((sum, m) => sum + m, 0));
  $('stat-streak').textContent = `${d.streakDays(sessions, now)}일`;
  $('stat-total').textContent = d.formatDuration(d.totalMinutes(sessions));
  $('chart').innerHTML = chartSvg(daily, dayLabels(now), store.dailyGoal);
  $('records-empty').hidden = sessions.length > 0;
  $('records-hint').hidden = sessions.length === 0;
  $('records-list').innerHTML = sessions.map((s) => `
    <li><button type="button" class="session" data-start="${s.start}">
      <span class="grow">
        <span class="title-s subject block">${escapeHtml(s.subject || '수업')}</span>
        <span class="body-s muted block">${describeSession(s)}</span>
      </span>
      <span class="side">
        <span class="title-s block">${d.formatDuration(d.sessionMinutes(s))}</span>
        <span class="label-m primary block">+${d.sessionXp(s)} XP</span>
      </span>
    </button></li>`).join('');
}

async function confirmDelete(start) {
  const session = store.sessions.find((s) => s.start === start);
  if (!session) return;
  const { action } = await openDialog({
    title: '이 기록을 삭제할까요?',
    message: `${session.subject || '수업'} · ${d.formatDuration(d.sessionMinutes(session))}\n삭제하면 ${d.sessionXp(session)} XP가 사라져요.`,
    actions: [{ id: 'cancel', label: '취소' }, { id: 'delete', label: '삭제', danger: true }],
  });
  if (action !== 'delete') return;
  store.deleteSession(start);
  renderRecords();
  toast('기록을 삭제했어요');
}

// ---- 고양이 ----

function renderCat() {
  const xp = store.totalXp;
  const level = d.levelForXp(xp);
  const stage = d.stageForLevel(level);
  const skin = store.skin;

  $('cat-big').innerHTML = catSvg(stage, skin);
  $('cat-name').textContent = store.catName;
  $('cat-stage-label').textContent = `Lv.${level} · ${stage.name}`;

  const needed = d.xpNeededForNextLevel(level);
  const into = d.xpIntoLevel(xp);
  $('cat-xp-value').textContent = `${into} / ${needed} XP`;
  setProgress($('cat-xp-bar'), into, needed);
  $('cat-xp-hint').textContent = d.isMaxLevel(level)
    ? '최고 레벨이에요! 그래도 함께 공부해 줄 거죠?'
    : `다음 레벨까지 ${d.formatDuration(d.xpRemaining(xp) / d.XP_PER_MINUTE)} 더 공부하면 돼요 (1분 = 1 XP)`;

  $('cat-stage-desc').textContent = stage.description;
  const next = d.nextStage(stage);
  $('cat-next-stage').textContent = next
    ? `Lv.${next.minLevel}이 되면 ${next.name}(으)로 진화해요`
    : '마지막 단계까지 진화했어요!';

  $('cat-stages').innerHTML = d.STAGES.map((s) => {
    const classes = ['stage'];
    if (s === stage) classes.push('current');
    if (level < s.minLevel) classes.push('locked');
    return `<div class="${classes.join(' ')}">
      <div class="cat-wrap">${catSvg(s, skin)}</div>
      <div class="label-m">${s.name}</div>
      <div class="label-s muted">Lv.${s.minLevel}~</div>
    </div>`;
  }).join('');
  scrollIntoRowCenter($('cat-stages'), $('cat-stages').querySelector('.stage.current'));

  $('cat-skins').innerHTML = d.SKINS.map((s) => {
    const unlocked = d.isSkinUnlocked(s, level);
    const label = unlocked ? s.name : `🔒 ${s.name} (Lv.${s.unlockLevel})`;
    return `<button type="button" class="chip" role="radio" aria-checked="${s.id === skin.id}"
      data-skin="${s.id}"${unlocked ? '' : ' disabled'}><span class="dot" style="background:${s.fur}"></span>${label}</button>`;
  }).join('');
}

/** 가로 스크롤 줄에서 지금 단계가 가운데 보이게 한다. */
function scrollIntoRowCenter(row, item) {
  if (!item) return;
  const offset = item.getBoundingClientRect().left - row.getBoundingClientRect().left;
  row.scrollLeft += offset - (row.clientWidth - item.offsetWidth) / 2;
}

function meow() {
  const bubble = $('cat-meow');
  bubble.textContent = pick(MEOWS);
  bubble.hidden = true;
  void bubble.offsetWidth; // 다시 보일 때 애니메이션을 처음부터
  bubble.hidden = false;
  clearTimeout(meowTimer);
  meowTimer = setTimeout(() => { bubble.hidden = true; }, MEOW_VISIBLE_MS);
}

// ---- 설정 ----

function snapGoal(minutes) {
  const clamped = Math.max(30, Math.min(480, minutes));
  return 30 + Math.round((clamped - 30) / 30) * 30;
}

function renderSettings() {
  $('settings-name-value').textContent = store.catName;
  const goal = snapGoal(store.dailyGoal);
  $('settings-goal').value = String(goal);
  $('settings-goal-value').textContent = d.formatDuration(goal);
  $('settings-version').textContent = APP_VERSION;
}

async function editName() {
  const { action, value } = await openDialog({
    title: '고양이 이름',
    input: { label: '이름 (최대 12자)', value: store.catName, maxLength: 12 },
    actions: [{ id: 'cancel', label: '취소' }, { id: 'save', label: '저장' }],
  });
  if (action === 'save' && value.trim()) {
    store.setCatName(value);
    renderSettings();
  }
}

async function exportBackup() {
  const name = `classcat-backup-${new Date().toISOString().slice(0, 10)}.json`;
  const file = new File([store.exportJson()], name, { type: 'application/json' });
  // 아이폰에서는 공유 시트로 '파일에 저장'·에어드롭 등을 고를 수 있다.
  if (navigator.canShare && navigator.canShare({ files: [file] })) {
    try {
      await navigator.share({ files: [file], title: '클래스 냥이 백업' });
      return;
    } catch (e) {
      if (e && e.name === 'AbortError') return;
    }
  }
  const url = URL.createObjectURL(file);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  toast('백업 파일을 만들었어요');
}

async function importBackup(file) {
  if (!file) return;
  const { action } = await openDialog({
    title: '백업을 가져올까요?',
    message: '지금 이 기기에 있는 기록이 백업 파일 내용으로 바뀌어요.',
    actions: [{ id: 'cancel', label: '취소' }, { id: 'import', label: '가져오기' }],
  });
  if (action !== 'import') return;
  try {
    store.importJson(await file.text());
    renderSettings();
    toast(`기록 ${store.sessions.length}개를 가져왔어요`);
  } catch (e) {
    toast('클래스 냥이 백업 파일이 아니에요');
  }
}

async function confirmReset() {
  const { action } = await openDialog({
    title: '정말 초기화할까요?',
    message: '모든 수업 기록과 경험치가 삭제되고 되돌릴 수 없어요.',
    actions: [{ id: 'cancel', label: '취소' }, { id: 'reset', label: '초기화', danger: true }],
  });
  if (action !== 'reset') return;
  store.clearRecords();
  toast('모든 기록을 초기화했어요');
}

// ---- 시작 ----

function init() {
  $('home-start').addEventListener('click', startSession);
  $('home-stop').addEventListener('click', stopSession);
  $('home-subject').addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.isComposing) event.target.blur();
  });
  $('home-cat').addEventListener('click', () => {
    bounce($('home-cat'));
    say(pick(MEOWS));
  });
  $('install-hint-close').addEventListener('click', dismissInstallHint);

  $('records-list').addEventListener('click', (event) => {
    const item = event.target.closest('.session');
    if (item) confirmDelete(Number(item.dataset.start));
  });

  $('cat-big').addEventListener('click', () => {
    bounce($('cat-big'));
    meow();
  });
  $('cat-skins').addEventListener('click', (event) => {
    const chip = event.target.closest('.chip');
    if (!chip || chip.disabled) return;
    store.setSkin(chip.dataset.skin);
    renderCat();
    bounce($('cat-big'));
  });

  $('settings-name').addEventListener('click', editName);
  $('settings-goal').addEventListener('input', (event) => {
    const minutes = Number(event.target.value);
    $('settings-goal-value').textContent = d.formatDuration(minutes);
    store.setDailyGoal(minutes);
  });
  $('settings-export').addEventListener('click', exportBackup);
  $('settings-import').addEventListener('click', () => $('settings-import-file').click());
  $('settings-import-file').addEventListener('change', (event) => {
    importBackup(event.target.files[0]);
    event.target.value = '';
  });
  $('settings-reset').addEventListener('click', confirmReset);

  window.addEventListener('hashchange', () => showTab(tabFromHash()));
  // 다른 앱에 갔다 오면 날짜가 바뀌었거나 타이머가 흘렀을 수 있다.
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) render();
  });

  showTab(tabFromHash());

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch((e) => console.warn('오프라인 캐시를 켜지 못했어요', e));
  }
}

init();
