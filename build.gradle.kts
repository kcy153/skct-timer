// AGP 9.x부터 Kotlin 지원이 내장돼 있어(built-in Kotlin) org.jetbrains.kotlin.android 플러그인은
// 더 이상 필요/사용 불가 — https://kotl.in/gradle/agp-built-in-kotlin
plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
