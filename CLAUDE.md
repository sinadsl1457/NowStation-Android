# NowStation Android

지하철에서 지금 무슨 역인지 잠금화면에 보여 주는 앱의 Android 클라이언트. 판단(탄 열차 찾기)은 서버가 한다 → NowStation-Server.
계획·일정은 서버 저장소의 `docs/PLAN.md`.

## 스택
- Kotlin, Jetpack Compose(Material 3), Hilt, Retrofit + kotlinx.serialization, Coroutines/Flow
- 버전은 MapPilot-Android에서 이 컴퓨터로 빌드가 확인된 것(AGP 9.3.3, Kotlin 2.2.10, Compose BOM 2025.12, Hilt 2.60.1)
- minSdk 26, targetSdk 36. 에뮬레이터는 Android 17(API 37)
- JDK: Android Studio 내장 `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
- 서버 주소: `local.properties`의 `API_BASE_URL`, 없으면 `http://10.0.2.2:8080/`(에뮬레이터에서 본 내 컴퓨터). 지금은 배포 서버(시드니 EC2, https `<IP를 -로>.sslip.io`)로 설정돼 있다. 시연용 재생 서버는 `-PAPI_BASE_URL=http://10.0.2.2:8080/`
- http: 모든 빌드에서 10.0.2.2·localhost만 허용(배포 서버는 https). release는 `API_BASE_URL`이 https가 아니면 빌드 중단

