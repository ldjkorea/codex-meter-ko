# 기여 안내

한국어 수정본의 이슈와 PR은 한국어판 저장소로 보냅니다. 원본 프로젝트는 별도로 운영됩니다.

기존 기능·영어 기본 리소스·인증·저장 호환성을 보존하고 최소 범위로 수정하세요. `bash android/run-tests.sh`와 `python android/tests/verify-localization.py`를 실행하세요. Android 빌드·lint와 실제 기기 확인은 수행 여부를 구분해서 기록하세요.

배포 키·비밀번호·토큰·실제 사용자 DB·원본 진단 로그를 소스나 PR에 포함하지 마세요. 테스트에는 가상 데이터를 사용하세요. 일반 PR은 서명 없는 빌드만 수행합니다. 공개 버전 변경과 Release는 docs/RELEASE_POLICY.ko.md에 따릅니다.
