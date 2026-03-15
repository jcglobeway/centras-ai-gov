# Development WBS And Milestones

## 1. Purpose

이 문서는 전체 개발 진행을 `중요 단위`, `선행 관계`, `완료 기준`으로 추적하기 위한 WBS다.
상세 API/화면/DB 규약은 기존 문서를 따르고, 이 문서는 `무엇을 어떤 순서로 끝낼지`를 고정한다.

관련 기준 문서:
- [07_delivery_plan.md](/C:/Users/User/Documents/work/mvp_docs/07_delivery_plan.md)
- [08_traceability_matrix.md](/C:/Users/User/Documents/work/mvp_docs/08_traceability_matrix.md)
- [10_auth_authz_api.md](/C:/Users/User/Documents/work/mvp_docs/10_auth_authz_api.md)
- [16_springboot_kotlin_ddd_msa_review.md](/C:/Users/User/Documents/work/mvp_docs/16_springboot_kotlin_ddd_msa_review.md)

## 2. Milestone Overview

### M1. Platform Foundation

- 목표: 관리자 세션, 역할, 조직 범위, 감사 기반을 고정한다.
- 완료 기준: `GET /admin/auth/me`와 권한 검증이 저장소 경계를 통해 동작한다.

### M2. Ingestion Operations

- 목표: crawl source 등록, job 실행, 상태 전이, worker callback 흐름을 닫는다.
- 완료 기준: source 생성부터 job 상태 갱신까지 API와 worker stub이 연결된다.

### M3. Chat Runtime

- 목표: 질문, 답변, retrieval 로그, citation 저장 흐름을 구현한다.
- 완료 기준: chat 요청 단위로 질문, 답변, 검색 근거를 trace할 수 있다.

### M4. QA Improvement Loop

- 목표: unresolved 목록, 상세 추적, QA 판정 저장, 후속 조치 연결을 구현한다.
- 완료 기준: QA 담당자가 검수 후 상태 전이를 완료할 수 있다.

### M5. Metrics And Delivery

- 목표: KPI 집계, 운영 대시보드, 고객사 대시보드, 회귀 테스트 팩을 완성한다.
- 완료 기준: 운영사/고객사가 서로 다른 KPI를 보고 운영 개선 루프를 닫을 수 있다.

## 3. WBS Tree

```mermaid
mindmap
  root((Public RAG Ops MVP))
    M1 Foundation
      Auth Session Store
      Role Action Policy
      Organization Scope
      Audit Log Baseline
    M2 Ingestion Ops
      Crawl Source CRUD
      Ingestion Job Lifecycle
      Worker Callback
      Document Version Link
    M3 Chat Runtime
      Question API
      Retrieval Adapter
      Citation Mapping
      Chat Trace Log
    M4 QA Loop
      Unresolved Queue
      Question Detail Trace
      QA Review Write
      Resolution Status Sync
    M5 Metrics Delivery
      Daily KPI Batch
      Ops Dashboard
      Client Dashboard
      Regression Pack
```

## 4. Milestone Dependency

```mermaid
flowchart LR
    M1[Milestone 1<br/>Platform Foundation]
    M2[Milestone 2<br/>Ingestion Operations]
    M3[Milestone 3<br/>Chat Runtime]
    M4[Milestone 4<br/>QA Improvement Loop]
    M5[Milestone 5<br/>Metrics And Delivery]

    M1 --> M2
    M1 --> M3
    M2 --> M3
    M3 --> M4
    M2 --> M4
    M3 --> M5
    M4 --> M5
```

## 5. Execution Timeline

```mermaid
gantt
    title MVP Development Milestones
    dateFormat  YYYY-MM-DD
    axisFormat  %m/%d

    section Foundation
    Auth session store              :done, foundation1, 2026-03-15, 2d
    Action auth policy              :done, foundation2, after foundation1, 2d
    Session lifecycle API           :done, foundation3, after foundation2, 2d
    Organization scope repository   :active, foundation4, after foundation3, 2d
    Audit log baseline              :foundation5, after foundation4, 2d

    section Ingestion Ops
    Crawl source query and command  :done, ingestion1, 2026-03-15, 2d
    Worker callback contract        :ingestion2, after foundation2, 3d
    Document version linkage        :ingestion3, after ingestion2, 2d
    Retry and rerun flow            :ingestion4, after ingestion3, 2d

    section Chat Runtime
    Question and answer contract    :chat1, after foundation3, 3d
    Retrieval adapter               :chat2, after ingestion2, 3d
    Citation response mapping       :chat3, after chat2, 2d
    Chat trace persistence          :chat4, after chat3, 2d

    section QA Loop
    Unresolved queue                :qa1, after chat4, 2d
    Question detail trace           :qa2, after qa1, 2d
    QA review write                 :qa3, after qa2, 2d
    Resolution sync                 :qa4, after qa3, 2d

    section Metrics And Delivery
    KPI batch                       :metrics1, after chat4, 2d
    Ops and client dashboards       :metrics2, after metrics1, 3d
    API and E2E regression pack     :metrics3, after qa4, 3d
    Hardening and release cut       :metrics4, after metrics2, 2d
```

## 6. Current Progress Cut

- 완료:
  - 저장소 부트스트랩
  - `auth/me` 개발용 세션 복원
  - 로그인/로그아웃과 세션 만료/폐기 API
  - ingestion 조회/쓰기 API
  - ingestion 상태 전이 규칙
- 진행 중:
  - 조직/권한 저장소화
- 다음 착수:
  - worker callback 계약
  - 실제 세션 저장소 영속화
  - ingestion 회귀 테스트 확장

## 7. Change Tracking Rule

- Milestone 변경은 `OpenSpec change` 단위로 쪼갠다.
- 하나의 change는 하나의 완료 가능한 단위를 가진다.
- change 완료 후 이 문서의 `Current Progress Cut`과 Gantt 상태를 함께 갱신한다.
