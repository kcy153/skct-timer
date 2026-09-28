import {
  createConfig,
  buildTimeline,
  segmentLabel,
  nextSectionNameAfterBreak,
  DEFAULT_BREAK_SEC,
  MIN_BREAK_SEC,
  MAX_BREAK_SEC,
  BREAK_STEP_SEC,
} from "./timeline.js";
import { buildAlerts } from "./alerts.js";
import { ExamClockState, computePhase, computeLiveScreen, formatMmSs } from "./engine.js";
import { toneEngine } from "./tone.js";

const STORAGE_KEY_BREAK = "skct-break-sec";
const STORAGE_KEY_SOUND = "skct-sound-enabled";

// 알림이 이 값(ms)보다 더 오래 지난 뒤에야 처리되면(탭이 오래 백그라운드였다 돌아온 경우)
// 소리는 건너뛰고 조용히 따라잡기만 한다 — 한꺼번에 여러 알림음이 몰려 울리는 것을 막음.
const STALE_ALERT_MS = 5000;

const params = new URLSearchParams(location.search);
const isFastMode = params.get("fast") === "1";
const forceMini = params.get("mini") === "1";
const APP_TITLE = "쓱시티 타이머";
const miniQuery = window.matchMedia("(max-height: 200px)");

function applyMini() {
  document.body.classList.toggle("mini", forceMini || miniQuery.matches);
}

const el = {
  screens: {
    start: document.getElementById("screen-start"),
    exam: document.getElementById("screen-exam"),
    finished: document.getElementById("screen-finished"),
  },
  breakSecInput: document.getElementById("breakSec"),
  breakSecLabel: document.getElementById("breakSecLabel"),
  soundToggle: document.getElementById("soundToggle"),
  startBtn: document.getElementById("startBtn"),
  miniBtn: document.getElementById("miniBtn"),
  segmentLabel: document.getElementById("examSegmentLabel"),
  time: document.getElementById("examTime"),
  nextLabel: document.getElementById("examNextLabel"),
  pauseBtn: document.getElementById("pauseBtn"),
  stopBtn: document.getElementById("stopBtn"),
  restartBtn: document.getElementById("restartBtn"),
  dialog: document.getElementById("confirmDialog"),
  confirmCancel: document.getElementById("confirmCancel"),
  confirmOk: document.getElementById("confirmOk"),
  fastBadge: document.getElementById("fastBadge"),
};

let config = createConfig();
let timeline = [];
let alerts = [];
let clock = null;
let nextAlertIndex = 0;
let worker = null;
let soundEnabled = true;

function applyFastScale(baseConfig) {
  if (!isFastMode) return baseConfig;
  return createConfig({
    ...baseConfig,
    sectionSec: Math.max(1, Math.ceil(baseConfig.sectionSec / 60)),
    breakSec: baseConfig.breakSec > 0 ? Math.max(1, Math.ceil(baseConfig.breakSec / 60)) : 0,
    midAlertLeadSec: Math.max(1, Math.ceil(baseConfig.midAlertLeadSec / 60)),
  });
}

function loadSettings() {
  const savedBreak = Number.parseInt(localStorage.getItem(STORAGE_KEY_BREAK) ?? "", 10);
  const breakSec = Number.isFinite(savedBreak) ? clampBreakSec(savedBreak) : DEFAULT_BREAK_SEC;
  const savedSound = localStorage.getItem(STORAGE_KEY_SOUND);
  soundEnabled = savedSound === null ? true : savedSound === "1";

  el.breakSecInput.min = String(MIN_BREAK_SEC);
  el.breakSecInput.max = String(MAX_BREAK_SEC);
  el.breakSecInput.step = String(BREAK_STEP_SEC);
  el.breakSecInput.value = String(breakSec);
  el.soundToggle.checked = soundEnabled;
  updateBreakSecLabel(breakSec);

  if (isFastMode) el.fastBadge.classList.remove("hidden");
}

function clampBreakSec(sec) {
  return Math.min(MAX_BREAK_SEC, Math.max(MIN_BREAK_SEC, sec));
}

function updateBreakSecLabel(sec) {
  el.breakSecLabel.textContent = formatMmSs(sec);
}

el.breakSecInput.addEventListener("input", () => {
  const sec = clampBreakSec(Number.parseInt(el.breakSecInput.value, 10));
  updateBreakSecLabel(sec);
  localStorage.setItem(STORAGE_KEY_BREAK, String(sec));
});

el.soundToggle.addEventListener("change", () => {
  soundEnabled = el.soundToggle.checked;
  localStorage.setItem(STORAGE_KEY_SOUND, soundEnabled ? "1" : "0");
});

