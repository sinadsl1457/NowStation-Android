# NowStation Android (지금역)

지하철에서 **지금 무슨 역인지**를 잠금화면에 보여 주는 앱입니다. 타는 역만 고르고 시작하면, 서버가 탄 열차를 찾아 따라가고 앱은 현재역·다음역을 보여 줍니다.
판단(탄 열차 찾기·환승)은 서버가 합니다 → [NowStation-Server](https://github.com/sinadsl1457/NowStation-Server) (전체 구조도는 서버 README)

> 진행 중(2026-11-16 ~ 11-22). 3일차: 홈·탑승 화면, 서버 연결, 잠금화면 진행 중 알림 / 4일차: 움직임 감지, 하차 진동

## 코드 지도

```
app/src/main/java/com/nowstation/app/
├ MainActivity.kt          탑승 중이면 탑승 화면, 아니면 홈
├ core/                    결과·오류 타입, 네트워크(Retrofit), 테마
├ station/                 역 검색 + 홈 화면(타는 역·내릴 역 고르고 시작)
└ ride/                    진행 중인 탑승
   ├ domain/RideTracker.kt   앱 전체가 같이 보는 탑승 상태(화면·서비스·알림)
   ├ data/                   서버 탑승 API
   ├ service/                포그라운드 서비스(5초마다 받아 옴) + 잠금화면 진행 중 알림
   └ ui/                     탑승 화면, 화면·알림 공용 문구
```

## 동작
1. 홈에서 타는 역(필수)·내릴 역(선택)을 고르고 시작 → 서버에 탑승 생성, 포그라운드 서비스 시작
2. 서비스가 5초마다 서버에서 받아 와 진행 중 알림을 갱신(앱을 닫아도 계속)
3. 지하에서 연결이 끊기면 마지막 역을 그대로 두고 "연결 끊김(다시 시도 중)"
4. 알림의 [종료]나 화면의 [탑승 끝내기] → 서버에도 알리고 서비스 종료

## 실행
```bash
# 1. 서버를 켠다(NowStation-Server: ./gradlew bootRun)
# 2. 앱(에뮬레이터는 10.0.2.2:8080으로 내 컴퓨터의 서버에 붙는다)
./gradlew installDebug
./gradlew testDebugUnitTest
```
실기기에서 쓰려면 `local.properties`에 `API_BASE_URL=http://<서버 주소>:8080/`.
