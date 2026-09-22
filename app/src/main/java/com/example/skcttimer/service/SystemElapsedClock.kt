package com.example.skcttimer.service

import android.os.SystemClock

/** 실제 안드로이드 기기에서 쓰는 Clock 구현 — elapsedRealtime()은 절전/화면꺼짐에도 계속 흐른다. */
object SystemElapsedClock : Clock {
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
}
