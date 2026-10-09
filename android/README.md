# Android 개발 안내

`app/`은 휴대폰, `wear/`는 Wear OS, `shared/`는 공통 모델입니다. 앱·Wear는 2.8.7 / 37입니다.

JDK 21, Android SDK 36/37.0, Build Tools 36.0.0을 사용합니다. 루트 README의 빌드 명령과 docs/BUILD_SECURITY.ko.md를 참고하세요.

기존 한국어 키가 없는 환경의 Gradle Release는 unsigned입니다. `build.sh`는 기존 로컬 키가 없으면 실패하며 자동 생성하지 않습니다. 키·개인 토큰·사용자 데이터를 저장소에 넣지 마세요.

`vendor/m2`는 로컬 빌드 검증용 기존 캐시이며, 폰트 재배포 권리 검증 전에는 현재 캐시와 APK를 공개하지 않습니다. 원본 자동 서명 workflow 및 암호화된 release key는 이 후보에서 제외했습니다.
