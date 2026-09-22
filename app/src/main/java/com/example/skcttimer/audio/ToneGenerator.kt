package com.example.skcttimer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.skcttimer.domain.AlertKind
import com.example.skcttimer.domain.generateSineWavePcm16
import com.example.skcttimer.domain.toneSpecFor

/**
 * §8-5: 사인파 PCM 버퍼를 미리 만들어 두고(prepare) AudioTrack(MODE_STATIC)으로 재생한다.
 * 알림 직전에 새로 만들지 않아서 재생 지연이 없다. 오디오 속성은 USAGE_MEDIA — 알람 볼륨이
 * 아니라 미디어 볼륨을 따른다(계획서 명시).
 */
class ToneGenerator(private val sampleRateHz: Int = 44_100) {

    private val tracks = mutableMapOf<AlertKind, AudioTrack>()

    fun prepare() {
        for (kind in AlertKind.entries) {
            tracks.getOrPut(kind) { buildTrack(kind) }
        }
    }

    fun play(kind: AlertKind) {
        val track = tracks[kind] ?: buildTrack(kind).also { tracks[kind] = it }
        try {
            track.stop()
        } catch (_: IllegalStateException) {
            // 아직 한 번도 재생 안 됐으면 stop()이 예외를 던질 수 있음 — 무해하게 무시
        }
        track.reloadStaticData()
        track.play()
    }

    fun release() {
        tracks.values.forEach { it.release() }
        tracks.clear()
    }

    private fun buildTrack(kind: AlertKind): AudioTrack {
        val pcm = generateSineWavePcm16(toneSpecFor(kind), sampleRateHz)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRateHz)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val track = AudioTrack(
            attributes,
            format,
            pcm.size * 2, // 16bit = 2바이트/샘플
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
        track.write(pcm, 0, pcm.size)
        return track
    }
}
