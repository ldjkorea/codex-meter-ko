# Codex Meter 2.8.12 검증 보고서

## 결과 및 범위

최종 소스·전체 자동 검사·phone/Wear Release 빌드·APK 서명/버전/자산 검사를 완료했습니다. 실기기 연결 및 에뮬레이터가 없어 DEVICE VERIFIED가 아닙니다. 공개 다운로드·해당 커밋의 CI·업데이트 발견 검증은 게시 단계에서 별도 증거 파일에 기록합니다. 원본 BenItBuhner 저장소, 기존 APK 및 개인 emblem-concepts는 변경하지 않습니다.

## 실제 화면 변경

홈 순서는 휘장 → 내 사용량 한눈에 → AI의 한마디 → 내 구독 가성비입니다. 휘장은 같은 렌더링 컴포넌트를 홈·가성비 상세에서 공유합니다. 이전 장기 완료 구간 평가 이력은 삭제하지 않고 별도 역사로 유지합니다. 현재 티어와 과거 장기 티어 이력이 다를 수 있습니다.

평가 이유·금액 계산 버튼 및 말투 선택을 제거했습니다. 기존 설정값과 과거 코치 문장은 저장 호환성을 위해 보존하며, 현재 안내만 매운맛으로 고정합니다. 9가지 실제 상태 × 3개 문구를 네 시간 단위로 안정적으로 선택합니다. 이 문구는 로컬 규칙이며 유료 AI 호출이나 개인 대화 수집을 추가하지 않습니다.

계정 및 구독 카드 오른쪽에 요금제별 256px 투명 WebP 이미지 5개를 추가했습니다. 별도 등급 휘장이나 공식 OpenAI 로고로 오인되지 않는 원본 계산 코어 그림입니다. 출처/생성 프롬프트/해시는 docs/plan-artwork/ASSETS.json에 기록했습니다. 작은 폭과 큰 글꼴에서는 이미지 크기를 줄이고 본문을 줄바꿈합니다. 실제 Android 화면 렌더링은 미검증입니다.

## 활용률·금액·티어 기준

- 서버 주간 구간은 `resetAt − windowSeconds`부터 `resetAt`까지입니다. 서울 달력 주나 최근 7일·결제 주기와 섞지 않습니다.
- 현재 관측의 누적 카운터를 즉시 반영합니다. 앱 설치 전의 일별 사용량은 생성하지 않습니다. 같은 시각/같은 관측은 한 번만 계산합니다.
- 별도 결제액이 없을 때 티어는 현재 실제 한도 구간 종료를 끝으로 한 **4주 참고 기간**을 사용합니다. 월간 한도가 따로 있다면 그 실제 길이를 사용합니다. 4주를 서버의 실제 월간 한도라고 주장하지 않습니다.
- 등록한 결제 기간이 유효하면 그 기간을 기준으로 기간 내 소비 × 한도 길이 ÷ 결제 기간 길이로 활용률을 환산합니다. 28일 결제의 주간 50%는 12.5%, 30일 결제라면 약 11.67%입니다. 서로 다른 한도의 퍼센트를 합산하지 않습니다.
- 금액은 **실제 등록액 × 활용률 ÷ 100**입니다. 10만원·활용률 12.5%이면 12,500원입니다. 가상 API 최대 가격/배수·확정 절약액·실제 토큰 비용으로 표현하지 않습니다.
- 기록이 결제 시작을 가로지르면 날짜별로 임의 배분하지 않습니다. 그 안에 양쪽 끝이 있는 관측 변화만 계산하며, 아무 근거가 없으면 0이 아니라 관측 부족으로 표시합니다. 이전 주차 기록이 부족하면 일부 관측을 명시합니다.
- Iron/Bronze/Silver/Gold/Platinum/Emerald/Diamond/Master/Grandmaster/Challenger는 누적 활용률 0/10/20/30/45/60/75/85/92/97% 경계로 현재 관측에 바로 적용합니다. 현재 결제/참고 기간이 바뀌면 등급이 내려갈 수 있으며 기존 장기 티어 기록은 보존합니다.
- 동일 구간 정상 증가, 정기 초기화, 조기 구간 변경, 보정, 정책 변경을 구분합니다. 정기 초기화 근처의 전역 쿠폰 이벤트는 정기 초기화를 리필로 만들지 않습니다.
- 확인된 쿠폰 이벤트와 관측된 카운터 초기화가 연결된 경우 소비를 누적합니다. 같은 주간 용량 100%를 넘는 실제 추가 소비가 확인되면 최고 티어를 적용합니다. 100% → 확인된 리필 0% → 100%는 실제 200% 소비이며 4주 기준 50%입니다. 쿠폰만 쓰고 소비하지 않으면 200%를 만들어내지 않습니다.
- **ChatGPT 밖에서 실행한 쿠폰은 서버 데이터만으로 원인을 확정하지 못할 수 있습니다.** 이 경우 원인 미확인 변화로 취급하여 자동 쿠폰 보너스를 보류합니다. 앱 내부 초기화 실행 버튼은 다시 추가하지 않습니다.
- 오래된 데이터/만료 구간은 현재 금액·등급으로 확정하지 않습니다. 일간 소비와 달력은 기존 관측 변화만 사용합니다. 관측 공백이나 자정 소비를 임의 생성하지 않습니다.

## 저장·동시성 및 기존 기능 보존

새 계산은 메모리의 파생 캐시이며 DB 스키마·마이그레이션·보존 기간·인증·API를 바꾸지 않습니다. 기존 SQLite 원장/이전 Preference/서명키/결제 기록이 원본입니다. 프로세스 재시작 후 원장을 다시 읽습니다. 새 네트워크 호출이나 SDK를 추가하지 않습니다.

