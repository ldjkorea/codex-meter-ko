# 2.8.11 기존 홈 위젯 휘장 · Windows Widget 0.1.0 beta 검증

## 실제 변경

휴대폰 티어 휘장은 이전 버전에서 EvolutionWidget 전용이었다. 기존 CodexUsageWidget의 게이지·막대·목록을 사용하면 볼 수 없었다. 기존 11개 홈 레이아웃에 작은 20/28dp 휘장을 추가하고 WidgetRenderer에 실티어 이미지 연결을 넣었다. 기존 게이지·퍼센트·새로고침·5시간 숨김·선택/순서/색/투명도는 그대로다. 큰 휘장 중심 구성은 기존 티어 진화형 위젯으로 가능하다.

WidgetCrest는 별도 codex_widget_crests Preference의 위젯별 enabled 기본 true만 사용한다. 예전 위젯에 새 휘장이 기본 표시되고 OFF 저장/복원/취소 미리보기/회전 초안/복원 ID/삭제가 독립적이다. 계정 변경/로그아웃의 기존 갱신 경로와 비동기 티어 평가 완료에도 기존 위젯을 다시 그린다. 휘장만 추가하며 사용량 요청을 추가하지 않는다. Bitmap은 96×96 ARGB, 배치 중에는 흐린 Iron/배치 중 접근성 표시이며 새 등급을 생성하지 않는다. 인증·DB·원장·티어 평가 18개 보호 계약은 바이트 동일하다. Samsung 잠금화면/AOD/Now Bar와 Wear에 새 휘장은 추가하지 않았다.

Windows는 WinForms/.NET Framework 4.8의 독립적인 작은 창이다. 상단 이동·테두리 크기 변경·항상 위·투명도·글자 크기·트레이 숨김/종료·F5/Shift+F10·선택적 HKCU 시작 시 실행을 구현했다. 새 외부 SDK나 광고/분석 서버 없이 OAuth PKCE loopback 1455/1457, 기존 사용량 endpoint와 계정 헤더를 사용한다. 토큰은 DPAPI CurrentUser로 별도 로컬 저장하며 PC Codex 인증/대화 파일을 읽지 않는다. 수동/5분 조회·30초 요청 제한·전체 30초 타임아웃·2MiB 응답 상한·실패 시 마지막 값·15분 최신성 구분을 적용했다.

PC 계정별 파일 저장은 SHA-256 폴더 이름·동일 관측 중복 제거·쓰기 잠금·세대 취소·완료 파일 교체/이전 사본·관측 역전 방지를 사용한다. 일별 기록은 삭제하지 않고 분석에 최근 90일을 읽는다. 서울 날짜의 같은 정책/초기화 구간 안 증가분만 일별 %p로 합산한다. 자정/6시간 공백/사용률 감소는 추정하지 않는다. 장기 완료 주간 티어 규칙을 재사용하고 별도 상태 파일에 유지한다. PC/휴대폰 기록/티어 자동 동기화는 구현하지 않았으며 기록 부족 시 배치 중이다. Windows 추가 한도/결제/캘린더 확장은 범위 밖이다.

## 검증

- `bash android/run-tests.sh`: 기존 3,503개와 새 홈 휘장 39개, 합계 3,542개 명시적 assertion 통과. 기존 별도 파서/위젯/OAuth 검사도 성공.
- `python android/tests/verify-localization.py`: 영어/한국어 앱 1,330 문자열, Wear 116. 기존 포맷터 629개와 5시간 표현 44개 assertion 통과.
- `python android/tests/verify-home-crests.py`: production helper의 기본 ON·OFF·독립 설정·복원·다시 읽기·미리보기 비저장·렌더·배치 중·10개 티어·변경 감지·저장 실패 39개. JVM fixture이며 삼성 런처 검증은 아니다. 11개 XML에서 추가 휘장을 제거하면 이전 실제 커밋 d10688f와 정확히 동일하다.
- `./windows-widget/build.ps1 -Test`: 78개 통과. DPAPI 재읽기·계정 분리·중복·12개 동시 기록/최신 값 유지·로그아웃 세대·자정/공백/초기화/정책·PKCE RFC 벡터/상태·가상 API/오류/취소/응답 상한·폼 설정을 검증했다. 340/400/640px와 글자 100/125/150%, 빈 상태/가상 관측의 18개 비트맵을 생성했다. 실제 바탕화면 조작/실서버 성공을 뜻하지 않는다.
- `bash android/gradlew -p android :app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease --offline --console=plain`: 성공.
- lint 신규 오류/경고 0. 기존 phone 경고 146개/Hint 2개, Wear 9개. 기존 baseline 오류 53/경고 111 필터 유지. 신규 drawable 로딩 권고는 호환 로더를 사용해 해결했으며 baseline은 갱신하지 않았다.
- apksigner: phone/Wear APK v2, signer 1개. aapt2: 2.8.11 / versionCode 41 / dev.bennett.codexmeter, phone minSdk26/Wear30.
- 최종 phone/Wear verify-publication: 52개 archive 폰트/라이선스 검사 통과. 최종 phone verify-precision-crests: 10개 실제 자산과 보호 계약 통과.
- Windows 빌드는 모든 휘장 원본/변환 해시를 출처 manifest와 검사한다. 자체 코드 서명은 없다.