## 구조 — 기능 폴더는 "상태를 누가 갖고 언제 사라지나"로 나눈다
```
com.nowstation.app   (코드 패키지. 스토어·기기에서 쓰는 앱 ID는 com.taektoy.nowstation)
├ core/      결과·오류 타입(AppResult, safeApiCall), 네트워크(Retrofit), 테마, 저장소 연결(Hilt). 홈 위에 덮는 밝은 화면은 `DarkStatusBarIcons()`로 상태 표시줄 글자를 어둡게
├ station/   역 검색 + 홈 화면. 상태는 홈 화면 ViewModel이 갖고 화면과 함께 사라진다
├ settings/  설정. 모두 앱 안에서: 알림 스위치(AlertSettings: 갈아타기·내릴 역·진동, RideService가 따름. 진행 중 알림은 포그라운드 서비스라 못 끔), 사용법 다시 보기, 문의하기(ContactViewModel → 서버 POST /api/feedback), 개인정보처리방침(WebView, 서버 /privacy), 라이선스, 버전. 개인정보처리방침은 Play 정책상 앱 안에서도 열려야 한다
├ guide/     처음 실행 가이드 4장(+ 봤는지 저장). 홈과 따로 생기고 사라져서(처음 한 번·다시 보기) 따로 둠. 알림 권한은 마지막 장 버튼에서 묻는다
├ schedule/  출발 예약(나갈 시각 전 알림). 홈 화면이 사라져도 알람·재부팅 뒤까지 살아야 해서 @Singleton Reservations가 목록과 알람을 함께 맞춘다
│  ├ domain/  Reservation(다음 알림 시각, 순수 Kotlin + 서울 시각 고정), RepeatLabel(안 함·매일·주중·주말·화~토), Reservations
│  ├ data/    폰 안 저장(한 키 JSON, 서버로 보내지 않음)
│  ├ alarm/   AlarmManager(setAlarmClock). 저장할 때 알림 허용 → "알람 및 리마인더" 순서로 확인하고, 둘 중 하나라도 꺼져 있으면 저장하지 않는다(iOS와 같음: 알람만 울리고 카드가 안 보이는 예약을 만들지 않음). 알림은 가이드에서 한 번 거절했으면 다시 묻고, 두 번 거절했거나 앱·예약 채널 알림을 껐으면 알림 설정 화면으로, 알림은 10분 전부터 계속 떠 있는 카드(개발자 결정: 울릴 때 한 번 소리·팝업, 나갈 시각까지 시스템이 거꾸로 셈, Live Update 칩, 탑승 시작·5분 뒤 다시·닫기, 나갈 시각 10분 뒤 저절로 내림), 재부팅·시각 변경 뒤 다시 걸기
│  └ ui/      예약 시트(반복은 iOS 알람처럼 요일 목록), 입체 순환선 시계판(5분 단위만), 홈 예약 목록. 홈은 자리(slot)만 준다. 홈에는 시각순 4개까지(개발자 결정: 많으면 홈이 길어짐), 넘치면 "모두 보기" → 전체 목록(설정처럼 옆에서 밀려 들어옴). 줄을 왼쪽으로 밀면 수정·삭제(iOS 목록처럼, 낭독기는 사용자 지정 동작)
└ ride/      진행 중인 탑승. 화면이 사라져도 계속돼야 해서 @Singleton RideTracker가 갖는다
   ├ domain/  RideTracker(앱 전체가 같이 보는 탑승 상태), 모델, 저장소 인터페이스
   ├ data/    서버 탑승 API
   ├ motion/  가속도 → 출발·도착 사건(순수 Kotlin, 합성 신호로 테스트). 기준값은 MotionRules 한곳(실기기로 다시 맞출 것)
   ├ service/ 포그라운드 서비스(갱신·센서·진동·CPU 깨우기) + 잠금화면 진행 중 알림(Android 16 Live Update)
   └ ui/      탑승 화면, 화면·알림이 같이 쓰는 문구(RideTexts)
```
- 탑승 화면은 이번정거장처럼: 경로 구간 칩(태평 ▶ 선릉, 선릉 ▶ 강남) + 가로 노선도(`StripCard`)에서 서버가 골라 둔 다음 열차를 칠하고, 다른 열차를 누르면 바꾼다. 노선도는 화면만 써서 `RideViewModel`이 15초마다 받는다(탑승마다 새 ViewModel, 끝나면 그만). 안내 띠(`RideTexts.banner`)가 골라 둠·탑승 확인·놓침·바꿈을 알린다(iOS와 같은 문구)
- 의존 방향: ui → domain ← data. domain은 Android를 모른다(RideTracker도 순수 Kotlin + 주입된 Clock).
- ViewModel은 Context를 모른다. 서비스 시작은 `RideServiceLauncher` 인터페이스로.
- 화면 이동 라이브러리 없이 "탑승 중이면 탑승 화면, 아니면 홈"으로 고른다(화면이 둘뿐).
- 예약 결정(개발자): 기본은 한 번(오늘만, 지난 시각이면 저장 막음), 반복은 고른 요일마다 끌 때까지(종료일 없음), 시각은 5분 단위, 미리 알림 5·10·15·20분(기본 10). 한 번짜리는 울린 뒤 꺼진 채 목록에 남고, 다시 켜면 오늘 지났으면 내일. 알림의 "탑승 시작"은 앱을 열어 그 경로로 바로 시작한다(앱이 앞에 있어야 포그라운드 서비스를 띄울 수 있음)
- 예약 확인: 저장 뒤 `adb shell dumpsys alarm | grep -A2 RESERVATION_ALERT`로 걸린 시각, 권한은 `adb shell appops get com.taektoy.nowstation SCHEDULE_EXACT_ALARM`. 알림을 한 번 거절한 상태는 `pm revoke ... POST_NOTIFICATIONS` + `pm set-permission-flags ... user-set`(두 번 거절은 `user-fixed`까지)
- 앱을 새로 켜면 런치 애니메이션(`core/ui/LaunchIntro`: 시작 화면 아이콘 → 홈 머리의 노선 그림, 홈은 머리 높이·투명도만 따른다) 뒤 처음 한 번 가이드가 올라온다
- 로직 없는 UseCase는 만들지 않는다. ViewModel → 저장소/RideTracker 직접.
- 시간은 `Clock` 주입.

## 작업 방식
- 커밋은 기능 하나씩. 피드백 반영 커밋은 무엇을 반영했는지 쓴다. squash·amend·force push 금지.
- 버그를 고치면 고치기 전에 실패하는 테스트로 고정한다.
- 테스트: ViewModel·RideTracker는 가짜 저장소, 저장소는 MockWebServer(서버와 같은 응답 모양), 화면은 에뮬레이터를 adb로 눌러 확인.
- 움직임 확인: `adb emu sensor set acceleration x:9.77631:0.812349`로 x에 ±0.9를 10초 넣고 `adb logcat -s RideService`
- 화면 잠금 확인은 `adb shell locksettings set-pin 1234` → 끝나면 `locksettings clear --old 1234`
- 에뮬레이터 한글 입력: 키보드가 한글 두벌식이라 `adb shell input text`에 자판 키를 넣는다(태평 = xovud, 강남 = rkdska). BACK 키는 앱을 닫을 수 있어 쓰지 않는다.
- 주석은 결정과 이유만.
