# Codex Meter 2.8.15 검증 보고

## 결과 및 보존

Android **2.8.15 / versionCode 45 / dev.bennett.codexmeter**, Windows **0.1.2 beta**. 원본 BenItBuhner 저장소에는 push하지 않습니다. 기존 한국어판 키·alias·패키지·사용자 기록·미커밋 개인 시안을 보존했습니다. 정상 설정에서 불필요한 진입만 숨겼고 DB나 Preference 데이터를 삭제하지 않았습니다. 새 외부 서버·분석 SDK·토큰 수집 엔진은 추가하지 않았습니다.

## 주요 변경 파일

| 영역 | 변경 파일 및 목적 |
| --- | --- |
| 전체 릴리스 기록 | `release-history.json`, `ReleaseCatalog`, `ReleaseHistoryActivity`, `WhatsNewActivity`, `ReleaseNotesUi`, `UpdateActivity`: 44개 버전 오프라인 저장, 설치 버전의 새로운 기능, 펼침 카드, 글머리표와 들여쓰기 |
| 홈 | `LiveCards`, `LedgerDashboard`, `FunHome`: 작은 가로 휘장, 주간 사용량 제목, 반응형 버튼, 미제공 5시간 문구 제거 |
| 설정/요금제 | `RecordSettings`, `PlanArtwork`, subscription 5종: 일반 설정 정리, Pro Lite 별칭 인식, 요금제별 그림 |
| Wi-Fi | `LanWifiScheduler`, `LanWifiJobService`, `CodexMeterApplication`, `BootReceiver`, `LanSyncUi`, manifest: 재연결 감지, 중복 제한, Wi-Fi 백그라운드 작업 |
| 인용 | `TiboQuote`: 날짜가 표시된 짧은 인용, 원문 링크, 앱에 저장된 문장 |
| 휘장 | Android prestige 10종, Windows prestige 10종, 출처/해시 manifest: 삼족오 문양으로 교체. 기존 레이아웃·옵션·티어 계산 보존 |
| 버전/회귀 | App/Wear Gradle, AppConstants, build.sh, test guards, 영어/한국어 리소스, CHANGELOG, Windows UA·도움말 갱신 |

`UsageApi`, `UsageParser`, SQLite 원장·계정·토큰·결제·티어 계산·위젯 슬롯 정책 등 주요 13개 파일은 2.8.14 실제 커밋과 비교하여 보존했음을 확인했습니다. 기존 공개 회귀 fixture와 lint baseline은 재생성하지 않았습니다.

## 실행한 검증

| 명령/환경 | 결과와 범위 |
| --- | --- |
| `bash android/run-tests.sh` | 전체 성공. 기존 4,035개 + 이번 86개 = 수치화된 4,121개 JVM/계산/SQLite/정책 검증, 기존 parser/source guards 포함 |
| `python android/tests/verify-ui-polish.py` | 운영 코드의 Wi-Fi 스케줄 결정·요금제 별칭·44개 릴리스 카탈로그 86개 성공 |
| `python android/tests/verify-localization.py` | 앱 1,391개/Wear 116개 문자열, 기존 formatter 629개 + 5시간 표시 44개, 37개 보호 파일 검사 성공 |
| `python android/tests/verify-ui-polish-native.py` | Android API 35 별도 에뮬레이터: UI 컴포넌트 28개 + 실제 RemoteViews 102개 성공. 27개 화면 캡처. 일반 411dp/좁은 320dp/큰 글꼴 1.4/라이트·다크, 휘장 중앙·투명도·표시 OFF, 실제 JobScheduler Wi-Fi 조건 등록 검증 |
| `windows-widget/build.ps1 -Test` | 95개 성공, 18개 WinForms 오프스크린 렌더. 실제 계정·API·레지스트리 쓰기 없음 |
| `python android/tests/verify-lan-sync.py` | 실제 Java/C# TLS 루프백 24개 성공. 합성 계정·분리된 임시 파일 사용. 물리 휴대폰 통신 검증 아님 |
| `assembleRelease`, `lintRelease` (app/wear) | 성공. 앱 활성 오류 0, 경고 178. Wear 오류 0, 경고 9. 기존 baseline 53 errors/111 warnings 유지. 이전 앱 166개 대비 경고 12개 증가 포함, 경고가 없는 빌드로 표현하지 않음 |
| `verify-publication.py` (최종 APK 2개) | 52개 archive의 공식 폰트 출처·금지 폰트 해시 부재 성공 |
| `verify-precision-crests.py`, `verify-live-utilization.py` (최종 휴대폰 APK) | 휘장 10개 및 보호 계약·기존 5개 그림 회귀 성공. 신규 요금제 5개도 aapt2 리소스 경로와 manifest SHA-256로 최종 APK 바이트 일치 확인 |
| `apksigner verify --verbose --print-certs`, `aapt2 dump badging` | 휴대폰/Wear 서명 검증, 휴대폰 버전 45/2.8.15 및 패키지 확인. 2.8.14 인증서와 동일 |

