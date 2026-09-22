package com.example.skcttimer.domain

/** §4-3: 소리 상수. 오디오 파일 없이 사인파를 코드로 생성한다. */
data class ToneSpec(
    val frequencyHz: Double,
    val durationMs: Int,
    val fadeMs: Int = 5,
)

object ToneSpecs {
    val PRE = ToneSpec(frequencyHz = 660.0, durationMs = 120)
    val MID = ToneSpec(frequencyHz = 880.0, durationMs = 400)
    val BOUNDARY = ToneSpec(frequencyHz = 1320.0, durationMs = 600)
}

fun toneSpecFor(kind: AlertKind): ToneSpec = when (kind) {
    AlertKind.PRE -> ToneSpecs.PRE
    AlertKind.MID -> ToneSpecs.MID
    AlertKind.BOUNDARY -> ToneSpecs.BOUNDARY
}

/**
 * spec을 16비트 PCM 모노 샘플로 렌더링한다. 시작/끝 fadeMs 동안 선형 페이드를 줘서
 * 클릭 노이즈를 없앤다(§8-5). 재생(AudioTrack)은 audio 패키지의 몫 — 여기는 순수 계산만.
 */
fun generateSineWavePcm16(spec: ToneSpec, sampleRateHz: Int = 44_100): ShortArray {
    val totalSamples = (spec.durationMs.toLong() * sampleRateHz / 1000).toInt().coerceAtLeast(1)
    val fadeSamples = (spec.fadeMs.toLong() * sampleRateHz / 1000).toInt().coerceIn(0, totalSamples / 2)
    val amplitude = Short.MAX_VALUE * 0.8 // 풀스케일 근처 클리핑 방지 여유

    return ShortArray(totalSamples) { i ->
        val angle = 2.0 * Math.PI * spec.frequencyHz * i / sampleRateHz
        val raw = kotlin.math.sin(angle)
        val fade = when {
            fadeSamples <= 0 -> 1.0
            i < fadeSamples -> i.toDouble() / fadeSamples
            i >= totalSamples - fadeSamples -> (totalSamples - 1 - i).toDouble() / fadeSamples
            else -> 1.0
        }
        (raw * amplitude * fade).toInt().toShort()
    }
}
