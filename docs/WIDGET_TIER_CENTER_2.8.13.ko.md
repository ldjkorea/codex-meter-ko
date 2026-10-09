# Codex Meter 2.8.13 — 중앙 휘장·티어 배경 검증 보고서

2026-10-10 · 한국어 베타 · Android 2.8.13 / versionCode 43 · `dev.bennett.codexmeter`.

## 실제 변경

- 모서리의 20/28dp 휘장을 7종 게이지 레이아웃의 첫째·둘째 항목 사이로 이동했습니다. 가운데는 가변 폭 1.2배 열을 사용하고, 휘장이 꺼지면 열 자체가 없어집니다.
- 휘장 이미지 캐시를 96×96에서 256×256으로 늘렸습니다. 기존 원본 투명 512px WebP 10개와 그 해시는 동일합니다. 캐시 하나는 256KiB이며 이전 RemoteViews가 참조하는 bitmap을 임의로 재활용하지 않습니다.
- 아이언·브론즈·실버·골드·플래티넘·에메랄드·다이아몬드·마스터·그랜드마스터·챌린저 각각의 어두운 색상, 은은한 명암, 얇은 테두리, 28dp 모서리를 적용했습니다. 기존 불투명도 56/88/100의 30개 둥근 배경입니다.
- 휘장이 켜진 반원 게이지는 기존 `compactDial` 렌더러를 사용해 숫자·긴 초기화 문구까지 함께 축소합니다. 원래 링의 항목 수를 유지하므로 4열용 리소스를 사용해도 새 항목이 추가되지 않습니다. 휘장 OFF에는 원래 반원 렌더러·테마·강조색이 적용됩니다.
- 배경 OFF는 투명이며 저장된 테마·강조색·순서·5시간 숨김·위젯 디자인을 수정하지 않습니다. 계정 없음·오래된 관측·평가 불가에는 확정 티어 배경을 만들지 않습니다.
- `WidgetConfigActivity`의 아직 저장하지 않은 표시 스위치도 같은 팔레트/배경을 사용합니다. AppCompat의 기존 호환 drawable 로더를 사용합니다.

## 수정 파일

`WidgetRenderer.java`: 미리보기 draft 설정을 첫 렌더부터 전달, 티어 배경·밝은 전경, 선택 항목을 유지한 가변 게이지, TalkBack 설명 연결.

`WidgetCrest.java`: 기존 설정·삭제/복원·계정 캐시 규칙을 보존하고 bitmap 해상도만 증가.

`WidgetConfigActivity.java`: 실제 위젯과 동일한 30개 배경을 미리보기 surface에도 적용.

새 `WidgetTierPalette.java`, `WidgetTierSurface.java`, `drawable/widget_tier_*.xml`: 표시용 색상과 리소스 선택. 인증·네트워크·DB·설정 저장 호출 없음.

`widget_rings`, `widget_rings_four`, `widget_dials`, `widget_dials_large`, `widget_dials_max`, `widget_rings_large`, `widget_rings_max`: 가운데 ImageView만 추가 배치. 11종 전체 레이아웃에서 이 추가 ImageView를 제거하면 독립된 원본 `d10688f`와 바이트가 일치합니다.

`WidgetTierPaletteSelfTest`, `verify-widget-tier-center`, `WidgetTierNativeTest`, `verify-widget-tier-native`: 순수 정책·리소스·실제 Android 렌더링 검증. 기존 source guard는 건너뛰거나 fixture를 재생성하지 않고 정확한 before/after 패치를 추가했습니다. 새 표시용 shared 파일은 upstream 보존 목록에 포함되는 기존 파일이 아니므로 별도 새 기능 테스트 대상으로 등록했습니다.

app/Wear Gradle, AppConstants, build.sh, run-tests, 버전 검사, README/CHANGELOG/Release Notes: 2.8.13/43 동기화. 인증·API·SQLite·저장 설정·기존 위젯/Wear 핵심 파일의 보호 검증이 통과합니다. 원본 프로젝트·이전 APK·비공개 `docs/emblem-concepts`는 보존합니다.

## 검증 명령과 결과