초기 저장 테스트가 sandbox 임시 파일 교체 권한으로 실패해 별도의 가상 데이터로 일반 사용자 테스트 환경에서 재검증했다. 티어 테스트의 기대값은 기존 한 단계 승급 규칙에 맞춰 수정했다. 150% 글꼴 버튼 줄바꿈은 폭과 줄바꿈 정책을 조정해 재렌더링했다. 사용자 데이터를 삭제하거나 기존 보호 fixture를 재생성하지 않았다.

## 변경 파일

- WidgetCrest.java: 독립 휘장 Preference/실티어 이미지/변경 시 기존 위젯 갱신.
- WidgetRenderer.java, WidgetConfigActivity.java, CodexUsageWidget.java, EvolutionWidget.java: 렌더/설정 미리보기/저장/복원/비동기 갱신 연결.
- 11개 widget XML, en/ko strings: 작은 휘장·설정 문구. 기존 레이아웃은 추가 부분 외 동일.
- verify-home-crests.py, evolution-reviewed-patches.json, run-tests.sh: 새 동작 검증·정확히 한 번 일치하는 추가 패치만 기존 보호 검사에 허용.
- app/Wear Gradle, AppConstants, build.sh/버전 검사/README/CHANGELOG/Release Notes: 2.8.11/41 동기화.
- windows-widget/src 6개 C# 파일·manifest·빌드·테스트·승인 자산 변환·출처·사용 안내: 독립 Windows 위젯.
- .github/workflows/build-apk.yml: 읽기 권한만 유지한 Windows 컴파일/가상 검사 job. 실로그인·게시·서명키 없이 실행.

## 산출물

- CodexMeter-2.8.11-ko.apk: 25,801,298 bytes · SHA-256 `cc3f2e5df04018028656b60041db5947578f622202bf1ccbe8da9e520800b4e1`
- CodexMeter-Wear-2.8.11-ko.apk: 15,862,093 bytes · SHA-256 `f8182cdd370ddc67f4df149b0e2a3a155a0f3c0c07747f0448faefd11a0334f7`
- CodexMeter-WindowsWidget-0.1.0-beta.zip: 3,631,667 bytes · SHA-256 `582030adaae79229a53baabf6570d9be30ee67dfaddff0e3b2b61445526400ce`
- LICENSE_NOTICES.zip: 613,872 bytes · SHA-256 `719dd579081eaae6db71312b9246096960e4934851a409f9230d286a279f075c`
- Windows EXE SHA-256 `769ab7095a2061ff802ab019838c9259eeb3691ef5c845efb7d0faa0ec97c73f`

Android 서명 인증서 SHA-256: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`. 이전 2.8.10과 동일하며 키·alias·Application ID 변경 없음. 설치된 실제 기기의 인증서는 미확인이다. Windows는 코드 서명이 없어 SmartScreen 경고가 나타날 수 있다.

## 미검증 / 판정

ADB 연결 기기 없음, 에뮬레이터 없음. Android 업데이트 설치/기존 로그인/실 API/실기록/홈 위젯 크기/회전/삼성 런처/재부팅/Doze/Wear/잠금화면은 미검증. Windows 실제 계정 로그인·서버 조회·바탕화면 이동/리사이즈/트레이/고DPI/다중 모니터/시작 시 실행도 미검증. 자동 테스트는 전용 가상 데이터로 실행했으며 실제 계정·기기 데이터는 건드리지 않았다.

Android BUILD READY, 동일 한국어판 서명/낮은 versionCode 조건의 설치 준비 완료. DEVICE VERIFIED 아님. Windows 빌드/가상 검사 완료한 0.1.0 베타이며 실서비스 연동 완료로 판단하지 않는다. 이전 APK/공개 릴리스/원본 저장소/개인 concept 폴더는 보존한다. 공개 게시·CI·익명 다운로드/자동업데이트 parser의 최종 증거는 게시 후 별도 기록한다.
