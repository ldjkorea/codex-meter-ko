# Codex Meter 2.8.2 — 화면 개선판 검증 보고

2026-10-08. 사용자의 “화면 변화가 작고 직관적이지 않다”는 피드백에 따라 실제 Android 화면 구성을 변경했다. 저장·인증·사용량 계산 기능을 추가하는 작업은 진행하지 않았다.

## 사용자가 확인할 변화

| 위치 | 이전 | 화면 개선판 |
| --- | --- | --- |
| 메인 | 기존 한도 카드 아래의 사용 기록 카드에 분석·수동 저장 진입 배치 | 상단의 색상 요약 카드에 남은 한도, 초기화까지 시간, 오늘 확인한 증가량, 분석 보기·지금 조회·저장 버튼 배치 |
| 분석 | 통계·달력·이벤트·내보내기를 한 화면에 나열 | 요약 / 추세 / 기록으로 이동 목적 분리 |
| 요약 | 숫자·설명 중심의 긴 목록 | 남은 한도를 크게 표시하고 오늘·어제 비교, 예측 상태, 사용 속도·하루 예산을 묶어서 표시 |
| 추세 | 단순 막대 | 오늘·7일·30일 선택, %p 눈금, 기간 증가량과 기록한 날짜 수, 막대 터치로 날짜 상세 |
| 기록 | 여러 종류의 상세 항목을 계속 나열 | 달력과 최근 날짜 목록, 펼쳐 보는 한도 변화·날짜 미배분 구간, 내보내기 버튼 |

기존 사용 기록 표시 설정은 유지한다. 해당 표시를 끈 경우 메인 상단 요약도 숨기며, 우측 상단 메뉴의 사용량 분석으로 진입할 수 있다. 메인 요약은 표시 설정이 켜진 주간 → 월간 → 5시간 한도 순으로 선택한다. 서로 다른 한도 사용량을 합산하지 않는다.

## 표시 정확성과 보존 범위

- 기록이 없거나 비교 가능한 관측 구간이 없으면 사용량을 `—`로 표시한다. 관측값 한 개만 있는 날, 자정을 넘어 날짜에 배분하지 못한 구간을 실제 0으로 표시하지 않는다.
- 같은 날짜의 유효한 관측 구간에서 실제 변화가 없으면 0 %p를 표시한다. 증가량은 기존 집계 결과를 읽는다.
- 부분 관측·측정 공백·기존 이관 기록 표시를 유지한다. 예측은 별도 영역에 ‘예측’으로 표시하며 기존 부족·오래됨·초기화 조건을 유지한다.
- 영어·한국어 새 문자열 37개를 추가했다. 큰 글꼴 또는 좁은 화면에서 요약 버튼·탭·비교 카드가 세로로 배치되도록 했다. 실제 화면에서의 잘림 여부는 미검증이다.
- 초기 화면에서 비동기 조회가 화면 부착보다 빨리 끝날 때 상단 카드가 로딩으로 남을 수 있는 조건을 제거했다.
- 로그인/OAuth/보안 토큰/UsageApi/SQLite/AccountSession/RefreshScheduler/기록 집계·예측/위젯·Wear 코드는 기존 안정화 커밋 `06a3230`과 동일하다. 분석 화면의 수동 조회·SAF 파일 생성·내보내기 메서드도 동일하며 자동 테스트로 확인했다.
- Application ID, 버전 2.8.2/32, 서명키·alias는 변경하지 않았다. 기존 APK와 사용자 데이터는 보존했다.

## 변경 파일

- `android/app/src/main/java/dev/bennett/codexmeter/MainActivity.java`: 상단 요약 및 분석 메뉴 연결, 기존 하단의 중복 진입 버튼 정리.
- `LedgerDashboard.java`: 메인 요약 카드와 읽기 전용 오늘 기록 조회.
- `LedgerAnalyticsActivity.java`: 세 화면 구성, 기간·달력·날짜 상세·내보내기 진입 정리.
- `LedgerTrendView.java`: 차트 눈금, 날짜 선택, 기록 부족 표시와 접근성 설명.
- `LedgerUi.java`: 새 화면에 한정한 카드·숫자·버튼·탭 스타일, 글꼴 크기 대응.
- `android/shared/src/main/java/dev/bennett/codexmeter/LedgerPresentation.java`: 관측 구간 유무와 선택 한도의 기간 표시 판단.
- `android/app/src/main/res/values{,-ko}/ledger_ui_strings.xml`: 영어·한국어 새 화면 문구.
- `android/tests/LedgerPresentationSelfTest.java`, `verify-ledger-ui.py`, `ui_fixes.py`: 표시 정확성 테스트 및 인증·저장·내보내기·버전 보존 검사.
- `android/run-tests.sh`, `verify-release-stability.py`, `verify-localization.py`: 새 표시 테스트 연결, 기존 소스 보존 검사에 UI 변경 범위 반영.
- `CHANGELOG.md`, `RELEASE_NOTES.ko.md`, 이 보고서와 설치 안내: 변경점 및 검증 한계 기록.

