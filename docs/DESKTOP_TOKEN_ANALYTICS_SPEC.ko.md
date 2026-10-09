# Phase 3 — Desktop Token Analytics 후속 기술 명세

상태: 설계만 작성. 수집기·Android 가져오기·동기화·토큰 기반 결제/한도 추정은 구현하지 않았다.

## 목적과 데이터 경계

Windows PC에서 관측한 Codex 처리 토큰을 서울 날짜별, 프로젝트·세션·실제 로그의 모델/추론 수준별로 분석한다. Android의 계정 한도 사용률과 함께 시간축으로 비교할 수 있게 하되 두 데이터셋의 단위를 유지한다. PC 처리 토큰은 계정 전체의 한도 차감량, 구독 결제 토큰 또는 사용자의 모든 기기 활동이 아니다.

이번 설계에서 현재 스레드 JSONL의 이벤트 필드명만 확인했다. `event_msg / token_count`에 `info.total_token_usage`, `info.last_token_usage`, `model_context_window`가 있고, 카운터에 input_tokens, cached_input_tokens, cache_write_input_tokens, output_tokens, reasoning_output_tokens, total_tokens가 있다. `turn_context`의 model/effort도 존재한다. 이 한 파일의 구조를 모든 버전의 계약으로 일반화하지 않는다. 이 파일에서 `token_usage_record` 형식을 확인하지 못했으므로 해당 명칭은 별도 어댑터 후보로 취급한다. 토큰 수/대화/인증 정보를 수집·전송하는 제품 코드를 추가하지 않았다.

## 수집기와 어댑터

후속 구현은 사용자가 명시적으로 허용한 로컬 세션 디렉터리를 읽는 opt-in Windows 프로세스로 한다. 활성 파일은 공유 읽기로 열고 완성된 UTF-8 JSONL 행만 처리한다. 파일 잠금, 부분 마지막 행, 회전, truncation, 복사/재배치, 프로세스 중단을 다룬다. 읽기 위치와 집계는 같은 로컬 트랜잭션에 commit한다. 원본 로그는 수정하지 않는다.

어댑터 `token_count-v1`과 `token_usage_record-<verified-version>`를 분리한다. 각 어댑터는 필드의 누적/증분 의미, counter 범위(세션/turn/request), epoch 리셋 조건, 부분집합 규칙과 출처 버전을 fixture로 증명해야 한다. 타입 이름만 보고 같은 의미로 처리하지 않는다. 미지원 형식/카운터는 unsupported/unknown으로 남긴다.

프로젝트는 로컬 cwd/메타데이터와 사용자의 경로 매핑을 통해 분류한다. 원본 경로·저장소 이름은 PC 내부 매핑에만 남긴다. 모바일 전달용 project_key/session_key는 로컬 키를 쓰는 가명 식별자이며, 표시 이름은 사용자 선택 별칭만 쓴다. 키는 APK 서명키와 무관하며 이번 단계에서 만들지 않는다. model_id/reasoning_effort는 해당 turn의 명시적인 메타데이터로 조인한다. 누락 값을 임의로 GPT-6.1 Sol High 등으로 채우지 않는다.

## 데이터 모델

| 데이터 | 주요 열/제약 |
| --- | --- |
| source_checkpoint | adapter/version, 로컬 source_key, byte_offset, file_generation; 완성 행 commit 후 갱신 |
| session_metadata | session_key, project_key, 생성 시각/coverage; 원본 경로는 로컬 별도 mapping |
| counter_checkpoint | session_key, counter_scope, epoch, event_key, occurred_at_utc, 이전 원시 누적 카운터 |
| token_delta | event_key UNIQUE, session_key/project_key/turn_key, 모델/추론, 발생 시각 UTC, 증가량, 품질, adapter/version |
| daily_token_aggregate | day/timezone + project/session/model/effort 단위 증가량 합계와 coverage |
| import_manifest | bundle_id/hash, source adapter, schema/version, coverage interval, 선택된 별칭 |

event_key는 출처의 안정적인 event/request ID를 우선한다. ID가 없으면 가명 session + counter_scope/epoch + 정규화된 카운터/turn 정보의 해시를 사용하는 어댑터별 전략을 검증한다. 시각 하나만으로 고유성을 판단하지 않는다. 동일 내용의 복사 파일은 같은 source/session으로 식별한다. 원문 line hash만 쓰면 재포맷/메타데이터 추가로 중복될 수 있으므로 의미가 같은 카운터의 dedup도 필요하다.

## 누적·중복·캐시 처리

