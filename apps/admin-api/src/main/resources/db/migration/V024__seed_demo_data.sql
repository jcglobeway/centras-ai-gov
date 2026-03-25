-- ── 데모용 목업 시드 데이터 ────────────────────────────────────────────────────
-- 14일치 지표, 다수의 질문/답변/QA리뷰/문서/인제스션잡을 추가한다.

-- ── chat_sessions ─────────────────────────────────────────────────────────────

INSERT INTO chat_sessions (id, organization_id, service_id, channel, user_key_hash, started_at, ended_at, created_at) VALUES
('chat_s_003', 'org_local_gov', 'svc_welfare', 'web',    'uhash_003', '2026-03-15 10:00:00', '2026-03-15 10:12:00', '2026-03-15 10:00:00'),
('chat_s_004', 'org_local_gov', 'svc_welfare', 'mobile', 'uhash_004', '2026-03-15 11:30:00', '2026-03-15 11:38:00', '2026-03-15 11:30:00'),
('chat_s_005', 'org_local_gov', 'svc_welfare', 'web',    'uhash_005', '2026-03-16 09:05:00', '2026-03-16 09:22:00', '2026-03-16 09:05:00'),
('chat_s_006', 'org_local_gov', 'svc_welfare', 'web',    'uhash_006', '2026-03-16 14:00:00', '2026-03-16 14:09:00', '2026-03-16 14:00:00'),
('chat_s_007', 'org_local_gov', 'svc_welfare', 'kiosk',  'uhash_007', '2026-03-17 09:50:00', '2026-03-17 10:01:00', '2026-03-17 09:50:00'),
('chat_s_008', 'org_central_gov', 'svc_faq',     'web',    'uhash_008', '2026-03-15 10:20:00', '2026-03-15 10:31:00', '2026-03-15 10:20:00'),
('chat_s_009', 'org_central_gov', 'svc_faq',     'mobile', 'uhash_009', '2026-03-15 13:45:00', '2026-03-15 13:52:00', '2026-03-15 13:45:00'),
('chat_s_010', 'org_central_gov', 'svc_faq',     'web',    'uhash_010', '2026-03-16 10:10:00', '2026-03-16 10:19:00', '2026-03-16 10:10:00'),
('chat_s_011', 'org_central_gov', 'svc_faq',     'web',    'uhash_011', '2026-03-17 11:00:00', '2026-03-17 11:14:00', '2026-03-17 11:00:00'),
('chat_s_012', 'org_local_gov', 'svc_welfare', 'web',    'uhash_012', '2026-03-17 15:30:00', '2026-03-17 15:44:00', '2026-03-17 15:30:00');

-- ── questions ─────────────────────────────────────────────────────────────────

