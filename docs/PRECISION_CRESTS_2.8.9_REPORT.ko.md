# Codex Meter 2.8.9 — Precision Crests

2026-10-09 · phone/Wear 2.8.9 / versionCode 39 · `dev.bennett.codexmeter`

## 실제 적용

승인된 금속·연산 코어 시안을 기준으로 Iron부터 Challenger까지 투명 배경 엠블럼 10종을 개별 제작했다. 로봇 얼굴 및 작은 배지 겹침을 활성 UI에서 제거하고 홈 160dp, 분석 144dp, 진화형 홈 위젯 64dp에 같은 `TierTheme.EMBLEMS` 매핑을 사용한다. 비배치 상태의 흐린 표시와 기존 짧은 승급 효과·효과 OFF·시스템 동작 축소 설정은 유지한다. 티어 이름은 기존 영문 체계이며 삼족오/왕실/정치 문양은 앱에 적용하지 않는다.

실제 APK에 넣은 WebP 10종은 총 705,864 bytes다. 각 이미지는 512×512, alpha를 실제로 확인했으며 `drawable-nodpi`로 저장해 밀도별 과도한 확대 디코딩을 피한다. 한 이미지 ARGB 디코딩 크기는 1,048,576 bytes다. 위젯은 기존 144×144 Bitmap 출력으로 제한한다. 런타임 이미지 다운로드나 새 의존성은 없다. 기존 VectorDrawable는 이전 자산과 회귀 근거 보존을 위해 삭제하지 않는다.

`docs/precision-crests/tier-gallery.png`는 실제 배포 자산을 어두운 배경과 밝은 배경의 48px 크기로 렌더링해 확인한 것이다. Android Activity/RemoteViews 화면 캡처가 아니다. 해시·크기·리소스 매핑은 `ASSETS.json`에 기록한다. 생성 방식과 디자인 기준은 `PROMPTS.md`를 참고한다.

## 자동 업데이트

2.8.8에 이미 있던 한국어 저장소 자동 확인·사용자 승인 다운로드/설치 경로를 유지한다. 신규 설정은 하루 한 번 확인, 베타 포함 채널이 기본이다. 기존 자동 확인 OFF·채널·주기·알림 설정을 덮어쓰지 않는다. 알림은 기본 OFF이며 사용자가 허용할 수 있다. 자동 다운로드/무인 설치를 새로 추가하지 않는다.

코드 감사에서 `buildUpdateCard`가 홈 재구성에 연결되지 않고 새 Release broadcast에도 홈 렌더 signature가 바뀌지 않는 것을 발견했다. 캐시에 새 버전이 있으면 로그인 상태와 관계없이 홈에 업데이트 카드를 넣고 updater preference 상태를 signature에 포함하도록 최소 수정했다. 이 표시는 추가 GitHub/사용량 요청을 만들지 않는다.

설정 경로는 **기록 → 설정 → 앱 정보 및 라이선스 → 업데이트**다. 여기서 자동으로 확인, 확인 주기, 업데이트 알림과 베타 포함 채널을 선택할 수 있다. OS 백그라운드 정책으로 주기 실행이 늦어질 수 있다. 크기·SHA-256·패키지·고정 한국어 서명·설치본과 동일 signer·증가한 versionCode 검사와 Android 설치 확인을 그대로 사용한다.

## 보존

실제 2.8.8 커밋 `567ee1a`를 기준으로 UsageApi/Parser, SQLite DB, 계정·토큰, AppPreferences, 구독/결산 저장, TierStore/EvaluationScheduler/Evolution 계산, 기존 위젯 설정·한도 표시, 업데이트 설정/Installer/Client/Scheduler, Now Bar 등 18개 계약 소스를 byte 비교한다. 기존 회귀 검사도 유지한다. 옛 fixture를 재생성하지 않고, 홈 엠블럼 크기·업데이트 카드 추가는 좁은 reviewed patch로 기록했다. 이전 APK·키·원장·티어 기록을 삭제하거나 이관하지 않는다. Wear 기능 소스는 그대로이고 버전만 맞춘다. upstream은 수정/push하지 않는다.

## 검증

```bash
bash android/run-tests.sh
python android/tests/verify-localization.py
python android/tests/verify-precision-crests.py <phone.apk>
python android/tests/verify-publication.py <phone.apk> <Wear.apk>
cd android
bash gradlew :app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease --offline --console=plain
```

기존 전체 pure/JVM/파일·SQLite fixture와 새 리소스·연결 검사가 통과했다. 기존 합계 3,472 assertions와 별도 현지화/초기화 673 assertions를 유지한다. phone EN/KO 1,327 문자열, Wear 116 문자열 및 형식 지정자 검사를 통과했다. APK 리소스 테이블에서 `prestige_0..9`의 실제 파일명을 찾아 10개 승인 이미지와 해시가 일치함을 확인했다. Release의 ZIP 경로는 최적화 과정에서 짧아지므로 원래 파일명으로 찾는 검사를 리소스 테이블 기반으로 수정했다. 검사를 생략하지 않았다.

phone/Wear Release 및 lint 성공. 새 lint Error 0; phone Warning 146/Hint 2, Wear Warning 9. 2.8.8의 phone 136보다 10개 늘어난 것은 교체 후 보존한 `evo_0..9`의 UnusedResources다. 기존 baseline의 Error 53/Warning 111은 그대로이며 이번에 해결했다고 주장하지 않는다. baseline을 바꾸거나 새 경고를 숨기지 않았다. 기존 unchecked Java/Gradle deprecation 안내도 남는다.

최종 APK v2 서명, 기존 2.8.8과 동일 인증서, 버전/패키지 및 Pretendard/제3자 고지를 검증했다. 인증서 SHA-256:

`6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`

휴대폰 25,798,610 bytes · SHA-256:

`3af5e32d5df73dbb8205fbcfb7d280c744b75ed5f8ca9d0b4bafbbe7c995550b`

Wear 15,861,641 bytes · SHA-256:

`795477b533a1b48ace604c48abb474f62d8c6a3cd0bedea893c233cf683ced49`

## 기기 및 배포 범위

ADB 연결 목록이 비어 있고 로컬 AVD 폴더도 없다. 기기에 설치하거나 데이터를 변경하지 않았다. 실제 갤럭시 업데이트·로그인/API·홈/분석 UI·위젯 런처/큰 글씨/회전/재부팅·Doze·PackageInstaller 승인/취소·Wear/AOD/Now Bar는 미검증이다. 그림 렌더링, JVM, 컴파일을 기기 검증으로 간주하지 않는다.

BUILD READY. INSTALL READY는 동일 한국어 서명·패키지와 낮은 설치 versionCode 조건이며 실제 설치본 인증서 비교는 미완료다. DEVICE VERIFIED는 아니다. 공개 베타 게시 및 외부 다운로드/생산 업데이트 파서 검증 결과는 Release의 최종 검증 보고서에 따로 기록한다.
