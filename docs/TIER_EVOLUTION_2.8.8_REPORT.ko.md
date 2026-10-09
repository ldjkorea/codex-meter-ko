# Codex Meter 2.8.8 — Tier Evolution 개발·검증 보고서

2026-10-09 · 앱/Wear 2.8.8 / versionCode 38 · 패키지 `dev.bennett.codexmeter`

## 기준과 보존

실제 공개된 `v2.8.7-beta`와 공개 main `f720a050e7024f73e6b7ffea9822a7c89e66363d`를 확인한 뒤 별도 체크아웃·`feature/tier-evolution`에서 작업했다. 비공개 2.8.6 개발 원본과 upstream은 수정하지 않았다. 2.8.7 Release 자산도 교체하지 않는다.

OAuth·토큰 저장·UsageApi/Parser·SQLite DB·AppPreferences·SubscriptionStore·FunStore·Now Bar·5시간 초기화 계산·Pretendard 코어는 2.8.7과 동일함을 소스로 검사한다. WidgetRenderer는 새 provider 갱신/로컬 평가 스케줄러 호출만 추가한다. 기존 WidgetOptions·기존 위젯 디자인 및 Wear 기능 소스는 보존한다. Wear 버전만 맞춘다. 데이터 마이그레이션이나 앱 제거·초기화를 실행하지 않았다.

## 실제 디자인

독창적인 로컬 VectorDrawable 캐릭터 **Meter Scout** 10종과 카드 프레임을 추가했다. 기존 배지 10종은 캐릭터 오른쪽 아래의 독립 엠블럼으로 유지한다. 자산은 이 프로젝트용 원본 도형이며 외부 게임 캐릭터·이미지·폰트를 복제하지 않았다. MIT 소스 라이선스를 적용하며 기존 제3자 라이선스와 별개다.

| 티어 | 형태·재질·장식 |
|---|---|
| Iron | 회색 기본 장갑, 직선 프레임과 긁힘 |
| Bronze | 구리색 장갑과 돌출 핀 |
| Silver | 은색 윤곽, 측면 장갑 |
| Gold | 금색 이중 프레임, 추가 안테나 |
| Platinum | 청백 금속과 정교한 패널 |
| Emerald | 초록 수정 장식과 강화된 외곽 |
| Diamond | 파란 결정 날개와 보석 프레임 |
| Master | 보라색 장식, 위성 동료 |
| Grandmaster | 붉은 금속·왕관·대칭 장식 |
| Challenger | 금빛 왕관·큰 외곽 장식·완성형 장갑 |

매트 블랙/차콜 바탕을 유지한다. 홈 요약·티어·코치 카드, 활성 버튼·탭, 차트 선택 색은 티어 팔레트를 사용한다. 밝은 테마는 명도를 낮춘 팔레트를 사용한다. 일반 화면에는 공식·가중치·점수를 노출하지 않고 배치 중/잠정/주간 평가 완료, 이전 티어와 유지·승급·등급 조정, 다음 평가 구간을 보여준다.

승급은 900ms, 상위 티어의 첫 진입 효과는 1600ms의 작은 스케일 변화만 사용한다. 무한 애니메이터가 없고 화면 가림·분리 시 멈춘다. 시스템 애니메이션 OFF와 설정의 효과 OFF를 존중한다. 실제 프레임률·전력은 기기에서 측정하지 않았다.

`docs/tier-previews/index.html`, SVG 10종과 `tier-gallery.png`를 제공한다. PNG는 SVG 도형을 렌더링해 시각 확인했다. **Android 네이티브 화면이나 위젯 스크린샷이 아니다.**

## 장기 티어 평가