캐시는 암호화된 계정 변경 표시, 실제 관측 시각·정책·카운터, 결제 설정, 서울 날짜, 기록 삭제 표시에 따라 무효화합니다. 이전 계정의 비동기 계산이 새 계정에 게시되지 않게 검사합니다. 기록 삭제 뒤 기존 파생 캐시를 다시 쓰지 않으며 저장/읽기 실패 시 재시도합니다. 위젯 설정을 삭제하거나 초기화하지 않습니다. 홈 11종과 티어 진화형 위젯의 휘장에 같은 현재 등급을 씁니다. AOD/Now Bar/Wear의 기존 사용량 표면과 PC 별도 장기 티어 규칙은 확장하지 않습니다.

## 실행한 검증

- `bash android/run-tests.sh`: **3,827개 명시적 assertion** 및 기존 parser/OAuth/widget/source 검사 통과. 기존 3,542개에서 위젯 변경 감지 2개, 새 활용 엔진/문구 266개, 캐시 17개를 추가했습니다.
- `python android/tests/verify-localization.py`: app 1,368 / Wear 116 문자열, 영어·한국어와 포맷 인수 일치. 기존 포맷터 629개와 5시간 표현 44개 검사 통과.
- SQLite production DAO/이관/동시 기록/초기화/계정 분리 검사는 별도 가상 DB로 실행했습니다. Android 프로세스 또는 설치된 앱의 DB에 접근하지 않았습니다.
- 새 캐시 검사는 실제 LiveUsageStore를 고립된 JVM 환경에서 실행해 계정 변경·같은 시각의 정책 변경·구독 수정·삭제·이전 작업 결과 폐기·읽기 실패/재시도를 검증했습니다. Android OS 재시작/기기 증거는 아닙니다.
- `bash android/gradlew -p android :app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease --offline --console=plain`: 최종 소스로 성공.
- lint: phone Warning 166 / Hint 2, Wear Warning 9. 기존 baseline Error 53 / Warning 111 필터를 유지했고 baseline 파일은 바꾸지 않았습니다. 이전 phone Warning 146보다 20개 늘었으며 주로 화면에서 은퇴한 기존 리소스의 미사용 경고입니다. 새 Live 계산/AI 리소스에 lint 진단은 없습니다. 새 오류나 baseline 억제로 실패를 숨기지 않았습니다.
- `verify-publication.py phone.apk wear.apk`: 폰트·라이선스 archive 52개 통과. `verify-precision-crests.py phone.apk`: 기존 휘장 10개 실제 payload와 인증/원장/업데이터/기존 티어의 18개 보호 계약 통과. `verify-live-utilization.py phone.apk`: 새 요금제 이미지 5개 최종 ZIP payload 해시 통과.
- APK signer 1개 / v2 검증 통과. phone minSdk26, Wear minSdk30, package `dev.bennett.codexmeter`, 2.8.12 /42. 인증서 SHA-256 `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`: 기존 한국어판과 동일. 키/alias 변경 없음.
- 초기 추가 테스트 2개는 유효 관측 이전에 새 구간 시작을 놓은 가상 입력을 수정했습니다. Java 캐시 테스트의 람다 final 변수 오류를 고쳤습니다. 기존 fixture를 재생성하지 않았으며 모든 검사를 재실행했습니다.

## 변경 파일

MainActivity/FunHome/FunActivity/LiveCards: 휘장 순서·현재 즉시 등급·고정 AI·공통 표현. RecordSettings/PlanArtwork/SubscriptionUi: 요금제 그림·설명/말투 진입 제거. SubscriptionValueUi: 등록액에 비례한 결제 기간 누적 환산. LedgerAnalyticsActivity: 실제 서버 구간 누적 및 변화량 구분·삭제 캐시 갱신. LiveUtilization/SpicyAi/LiveUsageStore: 순수 계산·문구 선택·비동기 파생 캐시. TierTheme/WidgetCrest/WidgetRenderer/EvolutionWidget: 즉시 등급 및 기존 위젯 재그리기. drawable-nodpi/plan_*.webp, en/ko live_strings.xml, tests/LiveUtilizationSelfTest 및 verify-live-*와 기존 정확한 source guards: 새 동작/접근성·보존 검증. app/Wear Gradle/AppConstants/build.sh/run-tests/README/CHANGELOG/Release Notes: 2.8.12/42 동기화.

## 산출물

- `CodexMeter-2.8.12-ko.apk`: 25,926,111 bytes · SHA-256 `0d521321b2951fcb369460308bb44eedfcd2e5b31acb7cdd16e326642d6a4d4b`
- `CodexMeter-Wear-2.8.12-ko.apk`: 15,866,209 bytes · SHA-256 `e4df6b915fc50bb4409f73f21bc10836f45d123bade3f2280672dd986f1775e2`
- `LICENSE_NOTICES.zip`: 613,872 bytes · SHA-256 `719dd579081eaae6db71312b9246096960e4934851a409f9230d286a279f075c`

## 미검증 및 판정

ADB 연결 기기 없음, 에뮬레이터 없음. 실제 삼성 업데이트 설치·설치본 인증서·로그인 유지·OAuth·사용량 서버 조회·UI/폰트/회전·위젯 재배치·재부팅·Doze·Wear 성공은 미검증입니다. Windows 0.1.0 소스와 이전 배포 ZIP은 수정하지 않았습니다. 휴대폰/PC의 기록·티어는 아직 서로 동기화하지 않습니다.

**BUILD READY**. 기존 한국어판 서명 및 낮은 versionCode 조건의 **설치 전달 준비 완료**. **DEVICE VERIFIED 아님**. 릴리스 게시 후 실제 익명 다운로드/CI/업데이트 발견 결과는 delivery/2.8.12-live/remote-verify와 최종 대화에서 보고합니다.