테스트 중 찾은 신규 lint 상수 오류와 주간 전용 계정의 불필요한 5시간 문구를 수정하고 재검증했습니다. 네이티브 fixture의 “5시간” 검사에서 “95시간”을 오인한 테스트 오류도 고쳤습니다. 새로고침은 좁은 화면에서도 읽히는 문자 버튼으로 정리했습니다.

## 인용 및 기록

[Tibo의 2026.04.18 X 원문](https://x.com/thsottiaux/status/2045299702590259631)을 공식 X oEmbed 응답으로 확인했습니다. 사용한 8단어 발췌는 “We are barely getting started with Codex.”이며 한국어로 번역했습니다. 원문의 긴 본문은 저장하지 않았습니다. 최신 글을 자동 수집하는 기능은 구현하지 않았습니다.

릴리스 카탈로그는 초기 원본 릴리스와 한국어판의 기록을 구분하고, 기존 CHANGELOG의 원문도 함께 보존합니다. 오래된 upstream APK를 업데이트 대상으로 제시하지 않습니다. 삼족오 휘장과 요금제 그림은 image_gen으로 개별 생성했고 알파를 보존한 파일로 패키징했습니다. 과거 precision-crests의 갤러리/프롬프트 문서는 당시 디자인 기록이며 활성 휘장은 최신 ASSETS.json을 기준으로 합니다.

## 서명 및 파일

기존 APK 인증서 SHA-256: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`.

- `CodexMeter-2.8.15-ko.apk`: 26,507,369 bytes / SHA-256 `8dc116e958576dde1691e9afa8c2591878ef2df3db27c716f3cec5c5f452cec4`
- `CodexMeter-Wear-2.8.15-ko.apk`: 15,871,345 bytes / SHA-256 `97bef9a597c0f96ed4e2ceb54a120bfc7d87209a6a2e23f2d17ebd50678114ba`
- `CodexMeter-WindowsWidget-0.1.2-beta.zip`: 4,767,335 bytes / SHA-256 `084c4cd4c73262f1f316670c075427bb4288e79f74c8c44c3c23d2f29efeca63`

## 미검증 및 판정

실제 삼성 휴대폰에 설치하지 않았습니다. 사용자 설치본 인증서·로그인 유지·실제 OpenAI 조회·실제 홈 런처/AOD·PC와 휴대폰 사이 Wi-Fi 공유·Doze/장시간 실행은 미검증입니다. 새 스케줄러는 기존 15분 백그라운드 최선 노력 방식이며 정확한 즉시 실행을 보장하지 않습니다. 기존 공유 연결 설정과 PC 위젯 실행이 필요합니다. Windows 실행 파일은 코드 서명되지 않았습니다.

화면 캡처와 130개 네이티브 검사는 합성 데이터의 컴포넌트/RemoteViews 검증이며 실제 사용자 계정의 전체 앱 동작을 증명하지 않습니다. 강제 종료된 앱은 사용자가 다시 실행할 때까지 재연결 콜백이 작동하지 않을 수 있습니다.

**BUILD READY**, 이전 한국어판 APK와 서명 일치 및 업데이트 전달 준비 완료. 실제 사용자 설치본과 비교하는 조건은 남아 있으며 **DEVICE VERIFIED는 아님**. 기존 데이터를 삭제하는 설치 우회는 하지 않습니다.