- 구간 종료 후 기존 `FunInsights.completed`가 인증한 정상 주간만 사용한다. 정확히 604800초 주간이며, 처음·마지막 관측이 경계에서 1시간 이내, 전체 구간의 80% 이상 관측, 최소 3개 관측, 연속 간격 6시간 이내, 초기화/보정/정책 충돌 및 legacy 원시 추정이 없어야 한다. 기존 인증 기준을 완화하지 않았다.
- 최근 11개 달력 주 범위에서 최대 8개의 유효 종료 주간을 선택한다. 최근 4개는 가중치 2, 나머지는 1. 가중 평균의 95%에 의미 있는 활용(60% 이상) 비중의 최대 5점과 ±2점으로 제한한 변화 추세를 반영한다. 낮은 사용률의 규칙적인 반복만으로 상위 등급이 되지 않는다.
- 임계값은 10/20/30/45/60/75/85/92/97이다. 1주로 배치하지 않고 2~3주는 잠정·최대 Platinum, 4~7주는 정식·최대 Master, 8주부터 상위 등급 가능하다. 초기 배치 후 한 번의 종료 평가에서 최대 한 단계만 움직인다. 상승에는 추가 2점 여유가 필요하다.
- 관측이 누락된 주는 0%로 생성하지 않는다. 현재 구간의 실시간 사용률로 장기 티어를 올리지 않는다. 한 번의 휴식 주는 강등하지 않는다. 연속된 유효 저사용 주와 반복된 낮은 평가가 있어야 한 단계 조정한다. 관측 공백 뒤에는 강등 연속성을 끊는다.
- 정책·플랜·계정은 분리한다. 새 주간 정책은 새 배치이며 이전 이력은 보관한다. 월간/5시간 기록은 주간에 섞지 않는다. 동일 종료 시각의 중복 평가를 막는다.

계산은 계정의 실제 활용 효율·생산성·토큰 비용이 아닌 **한도 활용 패턴에 대한 재미 등급**이다. 인증 기준이 엄격하므로 장시간 오프라인 사용자는 배치/잠정 상태가 오래 유지될 수 있다.

## 저장·이관

새 `codex_tier_evolution_v2` schema 2 저널을 계정+플랜 키별로 보관하고 그 안에서 정책별 `inputs`, `evaluations`, `state`, `legacy_imported`를 분리한다. 평가에 사용한 종료 시각 목록·규칙 버전·이전/새 티어·평가 시각을 보존한다. 최대 8주 계산과 별개로 인증된 주간 집계·평가 이력은 유지하며 원장 raw 보존 기간에 의존하지 않는다.

기존 결산 중 같은 정책·종료 ID와 `near_boundary_observations_no_interpolation` 품질을 가진 집계만 한 번 가져온다. 기존 FunStore의 결산·금액·규칙·옛 티어 문자열을 바꾸지 않는다. 새 평가 이력과 이전 기준의 결산을 화면에서 구분한다.

기존 네트워크 락과 계정 확인으로 동시/오래된 계정 쓰기를 차단한다. 동기 commit 실패는 이전 문서를 복원하고 재시도 가능하도록 실패를 전달한다. 파손 또는 범위 밖의 상태를 임의 티어로 표시하거나 읽기 과정에서 덮어쓰지 않는다. 원장 삭제는 기존 정책 그대로며 보관 결산·새 티어 저널·결제정보를 함께 지우지 않는다. 전체 계정 보관 데이터의 전용 내보내기/삭제 UI는 이번 범위에서 추가하지 않았다.

## 1~5개 진화형 위젯

홈 화면 위젯 선택 목록에 **Codex Meter · 진화형**을 추가한다. 기존 위젯을 자동 변환하거나 삭제하지 않는다.

5시간 잔여율 / 주간 잔여율 / 티어 엠블럼 / 5시간 초기화 / 주간 초기화의 모든 비어 있지 않은 31개 조합을 지원한다. 별도 `codex_evolution_widgets`에 widget ID별 선택·순서·불투명도·티어 테마 적용 여부를 저장한다. 미리보기는 저장하지 않으며 저장 버튼에서 한 위젯의 설정만 commit한다. 화면 회전 상태도 보존한다.

5시간 OFF는 두 항목 모두 렌더링에서 제거하고 빈 칸·확인불가·대체 5시간 항목을 만들지 않는다. 저장한 선택은 남겨 다시 ON이면 복원한다. 주간 OFF도 같은 정책이다. API 자체에 해당 한도가 없으면 기본에 억지로 생성하지 않는다. 모든 선택 항목이 숨겨지면 명확한 빈 상태를 표시한다. 좁은 폭·큰 글꼴·1~5개 수에 맞춰 열을 조절하며 높이가 부족하면 크기/구성 안내를 표시한다.

카운트다운은 마지막 정상 응답의 서버 reset 시각과 관측 시각에만 의존한다. fresh 조건에는 조회 실패 없음도 포함한다. 미제공·오래된 관측·시각 경과를 구분하며 RemoteViews Chronometer와 최대 15분 로컬 재렌더링을 사용한다. 사용량 서버를 추가 호출하지 않는다. OS의 Doze/백그라운드 제한으로 경계의 재렌더링이 늦어질 수 있으며 이를 기기에서 검증하지 않았다.

Samsung AOD/Now Bar/Wear는 기존 동작을 보호한다. 새 진화형 위젯은 **홈 화면 전용**이며 잠금화면/AOD에 새 provider를 추가하지 않았다.

