# Codex Meter 2.8.3 UI v3 — 구현 및 검증 보고


## A. 실제 UI 구성

| 위치 | 2.8.3 구현 |
| --- | --- |
| 메인 | ‘내 사용량 한눈에’ → 작은 ‘내 구독 가성비’ → ‘한도 상세보기’. 같은 한도 카드를 아래에 반복하지 않는다. |
| 요약 카드 | 서버에 존재하고 표시가 켜진 주간 → 월간 → 5시간 순으로 주요 한도를 선택한다. 잔여율·진행 막대·현재 구간 날짜 한 번·초기화까지 시간·오늘 관측 증가량·마지막 정상 조회 시각을 표시한다. |
| 메인 새로고침 | 현재 화면에서 API 조회·기존 자동/수동 원장 저장·요약 갱신을 수행한다. 요청 중 버튼을 비활성화하고 프로세스 공통 AtomicBoolean으로 반복 터치를 막는다. 성공·조회 실패·원장 저장 실패를 구분한다. |
| 한도 상세보기 | 스크롤 가능한 상세 대화상자에 기존 한도·추가 한도·사용량 크레딧·Reset Credit 정보를 표시한다. 닫으면 간결한 메인으로 돌아간다. 기존 표시·순서 설정을 읽는다. |
| 사용량 분석 | 정상 진입점을 요약 / 추세 / 기록 / 가성비로 통일한다. 옛 UsageHistoryActivity 일반 진입도 통합 화면으로 이동한다. 기존 고급 구간 차트는 추세 탭의 별도 버튼으로 보존한다. |
| 추세 | 기본은 이번 실제 한도 구간. 이전 구간 / 최근 7일 / 최근 30일은 별도 선택한다. 선택 막대 강조와 차트 아래 날짜·%p·측정 상태 표시를 제공한다. 30일 차트는 가로 스크롤로 읽는다. |
| 기록 | 오늘·선택 날짜 테두리, 측정 강도 색상, 선택 날짜 상세를 제공한다. 30일 목록을 달력 아래 반복하지 않는다. 원시 관측 목록은 날짜 상세 버튼으로 펼친다. 달력 날짜 높이는 48dp이고 좁은 화면은 가로 스크롤로 폭을 확보한다. |
| 가성비 | 직접 입력한 실제 결제액·통화·시작일·종료일, 해당 한도의 사용률·관측 증가량·측정 범위, 같은 경과 시간의 이전 구간 비교, 금전 추정 보류 이유를 구분한다. |
| Reset Credit | 앱·위젯·잠금화면·알림의 실행 진입을 제거했다. 잔여 횟수·만료일·조회·기존 변화 기록·만료 알림은 유지한다. 직접 실행은 ChatGPT에서 한다. |

요약 카드 표시 설정을 새로 분리했다. 옛 `showDashboardUsageHistory=false` 때문에 새 요약이 사라지지 않으며, 요약의 새 기본값은 표시다. 이전 설정 키를 삭제하지 않는다. 모든 한도를 숨겼을 때 임의로 다시 켜지 않고 표시 설정 안내를 보인다.

기존 Ui의 카드 모서리·테마·여백을 LedgerUi 구성 요소에서도 사용한다. 숫자 강조·상태 배지·버튼·좁은 화면 및 큰 글꼴 세로 배치·다크모드 색상·차트 접근성 이동 동작을 연결했다. **실제 Android 화면의 잘림, 글꼴 크기, 대비, TalkBack 체감 품질은 미검증이다.**

## B. 가성비의 계산과 한계

실제 결제액은 플랜의 가정 가격으로 채우지 않는다. 사용자 입력 금액을 BigDecimal로 저장하고 ISO 통화 코드와 날짜 범위를 검증한다. 결제 주기는 시작일 포함·종료일 제외이며 서버 주간 초기화 구간과 별개다. 기간이 끝났으면 입력 기간이 현재가 아님을 표시한다.

기존 PlanPricing에는 월 USD 700 / 3,500 / 14,000이라는 고정 가정과 주간÷4·5시간 추가÷6 계산이 있었다. 검증 가능한 금액 출처가 저장소에 없어 **활성 화면에서 금액 환산을 사용하지 않는다.** 호환용 계산 클래스와 기존 테스트는 보존했으므로 테스트 로그의 옛 pricing demo는 제품의 확정 비용을 뜻하지 않는다. 플랜 최대 이론 금액·관측 환산액·실제 API 비용·절약액 모두 이번 버전에서 확정 금액으로 제시하지 않는다.