- `bash android/run-tests.sh`: 기존 3,827 + 새 팔레트 182 = **4,009 assertions 통과**. 원장·마이그레이션·계정 격리·5시간 숨김·복원·사용량 계산·업데이트 신뢰 검증을 포함합니다.
- `python android/tests/verify-localization.py`: 한·영 리소스, 기본 영어, formatter fixtures, 기존 self-test, 보호 소스 검증 통과.
- `bash android/gradlew -p android :app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease --offline --console=plain`: 성공. phone lint 166 warnings/2 hints, Wear 9 warnings. 이번 작업의 compat drawable 경고 1개는 수정했습니다. 기존 phone baseline의 53 errors/111 warnings는 필터링되며 baseline 파일은 바꾸지 않았습니다. Gradle 10 이전 제거 예정 API 안내도 기존 상태입니다.
- `verify-publication.py`에 최종 phone/Wear 전달 파일명으로 APK 2개 전달: 라이선스·공식 Pretendard·금지 폰트 부재, 총 52 archive 검사 통과. 원래 Gradle의 소문자 wear 출력 파일명은 이 검사 도구의 전화 APK 분류에 들어가므로, 실제 배포 이름의 Wear APK로 검증했습니다.
- `verify-precision-crests.py phone.apk`, `verify-live-utilization.py phone.apk`: 실제 APK 내 원본 휘장 10개·요금제 이미지 5개 payload 해시 및 18개 핵심 보호 계약 통과.
- `apksigner verify --print-certs` phone/Wear: 성공. 기존 인증서 SHA-256 `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`. 신규 서명키·alias 변경 없음.
- `aapt2 dump badging`: application ID 그대로, versionName 2.8.13, versionCode 43. phone minSdk26 / Wear minSdk30.

## Android 네이티브 렌더링

다른 작업의 AVD와 분리해 workspace의 빈 API35 AVD를 만들었습니다. emulator-5568에 테스트 APK를 설치했고, 실제 Android RemoteViews 렌더러에서 **102 checks 통과**했습니다. 7개 레이아웃의 가운데 위치/겹침 없음/OFF, 10티어×2크기, 30개 배경의 실제 compat 로딩, 로그아웃 상태의 production buildPreview의 좁은/넓은 draft ON/OFF를 검증했습니다. 계정·사용량 API·원장 데이터를 생성하지 않았습니다. 출력 수치는 합성 값입니다.

21 PNG 캡처: `android/build/widget-tier-native/captures/widget-tier-fixtures`. 주요 4장은 `docs/widget-tier-2.8.13`. 이는 native view를 에뮬레이터에서 렌더링한 결과이며 삼성 런처 배치나 실제 휴대폰 사용 성공의 증거가 아닙니다.

첫 테스트 실행의 ADB 재연결/다른 AVD 점유/빈 패키지 조회 종료 코드/서명 도구의 같은 password 파일 중복 읽기/raw 결과 출력 문제는 격리 환경과 실행 도구에서 수정했습니다. 기존 AVD를 종료하거나 초기화하지 않았습니다. 최종 `INSTRUMENTATION_CODE: -1`과 102 checks, PNG 21개를 다시 확인했습니다. 테스트용 javac의 -source 17 system modules 안내는 앱 컴파일 오류가 아닙니다.

## 최종 APK

- Phone `CodexMeter-2.8.13-ko.apk`: 25,962,191 bytes · SHA-256 `4c8c83cc191b4cbb250f6c2b6bd5011ec08cf4b1f0fb3f757fd373b8e55a533d`.
- Wear `CodexMeter-Wear-2.8.13-ko.apk`: 15,867,005 bytes · SHA-256 `7fdb49e18147c192a0073e2d2e34c39b36e43f393e2aa47556927daa84f1dc57`.
- 로컬 전달 폴더: `delivery/2.8.13-tier-widget/`. 이번 작업의 중간 검증 APK도 하위 pre-final-check/pre-compat-check에 보존합니다. 이전 공개 2.8.12 이하 APK는 수정하지 않습니다.

## 확인하지 않은 범위

실제 삼성 갤럭시의 런처·위젯 재부팅·갤럭시 위젯 설정 화면·AOD/잠금화면의 시각 결과, 실제 설치본 서명 비교/로그인 유지/OAuth/API 조회, 실제 사용자 원장과 결제액 검증은 수행하지 않았습니다. 기존 잠금화면 전용 provider와 별도 티어 진화형 위젯은 기존 표시 로직을 유지하며 중앙 재배치 대상은 7종 기존 홈 게이지입니다. 높은 투명도와 밝은 벽지에서의 가독성은 실기기 확인이 필요합니다. 선명한 표시에는 불투명도 88~100%를 권장합니다.

**BUILD READY**, 기존 한국어판 서명/낮은 versionCode 조건의 설치 전달 준비. **DEVICE VERIFIED 아님**. GitHub 사용자 소유 베타 공개/익명 다운로드/CI 결과는 전달 폴더의 PUBLICATION 보고서와 최종 대화에서 별도 확인합니다.
