// 계획서 §2/§8-1을 그대로 옮긴 타임라인 로직. Kotlin domain/ExamConfig.kt·Timeline.kt 대응.

export const DEFAULT_BREAK_SEC = 60;
export const MIN_BREAK_SEC = 0;
export const MAX_BREAK_SEC = 5 * 60;
export const BREAK_STEP_SEC = 10;

// 실제 시험 순서로 확인됨(계획서 §12-1, 2026-09 사용자 확인).
export const DEFAULT_SECTION_NAMES = ["언어이해", "자료해석", "창의수리", "언어추리", "수열추리"];

export function createConfig(overrides = {}) {
  const config = {
    sectionCount: 5,
    sectionSec: 15 * 60,
    breakSec: DEFAULT_BREAK_SEC,
    countdownSec: 3,
    midAlertLeadSec: 3 * 60,
    sectionNames: DEFAULT_SECTION_NAMES,
    ...overrides,
  };
  if (config.sectionNames.length !== config.sectionCount) {
    throw new Error(
      `sectionNames.length(${config.sectionNames.length}) must equal sectionCount(${config.sectionCount})`
    );
  }
  return config;
}

/** SegmentType: 'COUNTDOWN' | 'SECTION' | 'BREAK' */

/**
 * Countdown → (Section → Break?) × sectionCount 순서로 배치.
 * breakSec이 0이면 Break 세그먼트를 만들지 않는다(경계 중복 방지).
 */
export function buildTimeline(config) {
  const segments = [];
  let cursor = 0;

  if (config.countdownSec > 0) {
    segments.push({ type: "COUNTDOWN", index: 0, startSec: cursor, endSec: cursor + config.countdownSec });
    cursor += config.countdownSec;
  }

  for (let i = 1; i <= config.sectionCount; i++) {
    segments.push({ type: "SECTION", index: i, startSec: cursor, endSec: cursor + config.sectionSec });
    cursor += config.sectionSec;

    const isLastSection = i === config.sectionCount;
    if (!isLastSection && config.breakSec > 0) {
      segments.push({ type: "BREAK", index: i, startSec: cursor, endSec: cursor + config.breakSec });
      cursor += config.breakSec;
    }
  }

  return segments;
}

export function segmentDurationSec(segment) {
  return segment.endSec - segment.startSec;
}

/** 화면과 알림이 공유하는 세그먼트 표시 이름. */
export function segmentLabel(config, segment) {
  if (segment.type === "COUNTDOWN") return "카운트다운";
  if (segment.type === "BREAK") return "휴식";
  return config.sectionNames[segment.index - 1] ?? `영역 ${segment.index}`;
}

/** 휴식 화면의 "다음: OO" 표시용 — 이 휴식 다음에 시작될 영역 이름. */
export function nextSectionNameAfterBreak(config, breakSegment) {
  if (breakSegment.type !== "BREAK") throw new Error(`not a break segment: ${JSON.stringify(breakSegment)}`);
  return config.sectionNames[breakSegment.index] ?? `영역 ${breakSegment.index + 1}`;
}
