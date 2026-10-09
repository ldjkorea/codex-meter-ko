> 최신 한국어 베타: **2.8.10 / versionCode 40**. 기록 캘린더 강조와 실제 구독료 계산 연결 검증을 포함합니다. [다운로드](https://github.com/ldjkorea/codex-meter-ko/releases/tag/v2.8.10-beta). 기존 앱을 삭제하지 않고 업데이트하세요.

# Codex Meter 한국어판

Codex 사용량을 확인하는 **비공식 Android 앱**입니다. 한국어 화면, 사용 기록, 티어 배지, 코치 말투, 직접 입력한 구독료에 따른 활용도 참고값을 제공합니다. 매트 블랙 디자인과 삼성 위젯을 유지한 무료 Android APK입니다.

원작은 [BenItBuhner/Codex-Meter](https://github.com/BenItBuhner/Codex-Meter)이며, Bennett의 MIT 라이선스와 저작권 고지를 유지합니다. 이 수정본은 OpenAI·Samsung·원작자가 제공하거나 보증하는 공식 앱이 아닙니다. 한국어 수정·배포 준비: [ldjkorea](https://github.com/ldjkorea).

**현재 베타: 2.8.9 / versionCode 39, `v2.8.9-beta`.** 이전 공개 APK는 보존합니다. 실제 Android 기기 검증은 아직 수행하지 않은 베타입니다.

[릴리스와 한국어 설치 안내](https://github.com/ldjkorea/codex-meter-ko/releases/tag/v2.8.9-beta) · [휴대폰 APK 다운로드](https://github.com/ldjkorea/codex-meter-ko/releases/download/v2.8.9-beta/CodexMeter-2.8.9-ko.apk)

## Android 설치

1. 저장소 **Releases**에서 최신 한국어판을 엽니다. `Source code.zip`은 설치 파일이 아닙니다.
2. 스마트폰에서는 `CodexMeter-<버전>-ko.apk`를 내려받습니다. `CodexMeter-Wear-<버전>-ko.apk`는 Wear OS 시계용입니다.
3. 다운로드 폴더에서 APK를 열고 필요할 때 해당 브라우저/파일 앱의 **이 출처의 앱 설치 허용**을 켭니다.
4. 기존 한국어판과 서명이 같다면 앱을 삭제하지 않고 업데이트합니다. 설치 후 출처 설치 권한을 다시 끌 수 있습니다.
5. 앱 버전, 로그인, 기록, 새로고침을 확인합니다. Android 시스템 언어에 따라 한국어를 적용하며 기본 영어 리소스도 유지합니다.

휴대폰 Android 8.0(API 26) 이상, Wear API 30 이상이 필요합니다. 실제 기기 설치·업데이트·로그인·Samsung AOD/Now Bar·Wear 화면은 이번 공개 준비에서 검증하지 않았습니다.

원본 영문 앱이나 다른 제작자의 같은 패키지 앱은 서명 차이 때문에 덮어쓸 수 없습니다. 패키지 `dev.bennett.codexmeter`를 유지하므로 동시에 설치할 수도 없습니다. 설치 실패 시 앱 제거나 데이터 초기화부터 하지 마세요. 기록 내보내기는 인증·설정·결제정보 전체 백업이 아닙니다. 오류 내용과 설치된 버전을 먼저 확인하세요.

## 기능과 한계

- API가 반환한 5시간·주간·월간 사용량을 표시하고 기록이 충분할 때만 사용 속도와 전망을 계산합니다.
- 한국어 화면, 기록, 티어 배지와 코치 말투를 제공합니다. 티어는 앱 내부의 재미 요소이며 OpenAI 공식 등급이 아닙니다.
- 직접 등록한 결제금액과 기간을 기반으로 구독 활용도 참고값을 표시합니다. API 비용·절약액·생산성 측정값이 아닙니다. 코치 문구에 별도 AI 호출을 사용하지 않습니다.
- 위젯·알림·선택적 사용량 모니터·Wear 연동을 포함합니다. 삼성 전용 화면 동작은 기기와 펌웨어에 따라 달라질 수 있습니다.

새 [Precision Crests 티어 디자인](docs/precision-crests/tier-gallery.png)과 [2.8.9 변경·검증 보고서](docs/PRECISION_CRESTS_2.8.9_REPORT.ko.md)를 제공합니다. 입체 금속·연산 코어 엠블럼 10종을 홈·분석·진화형 위젯에 공통 적용합니다. 미리보기는 실제 배포 자산을 렌더링한 것으로 Android 화면 캡처는 아닙니다. [이전 티어 평가 설계](docs/TIER_EVOLUTION_2.8.8_REPORT.ko.md)는 보존합니다.

## 로그인·개인정보·보안

로그인은 브라우저의 OpenAI 인증 화면을 사용합니다. 인증 토큰은 Android Keystore를 사용하는 AES-GCM 방식으로 기기에 저장됩니다. 사용 기록·결제정보·설정은 기기 내부에 보관됩니다. 내보낸 기록이나 진단 파일은 공유 전에 직접 확인하세요. 공개 이슈에 토큰·계정 식별자·결제정보·원본 로그를 올리지 마세요.

공용 백엔드나 광고·분석 서버는 없습니다. 인증·사용량 및 사용자가 선택한 작업을 위해 OpenAI/ChatGPT에 직접 요청하며 Wear 연동은 Google Play Services를 사용합니다. 2.8.8부터 공개 한국어 저장소의 GitHub 릴리스를 확인합니다. 업데이트 조회·다운로드에는 계정 토큰을 전송하지 않으며 자동 설치하지 않습니다.

ChatGPT 내부 사용량 경로와 삼성 전용 메타데이터는 안정된 제3자 API 계약이 아닙니다. 서비스 변경으로 로그인/조회가 중단될 수 있습니다. 이 저장소는 정식 보안 감사를 통과한 제품이나 상시 지원 서비스를 주장하지 않습니다.

## 업데이트

2.8.7 이하 사용자는 Releases에서 최신 APK를 한 번 수동 설치합니다. 2.8.8 이상은 앱의 **기록 → 설정 → 앱 정보 및 라이선스 → 업데이트**에서 확인할 수 있습니다. 베타 포함 채널과 하루 한 번 자동 확인을 기본으로 하며, 저장한 기존 채널·확인 설정은 유지합니다. 2.8.9부터 확인된 새 버전은 홈의 업데이트 카드에도 표시합니다. 알림은 사용자가 켜야 합니다. 다운로드 크기·SHA-256·패키지·한국어 인증서·증가한 versionCode를 검사한 뒤 Android 설치 확인을 요청합니다. 자동 확인은 OS의 백그라운드 정책에 따라 늦어질 수 있습니다. 다운로드 중 화면을 닫으면 작업을 취소합니다. 기기 내 설치·취소·업데이트 알림은 아직 실기기 검증 전입니다.

정식 versionCode를 증가시키며 기존 한국어 서명 키를 유지합니다. 동일 버전의 공개 APK를 조용히 교체하지 않습니다. [버전·배포 정책](docs/RELEASE_POLICY.ko.md)을 참고하세요.

## 빌드·자동 검증

프로젝트 루트는 `android/`입니다. JDK 21, Android SDK Platform 36 및 37.0, Build Tools 36.0.0이 필요합니다.

```bash
bash android/run-tests.sh
python android/tests/verify-localization.py
python android/tests/verify-publication.py
cd android
bash gradlew :app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease --console=plain
```

서명 키 없는 사본은 unsigned Release APK를 만듭니다. 설치용 배포 파일이 아닙니다. `android/build.sh`는 기존 `.local-signing/` 키와 password가 없으면 실패하며 새 키를 만들지 않습니다. 일반 기여자는 한국어 배포 키를 받지 않습니다.

기존 회귀 검사는 필요한 과거 소스를 SHA-256으로 검증하는 fixture로 보관하여 비공개 개발 Git 이력을 공개하지 않아도 실행됩니다. JVM fixture는 실제 기기 렌더링·로그인 검사가 아닙니다.

Actions는 검사·unsigned 컴파일·lint만 실행하며 **서명·태그·Release 게시를 수행하지 않습니다**. `contents: read`, 인증정보 저장 비활성화, Action 커밋 SHA 고정을 적용합니다. 배포 개인키를 Actions에 제공하지 않으며 설치용 APK는 검증한 로컬 빌드만 수동 게시합니다. [빌드·서명 전략](docs/BUILD_SECURITY.ko.md)을 참고하세요.

## 라이선스·지원

원본과 수정 소스의 [MIT LICENSE](LICENSE)를 보존합니다. 포함된 모든 라이브러리·폰트에 MIT가 적용되는 것은 아닙니다. [제3자 고지](THIRD_PARTY_NOTICES.md)에 별도 라이선스와 Pretendard 교체 내역을 적었습니다. 공식 Pretendard 1.3.9의 수정하지 않은 글꼴을 SIL OFL 1.1로 포함하며 APK에도 고지를 동봉합니다.

개인 개발자의 가능한 범위에서 지원합니다. 제보에는 앱/Android 버전, 기기 종류, 민감정보를 가린 재현 절차를 포함하세요. 수정 시점이나 서비스 지속을 보장하지 않습니다. 원본 프로젝트에 한국어 수정본 문제를 대신 접수하지 마세요.

[원본 README](docs/UPSTREAM_README.md) · [출처](docs/SOURCE_PROVENANCE.md) · [한국어 변경사항](RELEASE_NOTES.ko.md)