INSERT INTO questions (id, organization_id, service_id, chat_session_id, question_text, question_intent_label, question_category, answer_confidence, failure_reason_code, is_escalated, channel, created_at) VALUES
-- Seoul welfare - answered (정상 응답)
('q_004', 'org_local_gov', 'svc_welfare', 'chat_s_003', '긴급복지지원 신청 방법을 알려주세요', 'welfare_application',  'application_guide',  0.91, NULL,  FALSE, 'web',    '2026-03-15 10:01:00'),
('q_005', 'org_local_gov', 'svc_welfare', 'chat_s_003', '의료급여 1종 대상 기준이 어떻게 되나요', 'eligibility_check',   'eligibility',        0.88, NULL,  FALSE, 'web',    '2026-03-15 10:05:00'),
('q_006', 'org_local_gov', 'svc_welfare', 'chat_s_004', '장애인 복지카드 재발급 절차가 궁금합니다', 'document_request',   'card_reissue',       0.85, NULL,  FALSE, 'mobile', '2026-03-15 11:31:00'),
('q_007', 'org_local_gov', 'svc_welfare', 'chat_s_005', '기초생활수급자 주거급여 신청서류 목록 주세요', 'document_request', 'application_guide',  0.93, NULL,  FALSE, 'web',    '2026-03-16 09:06:00'),
-- Seoul welfare - fallback / no_answer (미결)
('q_008', 'org_local_gov', 'svc_welfare', 'chat_s_005', '2026년 서울시 청년수당 신청 일정 알려줘', 'schedule_inquiry',   'schedule',           0.32, 'A03', FALSE, 'web',    '2026-03-16 09:14:00'),
('q_009', 'org_local_gov', 'svc_welfare', 'chat_s_006', '외국인 등록증 소지자 복지 혜택 범위가 어디까지인가요', 'eligibility_check', 'eligibility', 0.21, 'A05', FALSE, 'web',    '2026-03-16 14:01:00'),
('q_010', 'org_local_gov', 'svc_welfare', 'chat_s_007', '저소득 노인 무료 틀니 지원 대상자 기준 알려주세요', 'eligibility_check', 'eligibility', 0.19, 'A02', TRUE,  'kiosk',  '2026-03-17 09:51:00'),
('q_011', 'org_local_gov', 'svc_welfare', 'chat_s_012', '서울시 긴급지원 생계비 신청 후 처리 기간은요', 'status_check',      'processing_time',    0.29, 'A03', FALSE, 'web',    '2026-03-17 15:31:00'),
-- Busan FAQ - answered
('q_012', 'org_central_gov', 'svc_faq',     'chat_s_008', '부산시청 민원실 운영시간 알려주세요', 'general_info',        'hours_location',     0.97, NULL,  FALSE, 'web',    '2026-03-15 10:21:00'),
('q_013', 'org_central_gov', 'svc_faq',     'chat_s_008', '건축물 사용승인 신청서 어디서 받나요', 'document_request',    'form_inquiry',       0.82, NULL,  FALSE, 'web',    '2026-03-15 10:25:00'),
('q_014', 'org_central_gov', 'svc_faq',     'chat_s_009', '부산시 주민등록 등초본 발급 방법', 'document_request',     'certificate',        0.90, NULL,  FALSE, 'mobile', '2026-03-15 13:46:00'),
-- Busan FAQ - fallback / no_answer (미결)
('q_015', 'org_central_gov', 'svc_faq',     'chat_s_010', '부산시 2026년 도시재생 뉴딜 신청 접수 안내', 'policy_inquiry',    'policy',             0.18, 'A01', FALSE, 'web',    '2026-03-16 10:11:00'),
('q_016', 'org_central_gov', 'svc_faq',     'chat_s_010', '해운대구 공영주차장 무료 이용 조건이 어떻게 되나요', 'general_info', 'parking',           0.27, 'A04', FALSE, 'web',    '2026-03-16 10:15:00'),
('q_017', 'org_central_gov', 'svc_faq',     'chat_s_011', '부산시 청소년 문화패스 발급 기준 및 사용처', 'policy_inquiry',    'youth_policy',       0.22, 'A02', FALSE, 'web',    '2026-03-17 11:01:00'),
('q_018', 'org_central_gov', 'svc_faq',     'chat_s_011', '외국인 주민등록번호 없이 민원 신청 가능한가요', 'eligibility_check', 'eligibility',       0.15, 'A05', TRUE,  'web',    '2026-03-17 11:08:00');

-- ── answers ───────────────────────────────────────────────────────────────────

