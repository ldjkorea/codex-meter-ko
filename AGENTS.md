# 한국어 Android 공개 후보 작업 지침

원본: BenItBuhner/Codex-Meter, MIT. 소스 기준은 docs/SOURCE_PROVENANCE.md에 기록한다.

- 작업 루트는 android/, 앱·공유·Wear 구조와 영어 기본 리소스를 보존한다.
- 원본 remote에 push하지 않는다. 기존 사용자 데이터·서명 키를 수정하거나 새 키를 만들지 않는다.
- 사용자 승인 없이 공개 저장소 생성·push·태그·Release 게시를 하지 않는다.
- 두 의존성의 기존 삼성 폰트는 공식 Pretendard로 교체했다. 공개 전 verify-publication.py로 출처·패치된 AAR·최종 APK의 금지 폰트 해시 부재를 확인한다. 기존 원본 AAR를 되돌려 넣지 않는다.
- 빌드 키가 없으면 unsigned만 만든다. 공개 CI에 secrets나 Release 쓰기 권한을 추가하지 않는다.
- 기존 회귀 검사 및 한국어 resource 검사를 유지한다. 과거 소스 fixture를 임의 갱신하여 실패를 숨기지 않는다.
- 버전은 앱/Wear/상수/build.sh/test guards/CHANGELOG를 함께 맞춘다.
- source/JVM 검사, 컴파일/lint, 실제 기기, 외부 서비스 성공을 분리해서 보고한다.
- iOS는 원본 참고 소스로 보존되어 있지만 한국어판 지원·릴리스 대상이 아니다.