공식 안내도 한도 소비가 모델·문맥·작업 특성에 따라 달라진다고 설명한다. 따라서 사용률×가정 금액이나 ‘70배 이득’을 가성비 판단으로 사용하지 않는다. 실제 토큰과 같은 결제 기간의 비용 근거가 없어 `금액 환산 근거 부족`으로 표시한다. PC 토큰 수집기는 구현하지 않았다.

출처: https://help.openai.com/en/articles/11369540-using-codex-with-your-chatgpt-plan

실제 결제액은 `codex_subscription_cost` 로컬 Preference에 계정 ID+플랜의 SHA-256 키로 격리한다. 토큰·계정 ID 원문을 결제 설정에 저장하거나 원장 내보내기에 포함하지 않는다. 로그아웃하면 접근할 수 없고 같은 계정/플랜으로 돌아오면 보존된 설정을 읽는다. 사용 기록 삭제는 원장에만 적용되며 별도 결제 설정은 유지한다. 저장 직전에 NETWORK_LOCK 안에서 현재 계정·플랜을 확인하고 commit 실패는 성공으로 표시하지 않는다.

주간 구간 시작은 `서버 reset 시각 - 서버 limit 길이`로 계산한다. 초기화 시각이나 길이가 없으면 구간을 만들지 않는다. 이번/이전 비교는 같은 정책·같은 길이·같은 경과 시간에 한정한다. 유효 관측 3회 이상, 경과 시간 80% 이상의 실제 관측 간격, 공백·보정 경계 없음 조건을 충족해야 비교한다. 조기 변경으로 구간이 겹치거나 플랜 변경·측정 공백이 있으면 보류한다. 이 조건도 완전 관측을 증명하지 않으므로 ‘부분 관측’을 유지한다.

날짜별 집계는 기존 LedgerAggregation 결과를 사용한다. 자정을 넘는 불확실한 증가를 날짜별 실측값으로 나누지 않는다. 전체 서버 구간의 정상 관측 차이는 날짜 배분과 별개로 합산하며 다른 한도의 %p와 합산하지 않는다. 현재 잔여율/사용률은 %, 차이는 %p다. `—`는 자료 부족, 0 %p는 비교 가능한 구간에서 실제 변화 없음이다. 예측 엔진과 SQLite 스키마·보존·이관 정책은 변경하지 않았다.

## C. 5시간 표시 정책과 위젯

표시 필터는 저장된 데이터·위젯 선택 CSV·디자인·순서·색상·투명도·크기를 바꾸지 않는다. OFF면 실제 데이터가 있어도 5시간 키를 렌더링 후보에서 제외한다. 빈 슬롯·아이콘·게이지·‘확인불가’도 제거하고 남은 슬롯으로 배치한다. 다시 ON이면 이전 선택을 재사용한다. ON이어도 정상 API 스냅샷에 5시간 한도가 없으면 기본 위젯에서 제외한다. 네트워크 실패 시 캐시 관측을 사용할 수 있으나 freshness 경고를 강제로 표시한다.

연결 범위: 메인 및 한도 상세, 통합 분석과 옛 고급 차트, 홈 위젯 9개 스타일의 공통 resolver, 위젯 설정 미리보기, 삼성 잠금화면/AOD 위젯 및 미리보기, Now Bar/Live Update 한도 선택, Phone→Wear 선택적 `show_five_hour` 필드, Wear 앱·타일·complication·ongoing monitor 표시 경로. 위젯 및 Now Bar는 설정 변경 즉시 갱신 요청을 받는다. AOD는 삼성 잠금화면 공급자 경로를 공유하지만 실제 삼성 동작은 미검증이다.

Wear에는 원본 스냅샷을 유지하고 표시용 스냅샷에서만 5시간을 제외한다. 기존 wire payload에 필드가 없으면 기존처럼 ON이다. 새 표시 정책 적용을 위해 **Wear도 2.8.3으로 업데이트해야 한다.** 5시간 전용 complication은 OFF면 NoData이고 복합 complication은 주간만 표시한다. 공급자 ID는 유지해 재활성화 선택을 보존한다. 데이터 수집·기록·기존 부족 알림의 별도 설정은 유지한다.

## D. 변경 파일과 보호 범위

주요 변경 파일은 아래와 같으며 전체 목록은 `CHANGED_FILES_2.8.3.txt`에 기록한다.