## GitHub 업데이트

기존 업데이트 시스템의 소스를 `ldjkorea/codex-meter-ko`로 한정한다. own HTTPS Release의 정확한 tag/phone APK/`SHA256SUMS.txt` 조합만 파싱한다. Wear APK를 휴대폰 업데이트로 선택하지 않는다. beta tag는 APK의 숫자 버전과 연결하며 prerelease 구분은 유지한다.

APK 다운로드 크기 상한 150MiB, Release 크기와 파일 길이 일치, SHA-256 일치, 패키지 일치, 아래 고정 한국어 인증서, 현재 설치본과 동일 signer 및 **증가한 versionCode**를 모두 확인한 후 Android PackageInstaller의 사용자 확인을 요청한다. 자동·무인 설치하지 않는다. 화면 종료/스레드 취소는 다운로드 및 세션 복사·commit을 취소하고 실패 세션을 정리한다.

기본 확인은 하루 한 번, 베타 포함 채널이며 기존 설정은 보존한다. 업데이트 알림은 기본 OFF다. 원본 영문 저장소를 조회하지 않으며 GitHub 요청에 사용자 OAuth/사용량 토큰을 넣지 않는다. 2.8.7에는 업데이트 기능이 비활성화돼 있으므로 이번 APK는 한 번 수동 설치해야 한다. 실제 Android 설치·사용자 취소·OEM 백그라운드 알림은 미검증이다.

## 변경 파일

| 영역 | 파일 |
|---|---|
| 평가 엔진 | shared `TierEvolution`, `KoreanUpdateTrust`, `EvolutionElements` |
| 저널·복구 | app `TierStore`, `TierEvaluationScheduler` |
| 디자인·이력 | `TierTheme`, `TierCompanionView`, `TierPresentation`, `evo_0..9.xml`, EN/KO evolution_strings |
| 홈/분석 연결 | `FunHome`, `FunActivity`, `Ui`, `LedgerDashboard`, `LedgerTrendView`, `MainActivity`의 저널 revision 표시 갱신, `RecordSettings` 효과 옵션 |
| 위젯 | `EvolutionWidget`, `EvolutionWidgetConfigActivity`, 3개 layout, provider XML/manifest, `WidgetRenderer.updateAll` 연결 |
| 업데이트 | `GitHubReleaseSource/Parser`, `UpdatePreferences`, `UpdateActivity`, `UpdateInstaller` |
| 버전/검사 | app/Wear Gradle, AppConstants, build.sh/run-tests.sh, 새 pure/file tests, 기존 source guards의 명시적 reviewed patches, CI checkout depth |
| 문서/미리보기 | README, CHANGELOG, Release Notes, Release policy, 이 보고서, SVG/HTML/PNG 및 로컬 자산 생성/렌더링 도구 |

기존 과거 fixture의 내용·해시를 재생성하지 않았다. 기존 source guard의 승인된 연결/버전 변경은 좁은 before/after 패치로 명시하고 나머지 보호 검사를 유지한다. 비공개 개발 Git 이력을 공개하지 않는다.

## 자동 검증

실행 명령:

```bash
bash android/run-tests.sh
python android/tests/verify-localization.py
python android/tests/verify-publication.py <phone.apk> <Wear.apk>
cd android
bash gradlew :app:assembleRelease :app:lintRelease :wear:assembleRelease :wear:lintRelease --offline --console=plain
```

핵심 수치화 검사: 기존 3242 + 새 pure 214 + 실제 TierStore 파일 기반 JVM fixture 16 = **3472 assertions**. 별도 formatter 629 + Plus/Pro 초기화 44 = **673 assertions**. 원래 ParserSelfTest와 소스 guard도 실행하며 수치화 합계에는 포함하지 않는다.

새 테스트는 100% 단일 주·꾸준한 저사용·꾸준한 고사용·성장/하락·한 번의 휴식·여러 휴식·관측 누락·정책 격리·중복 평가·등급 이동 한 단계 제한·옛 결산 이관·재개방·계정 변경·로그아웃·쓰기 실패·파손 상태와 31개 위젯 조합·OFF/ON·신뢰하지 않는 URL/패키지/서명/versionCode를 포함한다.