function showScreen(name) {
  for (const [key, node] of Object.entries(el.screens)) {
    node.classList.toggle("hidden", key !== name);
  }
  if (name !== "exam") document.title = APP_TITLE;
}

function startExam() {
  // 사용자 제스처(이 클릭 핸들러) 안에서 오디오 컨텍스트를 깨워둬야, 이후 프로그램적으로
  // 재생되는 알림음이 브라우저에 의해 막히지 않는다.
  toneEngine.ensureContext();

  const breakSec = clampBreakSec(Number.parseInt(el.breakSecInput.value, 10));
  const baseConfig = createConfig({ breakSec });
  config = applyFastScale(baseConfig);
  timeline = buildTimeline(config);
  alerts = buildAlerts(timeline, config);
  nextAlertIndex = 0;
  clock = new ExamClockState(Date.now());

  showScreen("exam");
  startTicking();
  tick();
}

function startTicking() {
  stopTicking();
  if (typeof Worker !== "undefined") {
    worker = new Worker("js/tick-worker.js");
    worker.onmessage = tick;
  } else {
    worker = { fallbackInterval: setInterval(tick, 200) };
  }
}

function stopTicking() {
  if (!worker) return;
  if (worker.terminate) worker.terminate();
  if (worker.fallbackInterval) clearInterval(worker.fallbackInterval);
  worker = null;
}

function tick() {
  if (!clock) return;
  const nowMs = Date.now();
  const activeElapsedMs = clock.activeElapsedMs(nowMs);
  const activeElapsedSec = Math.floor(activeElapsedMs / 1000);
  const phase = computePhase(timeline, activeElapsedSec);

  if (!clock.isPaused) {
    fireDueAlerts(activeElapsedMs);
  }

  const liveScreen = computeLiveScreen(phase, activeElapsedMs, clock.isPaused, config, nextSectionNameAfterBreak);
  render(liveScreen);

  if (liveScreen.screen === "Finished") {
    stopTicking();
    showScreen("finished");
  }
}

function fireDueAlerts(activeElapsedMs) {
  while (nextAlertIndex < alerts.length && alerts[nextAlertIndex].timeSec * 1000 <= activeElapsedMs) {
    const alert = alerts[nextAlertIndex];
    const staleness = activeElapsedMs - alert.timeSec * 1000;
    if (soundEnabled && staleness <= STALE_ALERT_MS) {
      toneEngine.play(alert.kind);
    }
    nextAlertIndex++;
  }
}

function render(liveScreen) {
  if (liveScreen.screen !== "Exam" && liveScreen.screen !== "BreakTime") return;

  const { segment, remainingSec, paused } = liveScreen;
  el.segmentLabel.textContent = segmentLabel(config, segment);
  el.time.textContent = formatMmSs(remainingSec);
  // 여러 창을 띄워 쓸 때 작업표시줄/창 제목에서도 구분되도록 제목에 과목명+남은 시간을 넣는다.
  document.title = `${formatMmSs(remainingSec)} ${el.segmentLabel.textContent}`;
  el.time.classList.toggle("warning", liveScreen.screen === "Exam" && liveScreen.isWarning);

  if (liveScreen.screen === "BreakTime") {
    el.nextLabel.textContent = `다음: ${liveScreen.nextSectionName}`;
    el.nextLabel.classList.remove("hidden");
  } else {
    el.nextLabel.classList.add("hidden");
  }

  el.pauseBtn.textContent = paused ? "재개" : "일시정지";
}

el.startBtn.addEventListener("click", startExam);

el.miniBtn.addEventListener("click", () => {
  const url = new URL(location.href);
  url.searchParams.set("mini", "1");
  window.open(url.toString(), "_blank", "popup,width=460,height=130");
});

miniQuery.addEventListener("change", applyMini);
applyMini();

el.pauseBtn.addEventListener("click", () => {
  if (!clock) return;
  const nowMs = Date.now();
  clock = clock.isPaused ? clock.resume(nowMs) : clock.pause(nowMs);
  tick();
});

el.stopBtn.addEventListener("click", () => {
  el.dialog.classList.remove("hidden");
});

el.confirmCancel.addEventListener("click", () => {
  el.dialog.classList.add("hidden");
});

el.confirmOk.addEventListener("click", () => {
  el.dialog.classList.add("hidden");
  stopTicking();
  clock = null;
  showScreen("start");
});

el.restartBtn.addEventListener("click", () => {
  showScreen("start");
});

document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "visible") tick();
});

loadSettings();
showScreen("start");
