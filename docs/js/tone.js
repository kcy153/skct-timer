// Kotlin domain/ToneWaveform.kt 대응 — 웹에서는 PCM 버퍼 대신 OscillatorNode+GainNode로
// 같은 스펙(주파수/길이/페이드)의 사인파를 직접 재생한다. Web Audio API는 표준이라 안정적.

export const TONE_SPECS = {
  PRE: { frequencyHz: 660, durationMs: 120, fadeMs: 5 },
  MID: { frequencyHz: 880, durationMs: 400, fadeMs: 5 },
  BOUNDARY: { frequencyHz: 1320, durationMs: 600, fadeMs: 5 },
};

const PEAK_GAIN = 0.8; // 풀스케일 근처 클리핑 방지 여유

class ToneEngine {
  constructor() {
    this.ctx = null;
  }

  /** 반드시 사용자 제스처(클릭 등) 핸들러 안에서 처음 호출해야 브라우저가 오디오를 허용한다. */
  ensureContext() {
    if (!this.ctx) {
      const Ctor = window.AudioContext || window.webkitAudioContext;
      this.ctx = new Ctor();
    }
    if (this.ctx.state === "suspended") {
      this.ctx.resume();
    }
    return this.ctx;
  }

  play(kind) {
    const spec = TONE_SPECS[kind];
    if (!spec) return;
    const ctx = this.ensureContext();

    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = "sine";
    osc.frequency.value = spec.frequencyHz;

    const now = ctx.currentTime;
    const dur = spec.durationMs / 1000;
    const fade = Math.min(spec.fadeMs / 1000, dur / 2);

    gain.gain.setValueAtTime(0, now);
    gain.gain.linearRampToValueAtTime(PEAK_GAIN, now + fade);
    gain.gain.setValueAtTime(PEAK_GAIN, now + dur - fade);
    gain.gain.linearRampToValueAtTime(0, now + dur);

    osc.connect(gain).connect(ctx.destination);
    osc.start(now);
    osc.stop(now + dur + 0.02);
  }
}

export const toneEngine = new ToneEngine();
