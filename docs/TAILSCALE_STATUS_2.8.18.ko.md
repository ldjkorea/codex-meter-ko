# GPT HUD 2.8.18 Tailscale 연결 검증

구현 및 로컬 빌드 완료. 사용자 휴대폰의 LTE·5G/Tailscale 연결은 아직 미검증입니다. 설치 안내에 외부망 확인 절차를 넣었습니다.

- 앱 2.8.18 / versionCode 48, dev.bennett.codexmeter. Windows 0.1.5 beta.
- 기존 2.8.17 한국어판 인증서 SHA-256과 동일: 6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3.
- bash android/run-tests.sh: 전체 통과. LAN 주소/전송/네트워크 조건 45개, 원장·위젯·Wear·기록·기존 기능 회귀 포함.
- python android/tests/verify-localization.py: 통과. 기존 보호 코드와 한국어/영어 리소스 확인.
- windows-widget/build.ps1 -Test: 269개 통과, 112개 기존 화면 렌더. 실제 서비스 로그인·레지스트리 변경 없음.
- Java LanSyncSelfTest + C# WidgetTests --lan-probe: 실제 TLS/gzip 통신 56개 통과. 합성 계정·루프백이며 실제 Tailscale 접속 증거는 아님.
- bash android/build.sh: 휴대폰·Wear Release 빌드 및 서명 검증 통과.
- :app:lintRelease :wear:lintRelease: 통과. 기존 경고 앱 180개 / Wear 11개. 수정한 연결 파일에 보고된 lint 문제 없음.
- verify-publication.py (최종 APK 2개): 기존 라이선스·폰트·의존성 검증 통과.
- 전용 에뮬레이터 benefit-perf-api35: 소유 확인된 합성 2.8.17 테스트 앱 위에 2.8.18 비파괴 업데이트 및 네이티브 회귀 36개 통과. 실제 사용자 앱·삼성 폰 아님.
- 설정 도우미 PowerShell 문법 및 플랫폼 명령 인자 확인 통과. 실제 관리자 권한 실행/방화벽 변경은 하지 않음.

초기 검사에서 이전 버전/릴리스 개수 고정값(2.8.17/47, 46개)을 발견해 새 버전/47개 기록으로 일관되게 수정했습니다. 과거 소스 baseline은 변경하지 않았습니다. TLS 테스트의 첫 긴 작업 경로에서 파일 생성이 실패했고 짧은 전용 임시 경로로 재실행하여 통과했습니다. 일반 사용자 데이터 폴더는 변경하지 않았습니다.

핵심 변경: LanSyncWire.java, LanSync.java, LanSyncUi.java, TaskStatusActivity.java, Windows LanSync.cs/Widget.cs. 그 외는 버전·한국어/영어 안내·릴리스 기록·테스트·설정 도우미입니다. OAuth/API/SQLite/계산/디자인/휘장/기존 저장 설정 및 키는 보존합니다. 같은 Wi-Fi 공유는 기존 방식이고 외부망에서 새로 허용한 것은 작업 상태 조회입니다. 연결 코드는 기존 계정별 보관 슬롯에 저장하므로 외부 사용 시 기존 LAN 코드 대신 Tailscale 코드를 등록합니다.

연결 조건: 기본 100.64.0.0/10 IPv4 주소, 활성 폰 VPN, 실행 중인 PC 위젯, 기존 인증서 고정/연결 비밀/동일 계정. 이 주소 범위는 다른 네트워크에서도 쓰일 수 있어 주소만으로 신원을 신뢰하지 않으며 기존 TLS·비밀·계정 검증을 모두 유지합니다. IPv6 및 사용자 지정 주소는 이번 범위에서 지원하지 않습니다.

15초는 작업 상태 상세 화면이 열린 동안의 확인 시도 간격입니다. 네트워크 지연과 동시 요청 억제로 정확한 표시 시각은 달라질 수 있습니다. 홈 위젯만 표시·Doze·화면 꺼짐에 15초를 보장하지 않습니다. 실제 Tailscale 설치·사용자 로그인·Windows 방화벽 도우미·휴대폰 LTE/5G·절전 및 장시간 동작은 설치 후 확인해야 합니다.

최종 파일:
- CodexMeter-2.8.18-ko.apk: 26,714,568 bytes; SHA-256 `a9396835ea79825c7fca84a5ea9e32db7fdb8c325d09655156aae5b4710ce1f0`
- CodexMeter-Wear-2.8.18-ko.apk: 16,035,737 bytes; SHA-256 `d78f7abe5a18e557c893c67e3d60dbe5d1b37fd0dd425745cb6574181b8c6fe0`
- CodexMeter-WindowsWidget-0.1.5-beta.zip: 4,854,398 bytes; SHA-256 `d0ea79d25307133445c2c576a5a4d68726d5a2e1f93bfb114ee0b99cb4a538d4`
- INSTALL.ko.md: 3,652 bytes; SHA-256 `007caf5507ea8da9cdece23c7ae86eae65227a5b49e02e316c724a2f2412d463`
- RELEASE_NOTES.ko.md: 1,282 bytes; SHA-256 `4c9fe34d3d00fbbd7a5989a592baa326a792fa8a298a61da726a85ddc2426112`
- LICENSE_NOTICES.zip: 613,872 bytes; SHA-256 `719dd579081eaae6db71312b9246096960e4934851a409f9230d286a279f075c`