- MainActivity, LedgerDashboard: 메인 중복 제거, 한도 상세, 요약 설정 분리, 현재 화면 새로고침.
- LedgerAnalyticsActivity, LedgerTrendView, UsageHistoryActivity: 통합 탐색, 실제 서버 구간/이전 비교, 차트 선택, 달력 중심 날짜 상세, 고급 차트 보존.
- SubscriptionUi, SubscriptionStore, SubscriptionCost: 실제 결제 설정·계정/플랜 격리·확정 금전 환산 보류.
- LedgerPeriods, V3Display: 서버 구간·동일 경과 시간 측정·서울 날짜 표시.
- WidgetVisibility, MeterVisibility, AppPreferences: 비파괴 표시 필터 및 즉시 표면 갱신.
- WidgetRenderer, WidgetConfigActivity, WidgetOptions, SamsungLockWidgetSupport, LockWidgetConfigActivity: 슬롯 축소, 미리보기, 기존 디자인 보존, Reset 실행 제거.
- NowBarManager, NowBarPreferences, ResetCreditActivity, ResetNotificationManager: 전역 표시 반영과 크레딧 정보 전용 UI.
- PhoneWearSync, WearUsageState, WearPreferences, WearPhoneSync, WearMainActivity, CodexTileLayouts, CodexComplicationService, FiveHourComplicationService, DualUsageComplicationService, WearOngoingMonitor: 원본 wire 보존 및 Wear 표시 필터.
- 영어/한국어 ui_v3_strings.xml: 새 문자열 40개씩. 기존 문자열·영어 fallback 유지.
- app/wear Gradle, AppConstants, build.sh: 2.8.3/33 및 User-Agent 일관화. 패키지 `dev.bennett.codexmeter`·namespace·키/alias 유지.
- run-tests.sh와 verify-* 및 UiV3SelfTest/SubscriptionStoreSelfTest: 바뀐 UI 계약과 새 구간·가격·표시·저장 테스트. 기존 제거 대상 버튼 존재 검사는 부재 검사로 변경.
- CHANGELOG, RELEASE_NOTES 및 본 보고서/설치 안내: 변경 범위·검증 한계·전달 안내.

OAuthService, SecureTokenStore, UsageApi, UsageParser, UsageLedgerDatabase, AccountSession, RefreshScheduler, UsageHistoryRecorder, SettingsTransferStore, SettingsActivity, LedgerAggregation, LedgerForecast는 시작 커밋과 동일함을 source guard로 확인했다. MainActivity 로그인/로그아웃과 AppPreferences 데이터 삭제 메서드, 분석 수동 조회/SAF 내보내기 메서드도 시작 커밋과 동일한지 검사했다. 인증·서버 주소·요청 형식·원장 DB 구조에 불필요한 변경이 없다.

## E. 실행한 검증

JAVA_HOME은 기존 JDK 21, SDK는 기존 Android SDK, 기존 vendored dependency 및 로컬 캐시를 사용했다.

```text
cd android
bash -x ./run-tests.sh
bash ./gradlew :app:assembleRelease :app:lintRelease :wear:assembleRelease :wear:lintRelease --offline --console=plain
cd ..
python android/tests/verify-localization.py
adb devices -l
apksigner verify --verbose --print-certs <각 최종 APK>
aapt dump badging <최종 휴대폰 APK>
Get-FileHash -Algorithm SHA256 <각 최종 APK>
git diff --check
```

| 검증 | 최종 결과 |
| --- | --- |
| 기존 parser/updater/OAuth/onboarding/위젯/Wear self-test | 통과 |
| UsageInsights·이벤트 | 2,181 assertions 통과 |
| 기존 원장·recorder·persistence | 67 통과 |
| NEXT 계산·capture·forecast·export | 532 통과 |
| production DAO + SQLite JVM fixture | 73 통과 |
| 계정·job lifecycle JVM fixture | 16 통과 |
| 표시 안전성 및 핵심 메서드 불변 검사 | 14 통과 + source guards 통과 |
| UI v3 구간·결제 단위·위젯 ON/OFF/복원/빈 상태/옛 설정/크기·Wear wire | 60 통과 |
| 실제 SubscriptionStore + 파일 기반 Preference fixture | 13 통과: 재로드·계정/플랜 격리·로그아웃·쓰기 실패·손상 데이터 |
| 영어/한국어 | 앱 1,000 문자열·Wear 116 문자열 parity, formatter 629 통과, 보호 파일 37·검토 변경 9 확인 |
| 휴대폰·Wear Release 빌드 | 성공 |
| lint | 성공. 휴대폰 88 warnings·2 hints, Wear 2 warnings. 기존 baseline의 휴대폰 60 errors·111 warnings가 필터됨. baseline 파일은 수정하지 않음. |
| 버전·서명 | 두 APK 2.8.3/33, 기존 한국어 APK와 인증서 일치, APK v2 검증 성공 |
| ADB·에뮬레이터 | 연결 기기 없음, SDK emulator/system-images 없음 |

