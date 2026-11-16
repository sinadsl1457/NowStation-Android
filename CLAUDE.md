# NowStation Android

지하철에서 지금 무슨 역인지 잠금화면에 보여 주는 앱의 Android 클라이언트. 판단(탄 열차 찾기)은 서버가 한다 → NowStation-Server.
계획·일정은 서버 저장소의 `docs/PLAN.md`.

## 스택
- Kotlin, Jetpack Compose(Material 3), Hilt, Retrofit + kotlinx.serialization, Coroutines/Flow
- 버전은 MapPilot-Android에서 이 컴퓨터로 빌드가 확인된 것(AGP 9.3.3, Kotlin 2.2.10, Compose BOM 2025.12, Hilt 2.60.1)
- minSdk 26, targetSdk 36. 에뮬레이터는 Android 17(API 37)
- JDK: Android Studio 내장 `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
- 서버 주소: `local.properties`의 `API_BASE_URL`, 없으면 `http://10.0.2.2:8080/`(에뮬레이터에서 본 내 컴퓨터). http는 10.0.2.2·localhost만 허용

## 구조 — 기능 폴더는 "상태를 누가 갖고 언제 사라지나"로 나눈다
```
com.nowstation.app
├ core/      결과·오류 타입(AppResult, safeApiCall), 네트워크(Retrofit), 테마, 저장소 연결(Hilt)
├ station/   역 검색 + 홈 화면. 상태는 홈 화면 ViewModel이 갖고 화면과 함께 사라진다
└ ride/      진행 중인 탑승. 화면이 사라져도 계속돼야 해서 @Singleton RideTracker가 갖는다
   ├ domain/  RideTracker(앱 전체가 같이 보는 탑승 상태), 모델, 저장소 인터페이스
   ├ data/    서버 탑승 API
   ├ service/ 포그라운드 서비스(몇 초마다 서버에서 받아 옴) + 잠금화면 진행 중 알림
   └ ui/      탑승 화면, 화면·알림이 같이 쓰는 문구(RideTexts)
```
- 의존 방향: ui → domain ← data. domain은 Android를 모른다(RideTracker도 순수 Kotlin + 주입된 Clock).
- ViewModel은 Context를 모른다. 서비스 시작은 `RideServiceLauncher` 인터페이스로.
- 화면 이동 라이브러리 없이 "탑승 중이면 탑승 화면, 아니면 홈"으로 고른다(화면이 둘뿐).
- 로직 없는 UseCase는 만들지 않는다. ViewModel → 저장소/RideTracker 직접.
- 시간은 `Clock` 주입.

## 작업 방식
- 커밋은 기능 하나씩. 피드백 반영 커밋은 무엇을 반영했는지 쓴다. squash·amend·force push 금지.
- 버그를 고치면 고치기 전에 실패하는 테스트로 고정한다.
- 테스트: ViewModel·RideTracker는 가짜 저장소, 저장소는 MockWebServer(서버와 같은 응답 모양), 화면은 에뮬레이터를 adb로 눌러 확인.
- 에뮬레이터 한글 입력: 키보드가 한글 두벌식이라 `adb shell input text`에 자판 키를 넣는다(태평 = xovud, 강남 = rkdska). BACK 키는 앱을 닫을 수 있어 쓰지 않는다.
- 주석은 결정과 이유만.
