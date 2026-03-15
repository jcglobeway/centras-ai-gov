# OpenSpec Workflow

이 저장소의 중요한 변경은 `OpenSpec change` 단위로 관리한다.

## Directory Layout

- `openspec/changes`: 진행 중인 change
- `openspec/archive`: 완료된 change
- `openspec/templates`: 새 change 생성 시 복사할 템플릿

## Change Rules

1. 중요한 변경을 시작하기 전에 `openspec/changes/<change-id>/`를 만든다.
2. 최소 파일은 `proposal.md`, `tasks.md`, `status.md`다.
3. 설계 판단이 길어지면 `design.md`를 추가한다.
4. 구현 중에는 `status.md`와 체크박스를 갱신해 진행 상태를 남긴다.
5. change 완료 후 검증이 끝나면 Git 커밋을 만든다.
6. 커밋 후 change 디렉터리는 `openspec/archive/<change-id>/`로 이동한다.

## Naming

- `change-id`는 `kebab-case`로 작성한다.
- 예시: `add-session-repository`, `add-ingestion-write-contracts`

## Commit Rules

- 중요한 브레이크포인트마다 커밋한다.
- 커밋 메시지는 모두 한글로 작성한다.
- change 하나가 끝나면 해당 change 범위에 맞는 커밋을 만든다.

## Working Convention

- 구현 전: `proposal.md`에 목적과 범위를 적는다.
- 구현 중: `tasks.md` 체크박스와 `status.md` 상태를 갱신한다.
- 구현 후: 검증 결과와 남은 리스크를 `status.md`에 적고 커밋한다.