INSERT INTO answers (id, question_id, answer_text, answer_status, response_time_ms, citation_count, fallback_reason_code, created_at) VALUES
-- answered
('ans_004', 'q_004', '긴급복지지원은 위기상황에 처한 저소득층을 신속하게 지원하는 제도입니다. 주민센터에 방문하거나 복지로(www.bokjiro.go.kr)에서 온라인으로 신청 가능합니다.', 'answered',  1150, 4, NULL,           '2026-03-15 10:01:06'),
('ans_005', 'q_005', '의료급여 1종 수급권자는 국민기초생활 수급자 중 근로능력이 없는 분이 해당합니다. 18세 미만, 65세 이상, 임산부, 중증질환자 등이 포함됩니다.', 'answered', 1080, 3, NULL,           '2026-03-15 10:05:06'),
('ans_006', 'q_006', '장애인 복지카드 재발급은 읍면동 주민센터에서 신청하시면 됩니다. 필요서류는 신청서, 신분증, 사진 1매입니다.', 'answered',  940, 3, NULL,           '2026-03-15 11:31:06'),
('ans_007', 'q_007', '주거급여 신청서류: 신청서, 금융정보 제공 동의서, 신분증 사본, 임대차계약서 사본, 통장 사본이 필요합니다.', 'answered',  870, 5, NULL,           '2026-03-16 09:06:06'),
-- fallback / no_answer (미결로 노출됨)
('ans_008', 'q_008', '요청하신 정보를 정확히 찾지 못했습니다. 서울시 청년포털(youth.seoul.go.kr)에서 최신 일정을 확인해 주세요.', 'fallback',  620, 0, 'LOW_CONFIDENCE', '2026-03-16 09:14:06'),
('ans_009', 'q_009', '해당 내용에 대한 정보가 지식베이스에 존재하지 않습니다.', 'no_answer',  410, 0, 'ZERO_RESULT',    '2026-03-16 14:01:06'),
('ans_010', 'q_010', '관련 정보를 찾지 못했습니다. 보건복지상담센터(129)에 문의해 주시기 바랍니다.', 'fallback',   530, 0, 'LOW_CONFIDENCE', '2026-03-17 09:51:06'),
('ans_011', 'q_011', '처리 기간 관련 최신 정보를 확인하지 못했습니다. 담당 주민센터에 직접 문의해 주세요.', 'fallback',  490, 0, 'OUT_OF_SCOPE',   '2026-03-17 15:31:06'),
-- Busan answered
('ans_012', 'q_012', '부산시청 민원실은 평일 09:00~18:00 운영합니다. 점심시간(12:00~13:00)에도 일부 창구가 운영됩니다.', 'answered',  780, 2, NULL,           '2026-03-15 10:21:06'),
('ans_013', 'q_013', '건축물 사용승인 신청서는 부산시청 건축행정 홈페이지 또는 민원24에서 내려받을 수 있습니다.', 'answered',  850, 3, NULL,           '2026-03-15 10:25:06'),
('ans_014', 'q_014', '주민등록 등초본은 주민센터 방문, 정부24(gov.kr), 무인민원발급기를 통해 발급 가능합니다.', 'answered',  920, 4, NULL,           '2026-03-15 13:46:06'),
-- Busan fallback / no_answer
('ans_015', 'q_015', '해당 사업의 2026년 일정은 현재 지식베이스에 등록되어 있지 않습니다.', 'no_answer',  360, 0, 'ZERO_RESULT',    '2026-03-16 10:11:06'),
('ans_016', 'q_016', '공영주차장 무료 이용 조건에 대한 상세 정보를 찾지 못했습니다.', 'fallback',   510, 0, 'LOW_CONFIDENCE', '2026-03-16 10:15:06'),
('ans_017', 'q_017', '청소년 문화패스 관련 정보가 최신화되어 있지 않습니다. 부산시 청소년지원과에 문의해 주세요.', 'fallback',  570, 0, 'OUT_OF_DATE',    '2026-03-17 11:01:06'),
('ans_018', 'q_018', '외국인 민원 신청 관련 정보는 지식베이스에서 찾을 수 없었습니다.', 'no_answer',  390, 0, 'ZERO_RESULT',    '2026-03-17 11:08:06');

-- ── qa_reviews ────────────────────────────────────────────────────────────────

