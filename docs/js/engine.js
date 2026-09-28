// Kotlin domain/ExamClockState.kt, EnginePhase.kt, LiveScreen.kt, TimeFormat.kt 대응.

/** 타이머의 시간 산술만 담당하는 순수 상태. 기준 시계는 항상 Date.now()(ms). */
export class ExamClockState {
  constructor(startMs, pausedAccumMs = 0, pauseStartMs = null) {
    this.startMs = startMs;
    this.pausedAccumMs = pausedAccumMs;
    this.pauseStartMs = pauseStartMs;
  }

  get isPaused() {
    return this.pauseStartMs !== null;
  }

  activeElapsedMs(nowMs) {
    const pausedSoFar = this.pausedAccumMs + (this.pauseStartMs !== null ? Math.max(0, nowMs - this.pauseStartMs) : 0);
    return Math.max(0, nowMs - this.startMs - pausedSoFar);
  }

  pause(nowMs) {
    if (this.isPaused) return this;
    return new ExamClockState(this.startMs, this.pausedAccumMs, nowMs);
  }

  resume(nowMs) {
    if (!this.isPaused) return this;
    const elapsedWhilePaused = Math.max(0, nowMs - this.pauseStartMs);
    return new ExamClockState(this.startMs, this.pausedAccumMs + elapsedWhilePaused, null);
  }
}

/** activeElapsedSec 시점에 타임라인의 어느 세그먼트에 있는지. */
export function computePhase(timeline, activeElapsedSec) {
  const totalSec = Math.max(...timeline.map((s) => s.endSec));
  if (activeElapsedSec >= totalSec) return { type: "Finished" };
  const segment = timeline.find((s) => activeElapsedSec >= s.startSec && activeElapsedSec < s.endSec);
  return { type: "Active", segment };
}

function ceilRemainingSec(segment, activeElapsedMs) {
  const remainingMs = Math.max(0, segment.endSec * 1000 - activeElapsedMs);
  return Math.floor((remainingMs + 999) / 1000);
}

/**
 * LiveScreen: { screen: 'Start' } | { screen:'Exam', segment, remainingSec, paused, isWarning }
 * | { screen:'BreakTime', segment, remainingSec, nextSectionName, paused } | { screen:'Finished' }
 */
export function computeLiveScreen(phase, activeElapsedMs, isPaused, config, nextSectionNameAfterBreak) {
  if (phase.type === "Finished") return { screen: "Finished" };

  const segment = phase.segment;
  const remainingSec = ceilRemainingSec(segment, activeElapsedMs);

  if (segment.type === "BREAK") {
    return {
      screen: "BreakTime",
      segment,
      remainingSec,
      nextSectionName: nextSectionNameAfterBreak(config, segment),
      paused: isPaused,
    };
  }

  return {
    screen: "Exam",
    segment,
    remainingSec,
    paused: isPaused,
    isWarning: segment.type === "SECTION" && remainingSec <= config.midAlertLeadSec,
  };
}

/** 남은 시간 표시(MM:SS). */
export function formatMmSs(totalSec) {
  const clamped = Math.max(0, totalSec);
  const m = Math.floor(clamped / 60);
  const s = clamped % 60;
  return `${m}:${String(s).padStart(2, "0")}`;
}