1. 같은 이벤트에 total 누적값과 last 증분값이 함께 있으면 하나의 정본만 집계한다. 누적값을 쓰면서 last를 추가하지 않는다. 다른 응답 메시지에 같은 counter snapshot이 다시 나와도 증가량은 한 번만 저장한다.
2. 누적 카운터는 같은 세션/범위/epoch의 이전 값과 차이를 낸다. 수집기가 중간부터 읽었고 이전 기준점이 없다면 첫 누적값 전체를 그날의 소비로 확정하지 않는다. baseline_unknown으로 두고 다음 유효 차이부터 집계한다. 세션 시작 누적 0이 증명되는 경우만 전체 시작 증가량을 사용할 수 있다.
3. 카운터가 감소하면 음수 증가나 0으로 덮은 정상 사용을 만들지 않는다. 명시적 epoch 리셋/compaction인지 보정인지 어댑터로 판별하고 불명확한 구간을 격리한다. 출력 카운터만 감소한 부분 보정도 검사한다.
4. out-of-order/정정 이벤트는 해당 epoch의 뒤쪽 차이와 날짜 집계를 재계산하는 deterministic upsert로 처리한다. 기존 증가량 위에 수정분을 단순 추가하지 않는다. 다시 읽기/재시작/복사 파일/중복 응답에서도 같은 결과여야 한다.
5. source 의미가 검증되면 비캐시 입력 = input - cached_input이다. cached_input은 입력의 부분집합으로 별도 합산하면 이중 계수가 된다. cached > input은 오류이며 음수 비캐시 입력을 0으로 은폐하지 않는다. cache_write_input은 별도 항목으로 보관하고 해당 어댑터의 정의 없이 입력/캐시 읽기에 더하지 않는다.
6. reasoning_output은 output의 부분집합인지 검증한다. 부분집합이면 output에 재가산하지 않는다. total_tokens를 모든 이름의 합으로 재구성하지 않는다. 어댑터별 정의와 값의 일관성을 확인하고 불일치는 quality finding으로 보존한다.
7. 모델별 처리 토큰은 명시적 turn attribution이 있는 구간만 배분한다. 한 누적 변화가 여러 모델/turn을 걸치고 배분 근거가 없으면 unattributed로 남긴다. 세션 마지막 모델로 전체를 배분하지 않는다.

## 시간대와 Android 연동

발생 시각과 수집 시각을 분리해 UTC로 저장하고 서울 00:00~24:00로 표시한다. 구간 토큰 증가량이 날짜 경계를 넘어갔고 source에서 응답별 시각/증분을 제공하지 않으면 해당 구간을 미배분으로 남긴다. 계정 한도 관측과는 시간대/범위를 맞춘 두 그래프를 제공하며 한도%p당 토큰 같은 환산계수를 산출하지 않는다. 여러 기기의 미수집 활동과 관측 간격 차이를 설명한다.

첫 연동은 사용자가 선택한 집계 JSON 파일과 Android SAF import를 우선한다. 대화·툴 입출력·원 JSONL·인증 정보·계정 식별자·원본 파일 경로를 내보내지 않는다. 별도 외부 서버는 기본 구조에 없다. bundle/schema/hash/단위/coverage를 검증하고 import_manifest로 중복 가져오기를 막는다. Android 사용량 원장과 별도 DB/table에 보관해 기존 계정 사용률을 덮어쓰지 않는다. 사용자 명시적 삭제, 보존 기간, 파일 전송 여부를 후속 구현에서 확정한다.

## 후속 개발 순서와 수락 테스트

1. 한 런타임/어댑터의 실제 의미를 확인하고 민감 정보 없는 synthetic JSONL fixture를 만든다.
2. PC 로컬 DB와 파일 tail/checkpoint를 구현한다. 부분행, 중단/재시작, 파일 회전/복사, 동일 counter 반복, total+last 동시, epoch 감소/정정/역순, cache subset, 모델 전환, 첫 baseline 누락, 서울 날짜 경계를 테스트한다.
3. PC 일별/프로젝트/세션/모델·추론 집계 화면과 coverage 표시를 검증한다. PC 밖 활동을 계정 전체로 보고하지 않는다.
4. 최소 집계 export/import를 구현한 뒤 Android 기존 원장·로그아웃·SAF·언어/테마·파일 검증 회귀를 수행한다.

결제비용/API 가격/프로젝트별 예상 구독 한도 비용은 범위 밖이다. 향후 요청 시에도 날짜별 근거와 API 환산 추정이라는 표기를 별도로 요구한다.