INSERT INTO qa_reviews (id, question_id, review_status, root_cause_code, action_type, action_target_id, review_comment, reviewer_id, reviewed_at, created_at) VALUES
('qa_demo_001', 'q_009', 'confirmed_issue', 'missing_doc',   'add_document',   NULL,      '외국인 복지 안내 문서가 누락되어 있음. 문서 추가 필요.', 'usr_qa_001', '2026-03-16 16:00:00', '2026-03-16 16:00:00'),
('qa_demo_002', 'q_015', 'confirmed_issue', 'outdated_doc',  'update_document','doc_302',  '2026년 도시재생 일정 변경 내용이 반영되지 않음.',        'usr_qa_001', '2026-03-16 17:00:00', '2026-03-16 17:00:00'),
('qa_demo_003', 'q_018', 'pending',          NULL,            NULL,             NULL,      NULL,                                                          'usr_qa_001', '2026-03-17 12:00:00', '2026-03-17 12:00:00');

-- ── documents ─────────────────────────────────────────────────────────────────

INSERT INTO documents (id, organization_id, document_type, title, source_uri, version_label, published_at, ingestion_status, index_status, visibility_scope, last_ingested_at, last_indexed_at, created_at, updated_at) VALUES
('doc_303', 'org_local_gov', 'policy',  '서울시 긴급복지지원 사업 안내 2026',         'https://seoul.example.go.kr/welfare/emergency-2026.pdf', 'v2.1', '2026-01-10 00:00:00', 'completed', 'indexed',     'public', '2026-01-11 00:00:00', '2026-01-11 00:00:00', '2026-01-11 00:00:00', '2026-01-11 00:00:00'),
('doc_304', 'org_local_gov', 'notice',  '2026 서울시 청년수당 모집 공고',             'https://seoul.example.go.kr/youth/notice-2026.html',     'v1.0', '2026-02-01 00:00:00', 'completed', 'indexed',     'public', '2026-02-02 00:00:00', '2026-02-02 00:00:00', '2026-02-02 00:00:00', '2026-02-02 00:00:00'),
('doc_305', 'org_local_gov', 'faq',     '의료급여 수급자 FAQ',                       'https://seoul.example.go.kr/welfare/medical-faq.html',   'v1.3', '2025-11-01 00:00:00', 'completed', 'indexed',     'public', '2026-01-05 00:00:00', '2026-01-05 00:00:00', '2026-01-05 00:00:00', '2026-03-01 00:00:00'),
('doc_306', 'org_local_gov', 'policy',  '장애인 복지서비스 종합 안내서',              'https://seoul.example.go.kr/welfare/disability-guide.pdf','v3.0', '2026-03-01 00:00:00', 'pending',   'not_indexed', 'public', NULL,                 NULL,                 '2026-03-10 00:00:00', '2026-03-10 00:00:00'),
('doc_307', 'org_central_gov', 'notice',  '부산시 2026 도시재생 뉴딜 신청 안내',       'https://busan.example.go.kr/urban/newdeal-2026.html',    'v1.0', '2026-02-20 00:00:00', 'failed',    'not_indexed', 'public', NULL,                 NULL,                 '2026-02-20 00:00:00', '2026-03-10 00:00:00'),
('doc_308', 'org_central_gov', 'faq',     '부산시청 민원서비스 안내 FAQ',               'https://busan.example.go.kr/civil/faq.html',             'v2.5', '2025-12-01 00:00:00', 'completed', 'indexed',     'public', '2026-01-10 00:00:00', '2026-01-10 00:00:00', '2026-01-10 00:00:00', '2026-01-10 00:00:00'),
('doc_309', 'org_central_gov', 'policy',  '해운대구 공영주차장 이용 안내',              'https://busan.example.go.kr/haeundae/parking.pdf',       'v1.1', '2025-10-01 00:00:00', 'completed', 'indexed',     'public', '2026-02-01 00:00:00', '2026-02-01 00:00:00', '2026-02-01 00:00:00', '2026-02-01 00:00:00'),
('doc_310', 'org_central_gov', 'notice',  '2026 부산시 청소년 문화패스 운영 안내',      'https://busan.example.go.kr/youth/culture-pass.html',    'v1.0', '2026-03-05 00:00:00', 'completed', 'indexed',     'public', '2026-03-06 00:00:00', '2026-03-06 00:00:00', '2026-03-06 00:00:00', '2026-03-06 00:00:00');

