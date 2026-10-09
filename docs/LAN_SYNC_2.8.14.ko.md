# PC·휴대폰 같은 Wi-Fi 자동 공유 — 2.8.14

사용자 요청에 따라 다른 UI/분석 기능을 변경하지 않고 로컬 기록·티어 공유만 구현했습니다. PC 로그인을 기기별로 유지합니다.

## 동작

- PC 위젯 0.1.1 beta의 연결 코드를 Android 설정에 1회 등록합니다. PC 위젯 실행 상태에서 휴대폰 앱 실행·정상 API 조회·수동 지금 동기화 시 같은 LAN으로 전송합니다. 추가 OpenAI API 조회는 하지 않습니다.
- 최근 90일 실제 관측을 양방향 병합합니다. meter+관측 UTC 시각으로 중복을 제거하고 기존 관측을 덮어쓰지 않습니다. 원시 응답보다 정밀한 값을 생성하지 않습니다.
- 휴대폰의 일별 장기 집계와 이벤트는 PC 계정 폴더 shared-history.json에 별도로 보관합니다. 90일 이전 PC 원시 기록은 휴대폰의 이미 마감한 집계에 합산하지 않으며 원본 PC 파일을 보존합니다.
- 휴대폰의 기존 LiveUtilization 결과를 PC PhoneMirror로 전달합니다. 결제 기간/확인된 크레딧/기존 티어 규칙을 PC에서 다르게 재계산하지 않습니다. 현재 플랜·한도 길이·초기화 구간과 맞고 관측 후 15분 이내일 때만 공유 티어를 현재 값으로 표시합니다.
- TLS 1.2, PC 인증서 SHA-256 고정, 무작위 256비트 연결 비밀, 같은 계정 SHA-256 확인을 사용합니다. 공용 IP/DNS 주소를 받아들이지 않습니다. 연결 코드는 Windows DPAPI/Android Keystore 암호화 저장을 사용합니다. OAuth 토큰·대화·실제 결제액·APK 서명키는 전송하지 않습니다.
- 삭제 경계를 SQLite metadata에 저장하고 이전 PC 관측의 자동 재유입을 차단합니다. 로그아웃/계정 변경 중 결과는 기존 NETWORK_LOCK 및 계정·삭제 경계·연결 설정 검사를 거칩니다. PC 로그아웃은 공유 리스너를 중지합니다.
- 클라우드, 외부 서버, 새 SDK/의존성은 추가하지 않았습니다. LAN 통신용 PC 인증서는 APK 서명키와 별개이며 기존 APK 키는 그대로 사용합니다.

## 변경 파일

Android LanSync/LanSyncUi/LanSyncWire 추가. UsageLedgerDatabase에는 병합·삭제 경계만 추가. UsageApi 성공 이후와 MainActivity onResume에 동기화 시작 훅 추가. RecordSettings에는 연결 설정 버튼 하나 추가. 영어·한국어 문구 및 버전 표기 갱신.

Windows LanSync.cs 추가. Core.cs에는 관측의 정밀도/수동 필드를 지원하고 User-Agent 버전만 갱신. Widget.cs에는 공유 설정·연결 코드·모바일 티어 표시를 추가. 큰 기록 병합이 UI를 막지 않도록 티어 파일은 비동기로 읽습니다. 기존 인증·API 주소·한도 집계·위젯 디자인·크기·투명도·원본 데이터는 유지했습니다.

## 검증

- android/run-tests.sh 성공: 기존 4,009개 + 추가 26개 = 4,035개 수치화된 검증 및 기존 parser/source guards. 실제 SQLite JVM 플랫폼 fixture 병합 12개, LAN 프레임/주소 14개 포함.
- windows-widget/build.ps1 -Test: 95개 검증 및 18개 WinForms 오프스크린 렌더 성공.
- android/tests/verify-lan-sync.py: Android에서 사용하는 Java 코드와 실제 C# TLS 리스너 사이 24개 루프백 통합 검증 성공. 계정 격리·인증서/비밀 불일치·양방향 전송·정밀도·중복·과거 집계 전달 검증. 가짜 계정과 별도 임시 파일만 사용했습니다.
- android/tests/verify-localization.py: 앱 1,380개/Wear 116개 문자열 및 기존 629+44 formatter fixture 성공.
- app/wear assembleRelease 및 lintRelease 성공. 신규 lint 오류 없음. 기존 baseline(53 errors, 111 warnings)은 수정/재생성하지 않았습니다.
- verify-publication.py: 최종 APK 2개 포함 52개 archive의 공식 폰트/출처 검증 성공. 기존 회귀 fixture는 갱신하지 않았으며, 허용하는 추가 훅만 lan-reviewed-patches.json에서 정확히 역변환하여 원래 소스를 비교합니다.

초기 테스트에서 Windows TLS 임시 개인키 호환 오류를 찾아 UserKeySet으로 수정했습니다. 새 Keystore 상수 lint 오류도 공식 상수 사용으로 수정하고 다시 빌드했습니다. 테스트 임시 경로/샌드박스 네트워크 제한은 분리해 실제 루프백 테스트를 수행했습니다.

## 파일과 서명

Android versionName 2.8.14 / versionCode 44 / dev.bennett.codexmeter / targetSdk 36. APK 서명 인증서 SHA-256: 6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3. 기존 한국어판과 동일합니다. Windows 0.1.1 beta 실행 파일은 코드 서명되지 않았습니다.

최종 앱 lint: 0개 활성 오류, 166개 Warning.

- CodexMeter-2.8.14-ko.apk: 25971123 bytes / SHA-256 168961048ddcad52b52035be8c4ce240bd6a93f10b933389a335933653c79aa5
- CodexMeter-Wear-2.8.14-ko.apk: 15871345 bytes / SHA-256 8e69f241ba1d40a926cd770984c410f8677274ea85952236b804dbf3be0c1a54
- CodexMeter-WindowsWidget-0.1.1-beta.zip: 3640122 bytes / SHA-256 c83e9af41be6846f49721ea1f02aac7a31f9abd19f29a1b7f5e3f0eab2118329

## 미검증/한계

실제 갤럭시 또는 Android 에뮬레이터에서 LAN 설정 화면·Android Keystore·실계정 로그인·실제 사용량 조회·방화벽 동작·휴대폰 업데이트는 이번에 검증하지 않았습니다. 루프백 Java/C# 전송과 SQLite fixture 성공을 실기기 성공으로 취급하지 않습니다. PC 위젯은 실행 중이어야 하며 휴대폰 강제 종료/절전/Doze에서 고정 주기 실행은 보장하지 않습니다. 게스트 Wi-Fi/기기 격리/PC IP 변경은 재설정이 필요할 수 있습니다.

현재 targetSdk 36을 유지합니다. 향후 targetSdk 37로 올리면 ACCESS_LOCAL_NETWORK 선언 및 런타임 권한 흐름이 필요합니다. 공식 근거: https://developer.android.com/privacy-and-security/local-network-permission

판정: BUILD READY. 같은 서명/하위 버전 설치본 업데이트 파일 및 안내 준비 완료. DEVICE VERIFIED 아님.
