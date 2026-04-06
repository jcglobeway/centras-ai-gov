-- V031: DB 리셋으로 사라진 데모 데이터 복구
-- 대상: daily_metrics_org V023 컬럼 재채우기, feedbacks/qa_reviews/audit_logs/ragas_evaluations 시드

-- ── daily_metrics_org V023 컬럼 재채우기 ──────────────────────────────────────

UPDATE daily_metrics_org SET
    auto_resolution_rate      = 0.6800,
    escalation_rate           = 0.2100,
    explicit_resolution_rate  = 0.4200,
    estimated_resolution_rate = 0.6100,
    revisit_rate              = 0.0820,
    after_hours_rate          = 0.3400,
    avg_session_turn_count    = 2.30,
    knowledge_gap_count       = 5,
    unanswered_count          = 7,
    low_satisfaction_count    = 3
WHERE organization_id = 'org_local_gov';

UPDATE daily_metrics_org SET
    auto_resolution_rate      = 0.7200,
    escalation_rate           = 0.1600,
    explicit_resolution_rate  = 0.4800,
    estimated_resolution_rate = 0.6700,
    revisit_rate              = 0.0610,
    after_hours_rate          = 0.2900,
    avg_session_turn_count    = 2.10,
    knowledge_gap_count       = 3,
    unanswered_count          = 4,
    low_satisfaction_count    = 2
WHERE organization_id = 'org_central_gov';

UPDATE daily_metrics_org SET
    auto_resolution_rate      = 0.7500,
    escalation_rate           = 0.1400,
    explicit_resolution_rate  = 0.5100,
    estimated_resolution_rate = 0.7000,
    revisit_rate              = 0.0530,
    after_hours_rate          = 0.2600,
    avg_session_turn_count    = 1.90,
    knowledge_gap_count       = 2,
    unanswered_count          = 3,
    low_satisfaction_count    = 1
WHERE organization_id = 'org_acc';

-- ── eval_session + questions (feedbacks/qa_reviews FK 의존) ────────────────────

INSERT INTO chat_sessions (id, organization_id, service_id, channel, user_key_hash, started_at, ended_at, created_at) VALUES
('eval_session_acc', 'org_acc', 'svc_acc_chatbot', 'web', 'uhash_eval_acc', '2026-03-15 09:00:00', '2026-03-15 09:30:00', '2026-03-15 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO questions (id, organization_id, service_id, chat_session_id, question_text, question_intent_label, channel, created_at) VALUES
('question_42022d71', 'org_acc', 'svc_acc_chatbot', 'eval_session_acc', '아시아문화전당 관람 예약 방법을 알려주세요', 'reservation_inquiry', 'web', '2026-03-15 09:01:00'),
('question_6fd278ee', 'org_acc', 'svc_acc_chatbot', 'eval_session_acc', '어린이 교육 프로그램 일정이 궁금합니다', 'schedule_inquiry', 'web', '2026-03-15 09:10:00'),
('question_f25994fe', 'org_acc', 'svc_acc_chatbot', 'eval_session_acc', '주차장 이용 요금 안내해 주세요', 'info_inquiry', 'web', '2026-03-15 09:20:00')
ON CONFLICT (id) DO NOTHING;

-- ── feedbacks ──────────────────────────────────────────────────────────────────

INSERT INTO feedbacks (id, organization_id, service_id, question_id, session_id, rating, comment, channel, feedback_type, clicked_link, clicked_document, target_action_type, target_action_completed, dwell_time_ms, submitted_at) VALUES
('feedback_001', 'org_acc', 'svc_acc_chatbot', 'question_42022d71', 'eval_session_acc', 5, '답변이 정확하고 친절했습니다.', 'web', 'explicit', false, true, 'reservation', true, 45000, NOW() - INTERVAL '2' DAY),
('feedback_002', 'org_acc', 'svc_acc_chatbot', 'question_6fd278ee', 'eval_session_acc', 3, '원하는 정보를 찾기 어려웠습니다.', 'web', 'explicit', false, false, NULL, false, 32000, NOW() - INTERVAL '1' DAY),
('feedback_003', 'org_acc', 'svc_acc_chatbot', 'question_f25994fe', 'eval_session_acc', 4, NULL, 'web', 'implicit', true, false, NULL, false, 28000, NOW() - INTERVAL '6' HOUR)
ON CONFLICT (id) DO NOTHING;

-- ── qa_reviews ─────────────────────────────────────────────────────────────────

INSERT INTO qa_reviews (id, question_id, review_status, root_cause_code, action_type, action_target_id, review_comment, reviewer_id, reviewed_at, created_at) VALUES
('qa_rev_001', 'question_42022d71', 'resolved',         'A02', 'document_updated', NULL, '문서 최신화 완료', 'usr_qa_001', NOW() - INTERVAL '1' DAY, NOW() - INTERVAL '2' DAY),
('qa_rev_002', 'question_6fd278ee', 'confirmed_issue',  'A01', 'document_added',   NULL, '관련 문서 부재 확인, 추가 필요', 'usr_qa_001', NOW() - INTERVAL '12' HOUR, NOW() - INTERVAL '1' DAY),
('qa_rev_003', 'question_f25994fe', 'pending',          NULL,  NULL,               NULL, NULL, 'usr_qa_001', NOW(), NOW() - INTERVAL '6' HOUR)
ON CONFLICT (id) DO NOTHING;

-- ── audit_logs ─────────────────────────────────────────────────────────────────

INSERT INTO audit_logs (id, actor_user_id, actor_role_code, organization_id, action_code, resource_type, resource_id, request_id, result_code, created_at) VALUES
('audit_001', 'usr_super_001',   'super_admin',      NULL,          'ADMIN_LOGIN',     'admin_session', NULL,           'req_a001', 'success', NOW() - INTERVAL '3' DAY),
('audit_002', 'usr_ops_global_001', 'ops_admin',     NULL,          'ADMIN_LOGIN',     'admin_session', NULL,           'req_a002', 'success', NOW() - INTERVAL '2' DAY),
('audit_003', 'usr_qa_001',      'qa_manager',       'org_acc',     'QA_REVIEW_UPDATE','qa_review',     'qa_rev_001',   'req_a003', 'success', NOW() - INTERVAL '1' DAY),
('audit_004', 'usr_qa_001',      'qa_manager',       'org_acc',     'QA_REVIEW_UPDATE','qa_review',     'qa_rev_002',   'req_a004', 'success', NOW() - INTERVAL '12' HOUR),
('audit_005', 'usr_client_acc_001', 'client_org_admin', 'org_acc',  'ADMIN_LOGIN',     'admin_session', NULL,           'req_a005', 'success', NOW() - INTERVAL '6' HOUR)
ON CONFLICT (id) DO NOTHING;

-- ── ragas_evaluations ──────────────────────────────────────────────────────────

INSERT INTO ragas_evaluations (id, question_id, faithfulness, answer_relevancy, context_precision, context_recall, judge_provider, judge_model, evaluated_at) VALUES
('ragas_eval_001', 'question_42022d71', 0.8412, 0.7923, 0.8100, 0.7600, 'ollama', 'qwen2.5:7b', NOW() - INTERVAL '1' DAY),
('ragas_eval_002', 'question_6fd278ee', 0.6234, 0.7105, 0.6800, 0.6300, 'ollama', 'qwen2.5:7b', NOW() - INTERVAL '1' DAY)
ON CONFLICT (id) DO NOTHING;