-- ── ingestion_jobs ────────────────────────────────────────────────────────────

INSERT INTO ingestion_jobs (id, organization_id, service_id, crawl_source_id, document_id, job_type, job_stage, job_status, runner_type, trigger_type, attempt_count, error_code, requested_at, started_at, finished_at, created_at) VALUES
('ing_job_303', 'org_local_gov', 'svc_welfare', 'crawl_src_001', 'doc_303', 'crawl', 'complete', 'succeeded', 'python_worker', 'scheduled', 1, NULL,           '2026-03-10 01:00:00', '2026-03-10 01:01:00', '2026-03-10 01:18:00', '2026-03-10 01:00:00'),
('ing_job_304', 'org_local_gov', 'svc_welfare', 'crawl_src_001', NULL,      'crawl', 'fetch',    'running',   'python_worker', 'manual',    1, NULL,           '2026-03-18 06:00:00', '2026-03-18 06:01:00', NULL,                 '2026-03-18 06:00:00'),
('ing_job_305', 'org_central_gov', 'svc_faq',     'crawl_src_002', 'doc_307', 'crawl', 'complete', 'failed',    'python_worker', 'scheduled', 3, 'PARSE_ERROR',  '2026-03-12 22:00:00', '2026-03-12 22:01:00', '2026-03-12 22:08:00', '2026-03-12 22:00:00'),
('ing_job_306', 'org_central_gov', 'svc_faq',     'crawl_src_002', 'doc_310', 'crawl', 'complete', 'succeeded', 'python_worker', 'scheduled', 1, NULL,           '2026-03-06 00:00:00', '2026-03-06 00:01:00', '2026-03-06 00:15:00', '2026-03-06 00:00:00'),
('ing_job_307', 'org_local_gov', 'svc_welfare', 'crawl_src_001', 'doc_306', 'crawl', 'extract',  'failed',    'python_worker', 'manual',    2, 'HTTP_403',     '2026-03-11 09:00:00', '2026-03-11 09:01:00', '2026-03-11 09:04:00', '2026-03-11 09:00:00');

-- ── daily_metrics_org (14일치) ─────────────────────────────────────────────────
-- 기존: metric_001 (2026-03-14, seoul), metric_002 (2026-03-14, busan)
-- 추가: 2026-03-05 ~ 2026-03-13, 2026-03-15 ~ 2026-03-18 (서울·부산 각각)