실제 생산 TierStore를 파일 기반 Android preference fixture에서 실행한다. SQLite 검사도 기존 실제 DAO를 JVM fixture에서 실행한다. **Android 프로세스 종료/기기 재부팅 증거는 아니다.** 124개 기존 계산/저장/표면 파일의 guard, 별도 37개 현지화 보호 파일과 새 2.8.7 보호 핵심도 검사한다. 휴대폰 EN/KO 1327문자열, Wear 116문자열, 형식 지정자 검사와 10종 형상 차별성을 확인한다.

티어 10종+배치 중의 dark/light 강조색 및 버튼 글자 조합은 계산상 대비 4.5:1 이상을 확인한다. 원래 dark 보조 글자의 카드 대비는 7.06~8.56:1이다. 실제 화면 밝기·OEM 색상·TalkBack의 동작은 별도 기기 확인이 필요하다.

작업 중 새 lint의 잘못된 들여쓰기, 새 위젯 접근성 설명·스위치/생성자 경고를 고쳤다. 기존 lint baseline은 갱신하거나 완화하지 않았다. 최종 오류·경고 및 APK 검사 결과는 아래 산출물 검증에 기록한다.

## 실기기 미검증 및 판정

ADB 연결 목록이 비어 있고 사용 가능한 AVD도 없어 설치하거나 데이터를 변경하지 않았다. 실제 설치본의 인증서와 비교하지 못했다. 공개 2.8.7 서명과 최종 APK 인증서 비교는 별도 산출물 검사로 수행한다.

**갤럭시 설치/로그인 상태 보존/실제 API 조회/SQLite 저장/UI/위젯 런처 배치·크기·회전·재부팅/큰 글씨/다크모드/Doze/알림/설치 승인·취소/AOD/Now Bar/Wear는 DEVICE VERIFIED가 아니다.** 단시간의 JVM·소스 검사나 자산 PNG를 이 검증으로 대신하지 않는다.

정확한 공개 APK 검증 결과와 체크섬은 Release의 `VERIFICATION.ko.md`, `SHA256SUMS.txt`를 참고한다. BUILD READY를 목표로 하며 INSTALL READY는 동일 한국어 서명·패키지·더 낮은 설치 versionCode 조건이다. 기기에 직접 설치해 확인한 완제품이라고 주장하지 않고 베타로 배포한다.

## 최종 산출물 검증

휴대폰 APK 25,089,720 bytes, SHA-256 `1ef653bf7d250907da74447bd57644b34697c38010d34d83524bdf9e8602320e`.

Wear APK 15,861,641 bytes, SHA-256 `ddb8870c32f4ae23841b3d482ac8b2e7b2d738c0c0e794c1a811c31d26428753`.

둘 다 versionName 2.8.8 / versionCode 38 / 동일 패키지이며 APK Signature v2 검증에 통과했다. 인증서 SHA-256은 `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`이다. 공개 2.8.7 APK를 비로그인 다운로드해 기존 SHA-256과 인증서를 확인했으며 새 파일과 같은 인증서다. 실제 기기에 설치된 앱의 서명 확인은 하지 못했다.

최종 lint의 unfiltered Error는 0개. 휴대폰 Warning 136개/Hint 2개, Wear Warning 9개다. 2.8.7의 126/2에서 휴대폰은 티어/업데이트 UI 교체로 사용하지 않게 된 문자열 10개가 추가 경고다. 기존 문자열은 삭제하지 않았다. Wear의 기존 2개에 의존성 새 버전 안내 7개가 더해졌으며 기능 소스나 의존성 버전은 바꾸지 않았다. 기존 phone baseline의 Error 53개/Warning 111개는 그대로 필터링된다. baseline을 수정하거나 새 오류를 숨기지 않았다. Gradle 10 호환성 경고도 남는다.

공식 Pretendard 4개 weight, OFL 원문, 두 패치 AAR, 금지 폰트 부재 및 최종 두 APK의 MIT/OFL assets를 검사했다. 52개 archive를 검사했다. Windows checkout에서 라이선스 텍스트 줄바꿈이 변환되는 문제를 발견해 공개 소스의 원문 바이트를 복원하고 `.gitattributes`로 라이선스 자동 변환을 막았다. 137개 라이선스/고지 파일의 원본 내용은 바꾸지 않았다.

업데이트 화면의 기존 알파/동일 versionCode/재설치/앱 삭제 안내를 교정했다. 동일·이전 버전은 GitHub 이력 보기만 제공하며 설치 버튼·설치 권한 요청·자동 설치 진입을 차단한다. 베타 채널의 내부 저장 값은 호환성을 위해 `alpha`를 유지한다. 조회 실패는 새 위젯에서도 오래된 관측과 별도 문구로 표시한다.
