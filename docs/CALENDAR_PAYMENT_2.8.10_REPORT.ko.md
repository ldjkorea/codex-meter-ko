# Codex Meter 2.8.10 — 구독료·기록 캘린더 검증

2026-10-09 · 2.8.10 / versionCode 40 · dev.bennett.codexmeter

## 실제 확인 결과

구독료 등록은 장식이 아니다. SubscriptionUi → SubscriptionStore의 계정·플랜별 로컬 저장 → SubscriptionValueUi → SubscriptionValue.calculate로 연결된다. 저장한 금액·통화·결제 기간을 재로드한 뒤 해당 한도 구간 길이/결제 기간 길이로 배분하고 현재 사용률을 적용한다. 금액을 두 배로 수정하고 다시 읽으면 계산된 금액도 정확히 두 배로 바뀌는 새 테스트가 통과했다. 홈의 렌더링 캐시에는 구독료 Preference 변경 값이 포함되어 설정에서 돌아오면 재계산되며, 가성비 상세도 onResume에서 다시 로드한다. 기간 불일치·오래된 관측·불확실한 구간은 금액 확정을 보류한다. 절약액·API 비용·실제 토큰 비용으로 표시하지 않는다.

캘린더도 장식이 아니다. 자동/수동 조회 → 기존 UsageLedgerDatabase.record → SQLite 일별 집계 → load().days → policy와 서울 날짜로 LedgerPresentation.find → 날짜 셀·선택일 요약·상세 관측 기록으로 연결된다. 실제 SQLite 브리지에서 초기 관측, 일별 증가, 중복 수동 저장, DB 재오픈, 없는 날짜, 다른 한도 격리를 검증했다. 원본 집계·DB 스키마·원시 기록·내보내기·로그인 코드는 변경하지 않았다.

## 변경한 UI

확인된 하루 주간 한도 소비량이 높은 날짜를 밝은 에메랄드 배경과 정적인 빛 테두리로 강조한다. 강조는 선택된 주간 정책의 실제 일별 증가분만 사용한다. 자정 경계·측정 공백의 미확정 증가분, 다른 한도, 누적 사용률, 관측값 한 개는 조건에 넣지 않는다. 선택 테두리·오늘 표시·TalkBack 날짜/사용량/측정 상태를 유지한다. 기존 색 강도는 다른 날짜와 한도에 유지하며 실제 0과 관측 없음도 구분한다. UI와 일반 변경 안내에 내부 강조 기준은 노출하지 않는다.

글자와 에메랄드 내부 배경 대비 4.97:1. 정적 Drawable로 구현해 애니메이션·추가 서버 호출·외부 라이브러리·비트맵·소프트웨어 레이어를 추가하지 않았다. Android 실제 렌더링 확인과 같은 뜻은 아니다.

## 검증

- `bash android/run-tests.sh`: 성공. 기존 3,472개 명시적 계산/저장 assertion에 새 31개를 추가해 3,503개 통과. 파서·업데이트·OAuth·위젯 등 기존 별도 검사도 통과.
- `python android/tests/verify-localization.py`: 성공. 앱 한국어/영어 문자열 1,328개, Wear 116개. 별도 포맷터 629개와 5시간 표현 44개 assertion 통과.
- `bash gradlew :app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease --offline --console=plain` (android 폴더): 성공.
- `verify-calendar-ui.py`: 날짜 선택/이전다음 달/상세 기록/실제 금액 연결/접근성/텍스트 대비 소스 검사 성공.
- `verify-publication.py` 최종 phone/Wear APK: 성공. 공식 폰트·출처·라이선스·금지 폰트 해시 검사 통과.
- `verify-precision-crests.py` 최종 phone APK: 10개 기존 자산 패키징과 18개 보호 계약 그대로 통과.
- APK 서명 검사: APK v2, signer 1개, 기존 한국어판 인증서와 일치.
- lint: 신규 오류 없음. 기존 phone 경고 146개, Wear 경고 9개. phone baseline의 기존 오류 53개·경고 111개 필터 유지. baseline 파일 변경 없음.

새 검사에서 캘린더 글자 대비 4.41:1을 발견해 배경을 수정하고 4.97:1로 재검증했다. 새 공유 파일이 과거 원본 비교 대상에 잡힌 경우는 신규 파일을 명시적으로 분리하고 새 동작 검사를 추가해 해결했다. 기존 소스 fixture와 보호 대상 해시는 갱신하지 않았다.

## 변경 파일

- app: LedgerAnalyticsActivity.java, CalendarGlowDrawable.java 및 en/ko calendar_strings.xml, ledger_ui_strings.xml — 일별 데이터로 강조·접근성 표시 연결.
- shared: LedgerCalendar.java — 측정된 일별 데이터의 표시 기준만 담당.
- tests: CalendarSelfTest.java, CalendarDatabaseSelfTest.java, SubscriptionStoreSelfTest.java, verify-calendar-ui.py 및 기존 테스트 실행 연결 — 영속 저장→표시/계산 경로 확인.
- 버전: app/wear Gradle, AppConstants, build.sh, 실행 검사 버전, CHANGELOG, 한국어 Release Notes, README — 2.8.10/40 일관화.
- 기존 LedgerAggregation, UsageLedgerDatabase, SubscriptionStore/Cost/Value, OAuth, API, 위젯, Wear 구현, 서명키는 보존.

## 최종 APK

- CodexMeter-2.8.10-ko.apk: 25,799,302 bytes · SHA-256 `63cf9eda83f9073f5d9f5d51db094c88e0d735be5c61d50b43ec307a72e7bfe7`
- CodexMeter-Wear-2.8.10-ko.apk: 15,862,093 bytes · SHA-256 `e422cf32d35dbbb9a81143c84545851397a69e1e729e3ec731d5f50a34744200`

서명 인증서 SHA-256: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`.

## 한계와 설치 조건

ADB 연결 목록이 비어 있어 실제 갤럭시 설치·터치·큰 글씨·다크모드 렌더·회전·실사용 API·캘린더 표시·Doze를 검증하지 못했다. 테스트 DB는 별도 임시 SQLite이고 설치된 앱의 데이터를 읽거나 변경하지 않았다. 실제 기기 서명 비교도 미완료다.

BUILD READY. INSTALL READY는 동일 한국어판 패키지/서명과 더 낮은 설치 versionCode를 전제로 한 조건부 준비다. DEVICE VERIFIED는 아니다. 기존 앱은 삭제하지 않고 업데이트하며 문제가 나도 데이터 삭제나 앱 제거로 해결하지 않는다. 게시 후 실제 공개 다운로드/업데이트 파서 검증 증거는 별도 최종 전달 보고서에 기록한다.
