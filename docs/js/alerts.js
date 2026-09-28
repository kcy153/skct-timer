// Kotlin domain/Alerts.kt 대응. AlertKind: 'PRE' | 'BOUNDARY' | 'MID'

const PRE_LEAD_SECONDS = [3, 2, 1];
const KIND_ORDER = { PRE: 0, BOUNDARY: 1, MID: 2 };

/**
 * 타임라인의 모든 "경계"(시험 시작, 영역 종료=휴식 시작, 휴식 종료=다음 영역 시작, 시험 종료)에
 * 대해 B-3/B-2/B-1(PRE)과 B(BOUNDARY)를 만들고, 각 영역 종료 180초 전에 MID를 하나씩 추가한다.
 * breakSec=0이면 Break 세그먼트가 없어 "영역 종료"와 "다음 영역 시작"이 같은 시각이 되므로,
 * Set으로 경계 시각을 모아 자연히 중복 제거된다.
 */
export function buildAlerts(timeline, config) {
  const boundaries = new Set();

  const countdown = timeline.find((s) => s.type === "COUNTDOWN");
  const sections = timeline.filter((s) => s.type === "SECTION").sort((a, b) => a.index - b.index);
  const breaks = timeline.filter((s) => s.type === "BREAK");

  boundaries.add(countdown ? countdown.endSec : sections[0]?.startSec ?? 0);
  sections.forEach((s) => boundaries.add(s.endSec));
  breaks.forEach((b) => boundaries.add(b.endSec));

  const alerts = [];
  for (const boundary of [...boundaries].sort((a, b) => a - b)) {
    for (const lead of PRE_LEAD_SECONDS) {
      const t = boundary - lead;
      if (t >= 0) alerts.push({ timeSec: t, kind: "PRE", leadSec: lead });
    }
    alerts.push({ timeSec: boundary, kind: "BOUNDARY", leadSec: 0 });
  }

  sections.forEach((section) => {
    const midTime = section.endSec - config.midAlertLeadSec;
    if (midTime > section.startSec) {
      alerts.push({ timeSec: midTime, kind: "MID", leadSec: 0 });
    }
  });

  return alerts.sort((a, b) => a.timeSec - b.timeSec || KIND_ORDER[a.kind] - KIND_ORDER[b.kind]);
}
