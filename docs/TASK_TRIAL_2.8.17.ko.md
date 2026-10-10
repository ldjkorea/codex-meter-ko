# 2.8.17 검증 결과

## 버전·서명

Android/Wear 2.8.17, versionCode 47. 패키지 dev.bennett.codexmeter 유지. 기존 2.8.16과 서명 인증서 SHA-256 일치: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`. 휴대폰·Wear 서명 검증 및 최종 APK Pretendard/라이선스/금지 폰트 검사 통과.

## 문구 및 성능

실제 활성 한국어 600개(15개 묶음), 정규화한 완전 중복 0개. 문장 의미의 중복을 기계 검사만으로 0개라고 주장하지 않습니다. 기본 5% 후보 80개, 240회 선택에서 최근 20개 반복 없음. Android 실제 선택도 21회 연속 서로 다른 문구 확인.

JVM 신규 검사 1772개 통과. 후보 파일 준비 22.79ms, 준비 후 추첨 p50 0.0009ms / p95 0.0045ms. Android API35 x86_64 에뮬레이터: 첫 파일 준비+설정 읽기 36.92ms, 준비 후 선택 p50 0.0275ms / p95 0.1872ms(1000회). 최종 재설치 반복 실행에서는 첫 준비+설정 읽기 13.30ms, p50 0.0178ms / p95 0.0389ms로 측정되어 캐시·실행 환경에 따른 변동이 있었습니다. UI 스레드에서 파일을 읽지 않으며 기존 FunHome 작업자를 재사용합니다. 이 값은 실기기 프레임 지연이나 인증된 홈의 콜드 실행/스크롤 성능을 증명하지 않습니다. 전체 앱 콜드 실행·홈 복귀·스크롤의 전후 비교는 미측정입니다.

문구 원본 35673바이트, APK 안 압축 12769바이트. 같은 로컬 Release 설정으로 2.8.16 26669054바이트 → 문구만 반영한 중간 APK 26681935바이트(+12881) → 최종 26711452바이트(+29517 추가, 전체 +42398). 중간 APK에는 TaskStatusData/상태 UI가 없습니다. 최종 추가분은 시험 기능 및 릴리스/버전 메타데이터를 포함한 묶음 차이이며 순수 상태 코드만의 용량으로 단정하지 않습니다.

## 시험 기능 증거 범위

- 실제 설치된 Windows Codex: OpenAI.Codex 26.1007.2314.0. 현재 로컬 기록의 task_started/task_complete 메타데이터 형식을 확인했습니다. 별도 App Server를 실행하지 않았고 Hooks·Codex 설정은 변경하지 않았습니다.
- 모의 이벤트·로그 파서: 새 턴, 중복, 오래된 턴 완료, 동시 스레드, 중단, 도구 실패의 턴 실패 오인 방지, 재시작 시 과거 기록 건너뛰기, OFF, 원문 제거 검사를 통과했습니다.
- Windows 테스트 259개 및 기존 112개 렌더 통과. 기존 인증 뒤 task-status 경로를 연결했습니다. 기존 핀 TLS/연결 비밀값/계정 검증/시간·프레임 제한을 유지합니다.
- PC 직접 시작한 실제 데스크톱 작업의 수집기 종단 검증: **미검증**. 실제 Remote 시작 작업: **미검증**. 실제 PC→휴대폰 전송: **미검증**. 합성 인증 데이터로 기존 전송 규칙을 검사한 것은 실계정 기기 간 증거가 아닙니다.
- Android 네이티브 36개 검사 통과: 문구 선택, 영어 fallback, OFF 상태, 상세 화면 실행, 좁은 화면/큰 글꼴의 한국어 카드, 1개/3개 작업 RemoteViews 합성 렌더. 삼성 홈 런처에 배치한 결과는 미검증입니다.
- 승인 대기·입력 대기·턴 실패·대기 중을 실제로 구분하는 수집원은 이번 버전에서 확보하지 못했습니다. 관찰 전 턴은 알 수 없고 로그가 조용하다고 종료를 추정하지 않습니다. 따라서 지원을 홍보하지 않으며 ‘이 환경은 아직 감지 미확인’으로 시작합니다.
- 수집기는 해당 Windows 사용자 `.codex/sessions`의 새 메타데이터를 읽습니다. Codex 자체 로그인 계정 일치를 검증하지는 않습니다. 휴대폰 전송은 기존 페어링 계정 규칙으로 제한합니다. 작업 이름은 프롬프트 대신 짧은 스레드 ID입니다.
- 로그 읽기 실패/미지원 형식/큰 항목을 건너뛸 때 상태는 불명 또는 마지막 확인값입니다. 현재 관찰기 접촉과 작업 상태는 분리합니다. 위젯은 항상 마지막 상태로 표시합니다.

## 실행한 검사

`bash android/run-tests.sh` 전체 회귀 성공(기존 SQLite·초기화·마이그레이션·가성비·위젯/Wear 포함).
`python android/tests/verify-localization.py` 리소스 parity 및 보호 파일 37개 검사 통과.
`windows-widget/build.ps1 -Test` 성공.
`gradlew :app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease --offline --console=plain` 성공. 기존 lint baseline 유지; app 180 경고, 기존 baseline의 53 오류·111 경고는 필터됨. 신규 문구/시험 파일에 연결된 lint 경고 없음. 이를 lint 경고 0이라고 표현하지 않습니다.
`python android/tests/verify-spicy-trial-native.py emulator-5554` 격리 QA AVD 검사 성공.
`python android/tests/verify-publication.py <최종 휴대폰 APK> <최종 Wear APK>` 2개 성공.
`apksigner verify --print-certs`, `aapt dump badging` 최종 APK 확인.

첫 상태 모듈 컴파일 중 괄호 오류는 수정했고 최종 빌드에 남지 않습니다. 최초 publication 검사는 Wear 파일명이 판별 규칙에 맞지 않아 실패했으며 최종 전달 파일명으로 재검증했습니다. 검사 baseline/fixture를 재생성해서 실패를 숨기지 않았습니다.

## 기존 작업 보존

기존 인증/API/SQLite/티어/가성비/WidgetOptions/WidgetRenderer/5시간 정책/런처 파일은 유지합니다. MainActivity는 문구의 foreground/background/manual 선택 시점만 추가했습니다. 개인 미추적 docs/emblem-concepts는 건드리거나 배포하지 않았습니다. 원본 BenItBuhner 저장소로 push하지 않습니다. 갤럭시 실기기 설치·로그인·API·장기 절전은 미검증입니다.
