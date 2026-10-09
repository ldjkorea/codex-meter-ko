# 2.8.7 공개 베타 검증 범위

기준: 기존 한국어판 2.8.6 / 36 (`6a2efd5`), 공개 버전 2.8.7 / 37.

## 로컬에서 확인한 것

- 전체 기존 회귀 검사와 한국어 리소스 검사를 통과했다. SQLite DAO·계정 전환·저장 실패·결제/티어/코치 계산은 JVM 및 SQLite fixture 검증이며 실제 계정에 요청하지 않았다.
- 추가 Plus/Pro 카운트다운 44개 assertion: 서버 상대/절대 시각, 관측 시각 유지, 우선순위, 누락, 지연, 매분 경과, 만료, 새 응답, 조회 실패, 오래된 캐시, 숨김, 한/영 표시, KST 날짜 경계.
- phone/Wear Release 컴파일 및 lint 통과. phone lint는 기존 기준 파일에서 53 errors/111 warnings를 제외했고 현재 보고서에 Warning 126개가 있다. Wear Warning 2개가 있다. 새 오류로 빌드가 중단되지 않았다는 의미이며 lint 문제가 전부 해결된 것은 아니다. 기존 WrongConstant·작은 글씨·번역 복수형·exported widget receiver 등의 후속 개선은 이번 범위에서 변경하지 않았다.
- 공식 Pretendard 1.3.9 파일 4개와 라이선스의 공식 출처/바이트 동일성 확인. 두 AAR의 기존 클래스와 나머지 ZIP entry 동일성 확인. source·AAR·최종 APK에 두 원본 금지 폰트 해시가 없음. 네이티브 렌더링 대신 리소스 연결, 라이브러리 호출 경로, Android 폰트 지원과 시스템 fallback 구현을 검토했다.
- 소스/fixture에서 토큰·개인 작업 경로·키·DB·로그를 검사했다. 인증/API/SQLite/저장/설정/Now Bar 등 핵심 15개 파일 및 공유/Wear 소스가 기존 파일과 동일함을 확인했다. 공개 이력은 새 snapshot에서 시작한다.
- 기존 APK와 새 APK의 동일 인증서, 동일 패키지, 36 → 37 버전 증가를 확인했다. 기존 소스/서명 파일/2.8.6 APK는 변경하지 않았다.

## 배포 APK

| 대상 | 패키지 | 버전 | 최소 OS | SHA-256 |
|---|---|---|---|---|
| 휴대폰 | dev.bennett.codexmeter | 2.8.7 / 37 | API 26 | fa58268c06dc4ab808e9cbb1918750fafb9b16c1c9bcf3200471d2fe5f4c7e98 |
| Wear | dev.bennett.codexmeter | 2.8.7 / 37 | API 30 | 022828c92e8c8ffa7813971e4f1b7e13bdfe496e71d58c902ccf981d59e4d32e |

인증서 SHA-256: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`.
휴대폰 APK v2/v3, Wear APK v3 서명 검증 통과.

GitHub runner의 실제 결과는 [Actions](https://github.com/ldjkorea/codex-meter-ko/actions)에서 해당 커밋으로 확인한다. 로컬 결과만으로 CI 성공을 주장하지 않는다. 배포자는 게시 후 저장소/Release에 로그인 없이 접근하고 자산을 다시 내려받아 위 해시와 비교한다.

## 아직 확인하지 못한 것

연결된 Android 기기와 설치된 에뮬레이터가 없어서 실기기 업데이트/데이터 보존, 로그인/API, 앱 화면, 큰 글씨/좁은 화면, 다크모드/OEM host, 삼성 잠금화면/AOD/Now Bar 및 Wear 동작은 실행하지 않았다. 시스템 글꼴 fallback은 기기마다 다를 수 있다. 사용자의 실제 계정·토큰·기록을 공개하거나 초기화하지 않았다. 최초 공개는 이 한계를 표시한 베타다.
