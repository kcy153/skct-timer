# 쓱시티 타이머

SKCT 인지역량 시험(5개 영역 × 15분, 영역 사이 1분 휴식)을 실제와 같은 시간 흐름으로 연습하기 위한
개인용 안드로이드 타이머 앱. 자세한 기획은 [`skct-timer-plan.md`](skct-timer-plan.md) 참고.

## 설치 방법

<img src="dist/install-qr.png" alt="설치 QR 코드" width="220" />

1. 폰 카메라로 위 QR을 스캔하고 링크를 연다. (직접 링크: [최신 릴리스](https://github.com/kcy153/skct-timer/releases/latest))
2. APK를 다운로드한다. 브라우저에서 "이런 파일은 기기에 해를 끼칠 수 있음" 경고가 나오면 "그래도 다운로드"를 선택한다.
3. 다운로드한 APK를 열고, "출처를 알 수 없는 앱 설치" 허용을 묻는 창이 뜨면 해당 브라우저에 대해 허용한다.
4. 설치 후 첫 실행에서 알림 권한을 허용한다. (거부해도 앱은 정상 동작하고, 소리·진동 알림도 그대로 울린다 — 진행 상태를 보여주는 상시 알림만 안 뜬다)
5. 업데이트할 때는 같은 방식으로 새 APK를 설치하면 기존 앱 위에 덮어써진다. (같은 keystore로 계속 서명하므로 데이터가 유지된다)

Play스토어를 거치지 않는 개인 배포용 APK라 자동 업데이트는 지원하지 않는다 — 새 버전이 나오면 위 과정을 다시 거쳐야 한다.

## 개발자용

### 빌드

```
./gradlew.bat testDebugUnitTest   # 유닛 테스트
./gradlew.bat assembleDebug       # 디버그 APK
./gradlew.bat assembleRelease     # 릴리스 APK (keystore.properties 필요, 아래 참고)
```

### 릴리스 서명

이 저장소에는 keystore가 포함돼 있지 않다(`.gitignore` 처리). 새로 clone한 환경에서 릴리스 빌드를 하려면:

1. 프로젝트 루트에 릴리스 keystore를 만든다(한 번만, 계속 같은 파일을 재사용):
   ```
   keytool -genkeypair -v -keystore skct-timer-release.jks -alias skct-timer \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. 프로젝트 루트에 `keystore.properties`를 만든다:
   ```
   storeFile=skct-timer-release.jks
   storePassword=<비밀번호>
   keyAlias=skct-timer
   keyPassword=<비밀번호>
   ```
3. keystore 파일과 비밀번호는 안전한 곳(비밀번호 관리자 등)에 반드시 따로 백업해둘 것 — 분실하면 기존 설치 위에 업데이트를 배포할 수 없게 된다.

### 새 버전 배포 (버전을 올려 다시 릴리스할 때)

1. `app/build.gradle.kts`의 `versionCode`/`versionName` 갱신
2. `./gradlew.bat assembleRelease`
3. `dist/skct-timer-v<버전>.apk`로 복사
4. `gh release create v<버전> dist/skct-timer-v<버전>.apk --title "..." --notes "..."`
5. `python scripts/make_qr.py <릴리스 자산 다운로드 URL>` — `dist/install-qr.png` 다시 생성

### 디버그 전용 기능

디버그 빌드의 시작 화면에는 모든 시간을 1/60로 줄이는 "빠른 테스트 시작" 버튼이 있다(§10-2). 릴리스 빌드에는 노출되지 않는다.

## 수동 테스트 체크리스트

실제 소리/진동은 코드로 검증할 수 없어 사람이 직접 확인해야 한다 (계획서 §10-3):

- [ ] 시작 버튼 → 3, 2, 1 예비음 → 높은 경계음 → 첫 영역 시작
- [ ] 영역 12분 지점(종료 3분 전)에 알림음 1번, 숫자가 앰버로 변경
- [ ] 영역 종료 3초 전부터 예비음 3번 → 경계음 → 휴식 화면
- [ ] 휴식 종료 3초 전부터 예비음 3번 → 경계음 → 다음 영역
- [ ] 5번째 영역 종료 시 예비음 → 경계음 → 종료 화면
- [ ] 일시정지 후 재개하면 남은 시간이 그대로 이어지고 알림 시각도 밀린다
- [ ] 화면을 끄거나 다른 앱으로 이동해도 알림이 제때 울린다
- [ ] 소리만 / 진동만 / 둘 다 / 둘 다 끔 각각 동작
- [ ] 휴식 시간을 바꾸면(예: 2분, 0초) 다음 시험부터 반영
- [ ] 뒤로가기 시 중단 확인창, 확인하면 서비스와 웨이크락이 정리됨
- [ ] 79분 전체를 한 번 돌렸을 때 종료 시각의 오차가 1초 이내