## 자동 검증 결과

JDK 21.0.12.1, Android SDK, 기존 Gradle 9.6.1 및 vendored 의존성으로 수행했다. 실행 시 JAVA_HOME과 ANDROID_SDK_ROOT는 기존 도구 폴더로 지정했다.

```text
cd android
bash ./run-tests.sh
bash ./gradlew :app:assembleRelease :app:lintRelease :wear:assembleRelease :wear:lintRelease --offline --console=plain
cd ..
python android/tests/verify-localization.py
git diff --check
adb devices -l
apksigner.bat verify --verbose --print-certs <최종 UI APK>
aapt.exe dump badging <최종 UI APK>
Get-FileHash <최종 UI APK> -Algorithm SHA256
```

모든 테스트·Release 빌드·리소스 검증이 성공했다. 전체 기존 self-test를 실행했으며, 별도 명시된 검증 수는 UsageInsights 2,181 / 이전 Ledger 67 / NEXT 계산·캡처·예측·내보내기 532 / 실제 DAO의 SQLite JVM fixture 73 / 계정·작업 수명주기 fixture 16 / 새 표시 규칙 14 assertions이다. SQLite JVM 검증은 Android 기기 실행 증거가 아니다.

한국어·영어 리소스 일치, 앱 문자열 960개·Wear 116개, formatter fixture 629개, 보호 파일 38개 및 검토 대상 9개 검사가 통과했다. 빌드 경고로 기존 unchecked 코드와 Gradle 차기 버전의 deprecated API 안내가 남았다.

lint는 휴대폰 78 warnings·2 hints, Wear 1 warning이다. 이전 휴대폰 68 warnings에서 증가한 10개는 교체된 화면의 옛 문자열을 보존해서 발생한 UnusedResources이다. 새 영어 문구의 복수형 후보 경고 2개는 문구를 수정하여 해결했다. 기존 lint baseline에 등록된 64 errors·115 warnings는 제외되어 있으며, lint 성공을 전체 원본 오류가 없다는 의미로 해석하지 않는다. baseline 자체는 변경하지 않았다.

## 최종 APK

- 파일: `delivery/2.8.2-ui-v2/CodexMeter-2.8.2-ko-ui-v2.apk`
- 패키지: `dev.bennett.codexmeter`
- versionName / versionCode: `2.8.2 / 32`
- minSdk / targetSdk: `26 / 36`
- SHA-256: `f3128f2fed9d936940d5f6e238eff2ed98da93ddedb5908f97afcc3c8d0114f7`
- 인증서 SHA-256: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`
- APK Signature Scheme v2 검증 성공. 원본 한국어판 2.8.1 APK의 인증서와 동일함을 다시 확인했다. 키 파일·암호는 전달 폴더에 포함하지 않았다.

새 UI APK는 이전 2.8.2 APK와 같은 버전 번호를 유지한 별도 빌드이다. 파일명으로 구분한다. 이전 최종 APK와 Wear APK를 덮어쓰지 않았다. Wear는 회귀 빌드·lint를 수행했고 화면 기능을 확장하지 않았다.

## 실기기 한계와 판정

ADB 목록에 기기가 없다. Android emulator 실행도 수행하지 않았다. 실제 삼성 기기 설치, 현재 설치본 서명 비교, 로그인·데이터 유지, 화면 렌더링·터치·스크롤·큰 글꼴·다크모드, 실제 API·위젯·AOD·Now Bar·Wear 테스트는 미완료다. 이번 결과를 실제 휴대폰 UI가 검증된 것으로 보고하지 않는다.

BUILD READY이며 설치용 파일과 안내를 준비했다. 설치본과의 직접 비교가 빠진 조건부 설치 준비 상태이고 DEVICE VERIFIED는 아니다. 기존 앱을 삭제하거나 데이터를 초기화하지 말고 새 APK를 열어 업데이트한다. 서명·버전 오류가 나오면 설치를 중단하고 기존 데이터를 유지한 채 진단해야 한다.