중간 실패는 달력 HorizontalScrollView 참조 및 의도적으로 제거한 옛 중복 카드/실행 버튼을 요구하던 소스 검사였다. 참조와 검사 계약을 수정한 후 최종 빌드·전체 테스트가 통과했다. 초기 UI 작업에서 잘못된 orientation 상수도 수정했다. 기존 baseline 오류는 은폐하거나 baseline을 재생성하지 않았다. 이전 UI v2 lint의 78 warnings 대비 휴대폰에서 늘어난 10개는 보존한 옛 문자열의 UnusedResources이며 다른 새 warning 유형은 추가되지 않았다. Wear는 기존 PluralsCandidate 1개에 옛 복합 사용량 설명의 UnusedResources 1개가 추가됐다. 기존 unchecked·deprecated API 경고는 남아 있다.

JVM 위젯 설정/JSON 재로드는 기기 재부팅·실제 위젯 크기 조절 테스트를 대체하지 않는다. SQLite fixture는 Android 강제 종료의 증거가 아니다. 네이티브 UI 테스트 환경이 없으므로 화면 렌더링·스크린샷 검증을 완료했다고 주장하지 않는다.

## F. 최종 APK와 전달

전달 폴더: `delivery/2.8.3-ui-v3/`.

| APK | 크기(bytes) | SHA-256 |
| --- | ---: | --- |
| CodexMeter-2.8.3-ko-ui-v3.apk | 17,867,925 | `89ce0b28967dc5409da291cf483a54283a12233fd0fb6246a66b948013f5f7f8` |
| CodexMeter-Wear-2.8.3-ko-ui-v3.apk | 15,227,294 | `dd7c436f10b9e83a5b982210e191585b338905243e57c426d60eba4648ea1c59` |

공통 패키지: `dev.bennett.codexmeter`. 휴대폰 minSdk 26, Wear minSdk 30. 서명 인증서 SHA-256: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`. 기존 로컬 2.8.1/31·2.8.2/32와 인증서가 일치한다. **휴대폰에 실제 설치된 앱의 버전·인증서는 비교하지 못했다.** 다른 개발자의 서명 또는 더 높은 설치 버전이면 업데이트를 진행하지 않는다.



로컬 SHA-256은 최종 APK의 값이다. Drive metadata는 원격 SHA를 제공하지 않으므로 원격 바이트 해시까지 확인했다고 주장하지 않는다. 업로드 후 이름·크기·폴더 포함·can_download를 다시 읽어 확인하고 전달 manifest에 기록한다. 공개 공유 권한 변경은 하지 않는다.

## G. 실기기 확인 항목 및 남은 문제

실제로 수행한 기기 테스트는 없다. 사용자가 업데이트 후 확인할 항목은 로그인/기록 유지, 실제 API 조회 및 메인 새로고침, 사용량 분석/날짜 선택, CSV·JSON 파일 생성, 결제액 입력 후 재실행, 5시간 OFF/ON의 기존 홈·잠금화면 위젯 즉시 반영, Now Bar, Wear 동기화, 큰 글꼴/다크모드/회전이다. 자정·Doze·오프라인·장시간 백그라운드도 실환경 검증이 필요하다.

- P1: 설치본 인증서·버전 비교와 실기기 UI/삼성 표면 검증 미완료. 설치 실패 시 앱 제거/데이터 초기화 금지, 오류 문구와 설치 버전부터 확인한다.
- P2: 금전적 가성비는 토큰·비용·동일 결제 기간 근거가 부족하여 미산출이다. 대신 실제 결제액과 검증 가능한 한도 활용도를 표시한다.
- P2: 기존 lint baseline 오류·경고와 Gradle/unchecked 경고는 별도 유지보수 대상이다.
- P2: 일반 UI에서는 통합했지만 기존 고급 차트는 하위 화면에 보존했다. 모든 옛 기능을 새 탭 안으로 완전히 다시 구현하지 않았다.

Phase 3 문서는 그대로 보존하고 새로운 수집기·서버·SDK·유료 호출은 추가하지 않았다.

## H. 판정

**BUILD READY**. 테스트·Release·버전·서명·체크섬 검증 완료. **INSTALL READY 전달 준비 완료, 실제 설치본 비교는 미완료**. 기존 한국어판 2.8.1/2.8.2와 같은 인증서라는 로컬 근거는 있으며 APK와 비파괴 설치 안내를 제공한다. **DEVICE VERIFIED 아님**. UI 체감 완성도와 삼성 표면의 실제 동작은 최종 사용자 기기 확인이 필요하다.