INSERT INTO daily_metrics_org (id, metric_date, organization_id, service_id, total_sessions, total_questions, resolved_rate, fallback_rate, zero_result_rate, avg_response_time_ms, created_at) VALUES
-- 서울 (완만한 개선 추세)
('m_s_0305', '2026-03-05', 'org_local_gov', 'svc_welfare', 101, 148, 80.10, 13.50, 6.40, 1380, '2026-03-06 00:00:00'),
('m_s_0306', '2026-03-06', 'org_local_gov', 'svc_welfare', 108, 157, 80.90, 12.80, 6.30, 1360, '2026-03-07 00:00:00'),
('m_s_0307', '2026-03-07', 'org_local_gov', 'svc_welfare', 105, 155, 81.30, 12.60, 6.10, 1340, '2026-03-08 00:00:00'),
('m_s_0308', '2026-03-08', 'org_local_gov', 'svc_welfare',  98, 142, 82.00, 12.10, 5.90, 1320, '2026-03-09 00:00:00'),
('m_s_0309', '2026-03-09', 'org_local_gov', 'svc_welfare',  87, 121, 81.70, 12.40, 5.90, 1310, '2026-03-10 00:00:00'),
('m_s_0310', '2026-03-10', 'org_local_gov', 'svc_welfare', 112, 165, 82.50, 11.90, 5.60, 1290, '2026-03-11 00:00:00'),
('m_s_0311', '2026-03-11', 'org_local_gov', 'svc_welfare', 115, 170, 83.10, 11.50, 5.40, 1270, '2026-03-12 00:00:00'),
('m_s_0312', '2026-03-12', 'org_local_gov', 'svc_welfare', 118, 174, 83.80, 11.20, 5.00, 1250, '2026-03-13 00:00:00'),
('m_s_0313', '2026-03-13', 'org_local_gov', 'svc_welfare', 117, 172, 84.20, 10.90, 4.90, 1230, '2026-03-14 00:00:00'),
('m_s_0315', '2026-03-15', 'org_local_gov', 'svc_welfare', 122, 181, 85.80, 10.10, 4.10, 1210, '2026-03-16 00:00:00'),
('m_s_0316', '2026-03-16', 'org_local_gov', 'svc_welfare', 126, 188, 86.10,  9.90, 4.00, 1200, '2026-03-17 00:00:00'),
('m_s_0317', '2026-03-17', 'org_local_gov', 'svc_welfare', 129, 193, 86.50,  9.60, 3.90, 1190, '2026-03-18 00:00:00'),
('m_s_0318', '2026-03-18', 'org_local_gov', 'svc_welfare', 131, 197, 86.90,  9.40, 3.70, 1170, '2026-03-18 23:00:00'),
-- 부산 (완만한 개선 추세)
('m_b_0305', '2026-03-05', 'org_central_gov', 'svc_faq',     71,  85, 74.20, 18.10, 7.70,  980, '2026-03-06 00:00:00'),
('m_b_0306', '2026-03-06', 'org_central_gov', 'svc_faq',     74,  89, 74.80, 17.70, 7.50,  965, '2026-03-07 00:00:00'),
('m_b_0307', '2026-03-07', 'org_central_gov', 'svc_faq',     72,  87, 75.30, 17.40, 7.30,  960, '2026-03-08 00:00:00'),
('m_b_0308', '2026-03-08', 'org_central_gov', 'svc_faq',     68,  81, 75.90, 17.00, 7.10,  955, '2026-03-09 00:00:00'),
('m_b_0309', '2026-03-09', 'org_central_gov', 'svc_faq',     62,  74, 75.50, 17.20, 7.30,  958, '2026-03-10 00:00:00'),
('m_b_0310', '2026-03-10', 'org_central_gov', 'svc_faq',     77,  92, 76.20, 16.80, 7.00,  950, '2026-03-11 00:00:00'),
('m_b_0311', '2026-03-11', 'org_central_gov', 'svc_faq',     79,  95, 76.80, 16.40, 6.80,  945, '2026-03-12 00:00:00'),
('m_b_0312', '2026-03-12', 'org_central_gov', 'svc_faq',     81,  97, 77.30, 16.10, 6.60,  942, '2026-03-13 00:00:00'),
('m_b_0313', '2026-03-13', 'org_central_gov', 'svc_faq',     80,  96, 77.80, 15.80, 6.40,  940, '2026-03-14 00:00:00'),
('m_b_0315', '2026-03-15', 'org_central_gov', 'svc_faq',     83, 100, 78.60, 15.30, 6.10,  938, '2026-03-16 00:00:00'),
('m_b_0316', '2026-03-16', 'org_central_gov', 'svc_faq',     85, 102, 79.10, 14.90, 6.00,  935, '2026-03-17 00:00:00'),
('m_b_0317', '2026-03-17', 'org_central_gov', 'svc_faq',     87, 104, 79.50, 14.60, 5.90,  930, '2026-03-18 00:00:00'),
('m_b_0318', '2026-03-18', 'org_central_gov', 'svc_faq',     89, 107, 79.90, 14.20, 5.80,  925, '2026-03-18 23:00:00');
