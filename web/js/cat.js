// 고양이 SVG 만들기: 몸통(털 색) → 줄무늬 → 얼굴 → 진화 단계 소품.

import { LAYERS } from './cat-art.js';
import { escapeHtml } from './util.js';

function pathTag(p, fillOverride) {
  const fill = fillOverride || p.fill || 'none';
  let attrs = `d="${p.d}" fill="${fill}"`;
  if (p.fillOpacity != null) attrs += ` fill-opacity="${p.fillOpacity}"`;
  if (p.stroke) {
    attrs += ` stroke="${p.stroke}" stroke-width="${p.strokeWidth}"`;
    if (p.strokeOpacity != null) attrs += ` stroke-opacity="${p.strokeOpacity}"`;
    if (p.cap) attrs += ` stroke-linecap="${p.cap}"`;
    if (p.join) attrs += ` stroke-linejoin="${p.join}"`;
  }
  return `<path ${attrs}/>`;
}

export function catSvg(stage, skin) {
  const parts = LAYERS.body.map((p) => pathTag(p, skin.fur));
  if (skin.striped) parts.push(...LAYERS.stripes.map((p) => pathTag(p)));
  parts.push(...LAYERS.face.map((p) => pathTag(p)));
  if (stage.accessory) parts.push(...LAYERS[stage.accessory].map((p) => pathTag(p)));
  const label = escapeHtml(`${stage.name}, ${skin.name}`);
  return `<svg viewBox="0 0 120 120" role="img" aria-label="${label}">${parts.join('')}</svg>`;
}

/** 눌렀을 때 말랑하게 튀는 애니메이션. */
export function bounce(element) {
  element.classList.remove('bounce');
  void element.offsetWidth; // 애니메이션을 처음부터 다시 시작하게 한다
  element.classList.add('bounce');
}
