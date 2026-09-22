package com.example.skcttimer.domain

/** §5: 남은 시간 표시(MM:SS). 알림 텍스트와 화면이 같은 포맷을 쓰도록 공유. */
fun formatMmSs(totalSec: Int): String {
    val clamped = totalSec.coerceAtLeast(0)
    val m = clamped / 60
    val s = clamped % 60
    return "%d:%02d".format(m, s)
}
