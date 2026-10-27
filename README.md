# 클래스 냥이 (ClassCat)

🐾 학교 수업을 들은 시간만큼 고양이가 성장하는 귀여운 공부 기록 앱!

수업을 시작할 때 버튼을 누르고, 끝나면 다시 누르세요. 들은 시간만큼 경험치가 쌓이고, 레벨이 오르면 고양이가 진화해요.

![진화 단계](docs/images/cat_stages.png)

---

## 📱 주요 기능

- **수업 시작 / 종료 타이머**
  - 과목 이름을 적고 시작할 수 있어요 (선택)
  - 앱을 꺼도 타이머는 계속 흘러가요 (시작 시각을 저장해 두기 때문)
  - 1분 미만의 수업은 기록하지 않고, 6시간이 넘으면 종료를 깜빡했는지 한 번 더 물어봐요
- **고양이 성장 시스템**
  - 수업 1분 = 1 XP
  - Lv.1 → Lv.2 는 1시간, Lv.2 → Lv.3 은 2시간 … 레벨 L 에서 다음 레벨까지 `60 × L` XP (최대 Lv.30)
  - 경험치는 수업 기록에서 계산하기 때문에 기록을 지우면 경험치도 함께 줄어요
- **진화 시스템**: 아기 냥이 → 새내기 냥이(Lv.3) → 우등생 냥이(Lv.6) → 장학생 냥이(Lv.10) → 박사 냥이(Lv.15)
- **털 색(스킨) 커스터마이징**: 레벨이 오르면 새 털 색이 해금돼요

  ![털 색](docs/images/cat_skins.png)

- **수업 기록**: 최근 7일 그래프, 연속 공부 일수, 총 공부 시간, 수업별 기록 (길게 눌러 삭제)
- **오늘의 목표**: 하루 목표 시간을 정하고 달성률을 확인해요
- **수업 시작 알림**: 매일 정해진 시간에 고양이가 불러 줘요 (재부팅 후에도 유지)
- **설정**: 고양이 이름, 하루 목표 시간, 알림 시간, 기록 초기화

---

## 🗂️ 화면 구성

| 탭 | 내용 |
|:--|:--|
| 🏠 홈 | 고양이 인사, 수업 타이머(시작/종료), 오늘 공부 시간과 목표 달성률 |
| 🕘 기록 | 최근 7일 / 연속 공부 / 총 공부 요약, 7일 막대 그래프, 수업 기록 목록 |
| 🐾 고양이 | 레벨·경험치, 진화 단계, 털 색 고르기 (고양이를 눌러 보세요!) |
| ⚙️ 설정 | 이름, 하루 목표, 수업 알림, 데이터 초기화, 버전 |

---

## 🛠️ 사용 기술

- **Android (Java)**, minSdk 24 / targetSdk 34
- **XML 레이아웃** + Material Components (Material 3)
- **Fragment + BottomNavigationView** 로 4개 탭 구성
- **LinearProgressIndicator** 로 XP·목표 진행률 표시
- **RecyclerView (ConcatAdapter + ListAdapter)** 로 기록 목록
- **SharedPreferences** 에 고양이 상태와 수업 기록 저장 (JSON)
- **AlarmManager + BroadcastReceiver** 로 매일 수업 알림
- 고양이 그림은 모두 **벡터 드로어블**: 몸통(털 색으로 tint) · 줄무늬 · 얼굴 · 진화 소품을 겹쳐서 그려요

## 🧱 코드 구조

```
app/src/main/java/com/example/campuscat/
├── MainActivity.java          하단 탭 + 툴바
├── domain/                    안드로이드에 의존하지 않는 순수 로직 (단위 테스트 대상)
│   ├── LevelSystem.java       XP ↔ 레벨 계산
│   ├── CatStage.java          진화 단계
│   ├── CatSkin.java           털 색과 해금 레벨
│   ├── StudySession.java      수업 한 번의 기록
│   ├── StudyStats.java        날짜별 합계, 연속 공부 일수 등 통계
│   ├── ReminderTime.java      다음 알림 시각 계산
│   └── TimeFormatter.java     "1시간 30분", "01:23:45" 같은 표시용 문자열
├── data/CatRepository.java    SharedPreferences 저장소
├── ui/                        화면 (Home / Records / Cat / Settings) 과 CatView, WeekBarChartView
└── reminder/                  알림 예약, 알림 표시, 재부팅 후 재예약
```

## ▶️ 빌드 & 테스트

```bash
./gradlew assembleDebug        # 디버그 APK → app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # 레벨·통계·시간 계산 단위 테스트
```

GitHub Actions(`.github/workflows/android.yml`)가 푸시할 때마다 빌드, 단위 테스트, lint 를 돌리고 디버그 APK 를 아티팩트로 올려요.

---

## ⚡ 향후 추가 예정

- 시간표 등록 (요일·교시별 수업 자동 알림)
- 고양이 소품 상점 (모은 XP로 아이템 구매)
- 홈 화면 위젯

---

## 👤 개발자

- **개발자 이름** : 타이머스
- **GitHub** : https://github.com/lwg5421
