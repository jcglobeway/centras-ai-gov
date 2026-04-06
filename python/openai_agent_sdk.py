from agents import FileSearchTool, HostedMCPTool, Agent, ModelSettings, TResponseInputItem, Runner, RunConfig, trace
from openai import AsyncOpenAI
from types import SimpleNamespace
from guardrails.runtime import load_config_bundle, instantiate_guardrails, run_guardrails
from pydantic import BaseModel

# Tool definitions
file_search = FileSearchTool(
    vector_store_ids=[
        "vs_69bd04a7db1081919e0df73b32db9705"
    ]
)
mcp = HostedMCPTool(tool_config={
    "type": "mcp",
    "server_label": "centras_email_mcp",
    "allowed_tools": [
        "notify_sales_team",
        "send_proposal"
    ],
    "headers": {
        "_x-_a_p_i-_key": "4H2kq4kS7O1APsxrPlHJBvFxM6oBybV_wrueUnbedbU"
    },
    "require_approval": "never",
    "server_url": "https://park-macbookpro.tailedf4dc.ts.net/mcp"
})
# Shared client for guardrails and file search
client = AsyncOpenAI()
ctx = SimpleNamespace(guardrail_llm=client)
# Guardrails definitions
input_safety_check_config = {
    "guardrails": [
        { "name": "Contains PII", "config": { "block": False, "detect_encoded_pii": True, "entities": ["CREDIT_CARD", "US_BANK_NUMBER", "US_PASSPORT", "US_SSN"] } },
        { "name": "Moderation", "config": { "categories": ["sexual/minors", "hate/threatening", "harassment/threatening", "self-harm/instructions", "violence/graphic", "illicit/violent"] } }
    ]
}
def guardrails_has_tripwire(results):
    return any((hasattr(r, "tripwire_triggered") and (r.tripwire_triggered is True)) for r in (results or []))

def get_guardrail_safe_text(results, fallback_text):
    for r in (results or []):
        info = (r.info if hasattr(r, "info") else None) or {}
        if isinstance(info, dict) and ("checked_text" in info):
            return info.get("checked_text") or fallback_text
    pii = next(((r.info if hasattr(r, "info") else {}) for r in (results or []) if isinstance((r.info if hasattr(r, "info") else None) or {}, dict) and ("anonymized_text" in ((r.info if hasattr(r, "info") else None) or {}))), None)
    if isinstance(pii, dict) and ("anonymized_text" in pii):
        return pii.get("anonymized_text") or fallback_text
    return fallback_text

async def scrub_conversation_history(history, config):
    try:
        guardrails = (config or {}).get("guardrails") or []
        pii = next((g for g in guardrails if (g or {}).get("name") == "Contains PII"), None)
        if not pii:
            return
        pii_only = {"guardrails": [pii]}
        for msg in (history or []):
            content = (msg or {}).get("content") or []
            for part in content:
                if isinstance(part, dict) and part.get("type") == "input_text" and isinstance(part.get("text"), str):
                    res = await run_guardrails(ctx, part["text"], "text/plain", instantiate_guardrails(load_config_bundle(pii_only)), suppress_tripwire=True, raise_guardrail_errors=True)
                    part["text"] = get_guardrail_safe_text(res, part["text"])
    except Exception:
        pass

async def scrub_workflow_input(workflow, input_key, config):
    try:
        guardrails = (config or {}).get("guardrails") or []
        pii = next((g for g in guardrails if (g or {}).get("name") == "Contains PII"), None)
        if not pii:
            return
        if not isinstance(workflow, dict):
            return
        value = workflow.get(input_key)
        if not isinstance(value, str):
            return
        pii_only = {"guardrails": [pii]}
        res = await run_guardrails(ctx, value, "text/plain", instantiate_guardrails(load_config_bundle(pii_only)), suppress_tripwire=True, raise_guardrail_errors=True)
        workflow[input_key] = get_guardrail_safe_text(res, value)
    except Exception:
        pass

async def run_and_apply_guardrails(input_text, config, history, workflow):
    results = await run_guardrails(ctx, input_text, "text/plain", instantiate_guardrails(load_config_bundle(config)), suppress_tripwire=True, raise_guardrail_errors=True)
    guardrails = (config or {}).get("guardrails") or []
    mask_pii = next((g for g in guardrails if (g or {}).get("name") == "Contains PII" and ((g or {}).get("config") or {}).get("block") is False), None) is not None
    if mask_pii:
        await scrub_conversation_history(history, config)
        await scrub_workflow_input(workflow, "input_as_text", config)
        await scrub_workflow_input(workflow, "input_text", config)
    has_tripwire = guardrails_has_tripwire(results)
    safe_text = get_guardrail_safe_text(results, input_text)
    fail_output = build_guardrail_fail_output(results or [])
    pass_output = {"safe_text": (get_guardrail_safe_text(results, input_text) or input_text)}
    return {"results": results, "has_tripwire": has_tripwire, "safe_text": safe_text, "fail_output": fail_output, "pass_output": pass_output}

def build_guardrail_fail_output(results):
    def _get(name: str):
        for r in (results or []):
            info = (r.info if hasattr(r, "info") else None) or {}
            gname = (info.get("guardrail_name") if isinstance(info, dict) else None) or (info.get("guardrailName") if isinstance(info, dict) else None)
            if gname == name:
                return r
        return None
    pii, mod, jb, hal, nsfw, url, custom, pid = map(_get, ["Contains PII", "Moderation", "Jailbreak", "Hallucination Detection", "NSFW Text", "URL Filter", "Custom Prompt Check", "Prompt Injection Detection"])
    def _tripwire(r):
        return bool(r.tripwire_triggered)
    def _info(r):
        return r.info
    jb_info, hal_info, nsfw_info, url_info, custom_info, pid_info, mod_info, pii_info = map(_info, [jb, hal, nsfw, url, custom, pid, mod, pii])
    detected_entities = pii_info.get("detected_entities") if isinstance(pii_info, dict) else {}
    pii_counts = []
    if isinstance(detected_entities, dict):
        for k, v in detected_entities.items():
            if isinstance(v, list):
                pii_counts.append(f"{k}:{len(v)}")
    flagged_categories = (mod_info.get("flagged_categories") if isinstance(mod_info, dict) else None) or []

    return {
        "pii": { "failed": (len(pii_counts) > 0) or _tripwire(pii), "detected_counts": pii_counts },
        "moderation": { "failed": _tripwire(mod) or (len(flagged_categories) > 0), "flagged_categories": flagged_categories },
        "jailbreak": { "failed": _tripwire(jb) },
        "hallucination": { "failed": _tripwire(hal), "reasoning": (hal_info.get("reasoning") if isinstance(hal_info, dict) else None), "hallucination_type": (hal_info.get("hallucination_type") if isinstance(hal_info, dict) else None), "hallucinated_statements": (hal_info.get("hallucinated_statements") if isinstance(hal_info, dict) else None), "verified_statements": (hal_info.get("verified_statements") if isinstance(hal_info, dict) else None) },
        "nsfw": { "failed": _tripwire(nsfw) },
        "url_filter": { "failed": _tripwire(url) },
        "custom_prompt_check": { "failed": _tripwire(custom) },
        "prompt_injection": { "failed": _tripwire(pid) },
    }
class ClassifierAgentSchema(BaseModel):
    interest_area: str


class SmalltalkAgentSchema(BaseModel):
    title: str
    message: str
    buttonLabel: str


product_agent = Agent(
    name="product_agent",
    instructions="""# 역할
Centras.ai 제품/서비스 전문 안내 에이전트입니다.
방문자의 질문에 맞는 제품 정보를 search_product_info 함수로 조회하여 안내합니다.

# 규칙
- 답변 전 반드시 search_product_info를 먼저 호출하여 정확한 정보를 확인한다
- 핵심 성과 수치(재방문율 74.9%, 응답률 100% 등)를 적극 활용한다
- 가격 질문: \"기관별 맞춤 견적이라 제안서에서 확인하실 수 있습니다 📋\"
- 안내 후 \"도입 사례나 기술 문의가 더 필요하시면 말씀해 주세요!\" 로 마무리
- 존댓말 유지, 이모지 적절히 사용""",
    model="gpt-4o-mini",
    tools=[
        file_search
    ],
    model_settings=ModelSettings(
        temperature=1,
        top_p=1,
        max_tokens=2048,
        store=True
    )
)


usecase_agent = Agent(
    name="usecase_agent",
    instructions="""# 역할
Centras.ai 도입 상담 및 활용 사례 전문 에이전트입니다.
공공기관 도입 사례, 기대 효과, 도입 프로세스를 search_use_cases 함수로 조회하여 안내합니다.

# 규칙
- 답변 전 반드시 search_use_cases를 먼저 호출하여 정확한 정보를 확인한다
- 방문자의 기관 유형(시청/도청/교육청 등) 파악 후 가장 유사한 사례를 우선 제시한다
- 도입 효과 수치(처리 시간 60% 단축, 응답률 100% 등)를 구체적으로 언급한다
- 전국 48개 기관 도입 현황을 적극 활용한다
- 안내 후 \"실제 데모나 맞춤 제안서를 원하시면 말씀해 주세요! 🎯\" 로 마무리
- 존댓말 유지, 이모지 적절히 사용

# 중요 규칙
- search_use_cases 함수 호출 결과에 없는 기관은 절대 언급하지 않는다
- \"서울시청 도입 사례가 있나요?\" 처럼 Vector Store에 없는 기관을 물어볼 경우:
  \"현재 해당 기관의 도입 사례는 확인되지 않습니다.
   실제 도입 기관 사례가 궁금하시면 알려드릴게요! 😊\"
  라고 답변한다
- search_use_cases 결과에 명시된 기관명만 언급한다
- 추측하거나 유사 사례를 마치 해당 기관 사례인 것처럼 답변하지 않는다
- 확인되지 않은 정보는 반드시 \"확인되지 않습니다\"로 답변한다""",
    model="gpt-4o-mini",
    tools=[
        file_search
    ],
    model_settings=ModelSettings(
        temperature=1,
        top_p=1,
        max_tokens=2048,
        store=True
    )
)


proposal_agent = Agent(
    name="proposal_agent",
    instructions="""# 역할
  Centras.ai 제안서 요청 전담 에이전트입니다.
  리드 정보 수집 → 자격 평가 → 제안서 발송 → 영업팀 알림까지
  전체 Proposal 파이프라인을 단독으로 완결합니다.

  # 실행 순서 (반드시 이 순서대로)

  ## 1단계: 기본 정보 수집 (1회 질문으로 처리)
  아래 내용을 한 번에 요청:
  \"제안서 발송을 위해 아래 정보를 알려주세요 😊
  - 성함 / 기관명
  - 이메일 주소
  - 전화번호 (선택)\"

  ## 2단계: 도입 현황 파악 (1회 질문으로 처리)
  \"감사합니다! 마지막으로 한 가지만 여쭤볼게요.
  - 예산 규모: 상 / 중 / 하
  - 도입 시기: 즉시 / 3개월 내 / 6개월 내 / 검토중\"

  ## 3단계: 등급 판단
  수집한 정보를 바탕으로 내부적으로 등급 결정:
  - 예산 상 + 즉시/3개월: A등급
  - 예산 중 또는 6개월: B등급
  - 나머지: C등급

  ## 4단계: 제안서 발송
  send_proposal 함수 호출.
  \"맞춤 제안서를 {lead_email}로 발송했습니다! ✅\"

  ## 5단계: 영업팀 알림
  notify_sales_team 함수 호출.
  \"담당자가 2영업일 내에 연락드릴 예정입니다. 추가 문의는 02-1899-2842로 주세요.\"

  # 가드레일
  - 이메일 형식 오류 시 재입력 요청
  - 존댓말 유지, 이모지 적절히 사용""",
    model="gpt-4o-mini",
    tools=[
        file_search,
        mcp
    ],
    model_settings=ModelSettings(
        temperature=1,
        top_p=1,
        max_tokens=2048,
        store=True
    )
)


tech_agent = Agent(
    name="tech_agent",
    instructions="""# 역할
Centras.ai 기술/기능 전문 안내 에이전트입니다.
API 연동, 보안 인증, 기술 스펙 관련 질문에 search_tech_details 함수로 조회하여 답변합니다.

# 규칙
- 답변 전 반드시 search_tech_details를 먼저 호출하여 정확한 정보를 확인한다
- 기술 용어는 공공기관 IT 담당자 눈높이에 맞게 풀어서 설명한다
- SAO 엔진, GPT 기반 다국어, 실시간 분석 등 핵심 기술 강점을 부각한다
- 보안/인증 질문은 반드시 search_tech_details 호출 후 답변한다
- 안내 후 \"자세한 기술 검토가 필요하시면 담당 엔지니어 미팅을 제안드릴게요 🛠️\" 로 마무리
- 존댓말 유지, 이모지 적절히 사용""",
    model="gpt-4o-mini",
    tools=[
        file_search
    ],
    model_settings=ModelSettings(
        temperature=1,
        top_p=1,
        max_tokens=2048,
        store=True
    )
)


classifier_agent = Agent(
    name="classifier_agent",
    instructions="""Centras.ai의 영업 챗봇으로 방문자를 환영하고, 사용자의 입력을 제시된 선택지에 따라 정확히 분류해 classify_interest 함수를 호출하여 사용자의 관심사를 분류하세요. 모든 분류 논리는 아래 조건과 절차에 따라 명확하게 reasoning(입력 분석 및 매핑)을 진행한 후, 반드시 classify_interest 호출로 결론을 내고 state에 저장합니다.

## 절차 및 규칙

1. **초기 환영 및 안내 출력**  
   다음 메시지를 출력하세요:
   ```
   안녕하세요! Centras AI 어시스턴트입니다. 😊  
   공공 민원 자동화 솔루션에 관심 가져주셔서 감사합니다.

   어떤 내용을 도와드릴까요?
   1️⃣ 제품/서비스 소개
   2️⃣ 도입 상담 및 활용 사례
   3️⃣ 기술/기능 관련 문의
   4️⃣ 가격 및 제안서 요청
   ```

2. **사용자 입력 대기 및 분류 reasoning**
   - 사용자가 입력한 내용 또는 선택한 번호를 받아 아래 기준에 따라 명확하게 분석하세요.
   - **매핑 기준**
     - '1', '제품', '소개' 등 제품/소개 관련:  
       → `product` (confidence: 1.0)
     - '2', '상담', '도입', '사례' 등 상담/사례 관련:  
       → `usecase` (confidence: 1.0)
     - '3', '기술', 'API', '기능' 등 기술 문의 관련:  
       → `tech` (confidence: 1.0)
     - '4', '가격', '견적', '제안서' 등 가격/제안서 관련:  
       → `proposal` (confidence: 1.0)
   - 기타 불명확하거나 다중/모호한 입력:  
     → confidence 0.7 미만으로 판단, 해당 경우 반드시 사용자에게 다시 명확히 입력해달라고 재질문합니다.  
       *재질문 메시지: \"입력이 명확하지 않습니다. 번호(1~4) 또는 원하는 항목을 다시 입력해 주세요.\"*

3. **classify_interest 함수 호출**
   - reasoning을 통해 최종적으로 판별한 category와 confidence 값을 사용하여 classify_interest 함수를 호출하세요.
     - 예시: `classify_interest(category='product', confidence=1.0)`
   - 함수가 반환한 category 값을 반드시 `state.interest_area` 변수에 저장합니다.
     - 예: `state.interest_area = category`

4. **즉시 종료**
   - 함수 호출 및 state에 저장 이후에는 절대로 추가 멘트나 응답, 설명 없이 조용히 종료합니다.

## 절대 지켜야 할 점
- 제품 설명, 사례 안내, 기술 정보 등의 직접적 안내, 설명, 추가 도출 금지
- reasoning(분류 근거, 매핑 과정) 과 결론(함수 호출)은 반드시 reasoning→classification/함수호출 순서로 나타내야 하며 결론이 reasoning 앞에 오지 않도록 주의

# Output Format

- 응답 내용(대화 메시지 및 재질문 제외)은 모두 한국어로 작성
- 함수호출, reasoning, 변수 저장 과정 등은 내부 프로세스로, 출력에 포함하지 않습니다(사용자에게 노출 X)
- 오직 안내 메시지(또는 재질문), classify_interest 함수 호출, 변수 저장, 그리고 침묵 종료만 실행
- 긴 입력, 다중 의도 등 edge-case에서는 reasoning을 통해 가장 유력한 카테고리 판단 후, confidence<0.7일 경우 반드시 재질문
- 예외처리 등은 내부적으로 reasoning 후 결론에서만 처리

# Example

입력: \"가격이랑 도입 둘 다 궁금해요\"

- reasoning: 입력에 '가격'과 '도입' 모두 포함되어 있고, 두 카테고리 모두 연관성이 높으나 명확히 하나로 분류할 수 없어 confidence를 0.7 미만으로 판단합니다.  
- 행동: 사용자에게 \"입력이 명확하지 않습니다. 번호(1~4) 또는 원하는 항목을 다시 입력해 주세요.\"를 출력하고, 재입력 유도.

입력: \"서비스 소개 해주세요\"

- reasoning: '서비스 소개'는 제품/소개 카테고리에 해당하므로, 카테고리를 'product', confidence를 1.0으로 분류합니다.
- classify_interest(category='product', confidence=1.0) 호출
- state.interest_area = category
- 더 이상 출력 없이 종료

입력: \"3\"

- reasoning: 숫자 '3'은 기술 문의에 해당하므로 카테고리를 'tech', confidence를 1.0으로 분류합니다.
- classify_interest(category='tech', confidence=1.0) 호출
- state.interest_area = category
- 함수가 반환한 category 값을 interest_area 필드에 담아 JSON으로 반환한다 - 예시: {\"interest_area\": \"product\"} - state.interest_area 저장은 JSON 출력으로 대체한다

(※ 실제 예시는 더 복잡하거나 긴 입력도 충분히 reasoning 및 mapping을 통해 처리해야 함)

---

**중요:**  
- 모든 reasoning(분류 논리, 매핑 근거) → classify_interest 함수 호출 및 result/state 저장 → 즉시 종료 의 순서를 반드시 지키세요.
- 사용자의 입력이 불명확/모호하면 반드시 confidence<0.7로 판단하고 재질문을 출력하여 명확성을 확보하세요.
- 분류/매핑 reasoning이 최종 결론보다 앞에 오도록 순서에 유의하세요.""",
    model="gpt-4o-mini",
    output_type=ClassifierAgentSchema,
    model_settings=ModelSettings(
        temperature=1,
        top_p=1,
        max_tokens=2048,
        store=True
    )
)


smalltalk_agent = Agent(
    name="Smalltalk Agent",
    instructions="Try to let the user know that you're scopped to answer product, usercase, tech and proposal categories only. Always output message card at last.",
    model="gpt-4o-mini",
    output_type=SmalltalkAgentSchema,
    model_settings=ModelSettings(
        temperature=1,
        top_p=1,
        max_tokens=2048,
        store=True
    )
)


class WorkflowInput(BaseModel):
    input_as_text: str


# Main code entrypoint
async def run_workflow(workflow_input: WorkflowInput):
    with trace("Centras AI Sales Assistant"):
        state = {
            "interest_area": None,
            "lead_name": None,
            "company_name": None,
            "lead_email": None,
            "lead_phone": None,
            "budget": None,
            "decision_maker": None,
            "timeline": None,
            "lead_score": None,
            "lead_grade": None
        }
        workflow = workflow_input.model_dump()
        conversation_history: list[TResponseInputItem] = [
            {
                "role": "user",
                "content": [
                    {
                        "type": "input_text",
                        "text": workflow["input_as_text"]
                    }
                ]
            }
        ]
        try:
            guardrails_input_text = workflow["input_as_text"]
            guardrails_result = await run_and_apply_guardrails(guardrails_input_text, input_safety_check_config, conversation_history, workflow)
            guardrails_hastripwire = guardrails_result["has_tripwire"]
            guardrails_anonymizedtext = guardrails_result["safe_text"]
            guardrails_output = (guardrails_hastripwire and guardrails_result["fail_output"]) or guardrails_result["pass_output"]
            if guardrails_hastripwire:
                end_result = {
                    "failed": False,
                    "reason": ""
                }
                return end_result
            else:
                classifier_agent_result_temp = await Runner.run(
                    classifier_agent,
                    input=[
                        *conversation_history,
                        {
                            "role": "user",
                            "content": [
                                {
                                    "type": "input_text",
                                    "text": f" {workflow["input_as_text"]}"
                                }
                            ]
                        }
                    ],
                    run_config=RunConfig(trace_metadata={
                        "__trace_source__": "agent-builder",
                        "workflow_id": "wf_69b8c13a234881909abd2d50b10f67eb0ffa529b07a529aa"
                    })
                )

                conversation_history.extend([item.to_input_item() for item in classifier_agent_result_temp.new_items])

                classifier_agent_result = {
                    "output_text": classifier_agent_result_temp.final_output.json(),
                    "output_parsed": classifier_agent_result_temp.final_output.model_dump()
                }
                if classifier_agent_result["output_parsed"]["interest_area"] == "product":
                    product_agent_result_temp = await Runner.run(
                        product_agent,
                        input=[
                            *conversation_history
                        ],
                        run_config=RunConfig(trace_metadata={
                            "__trace_source__": "agent-builder",
                            "workflow_id": "wf_69b8c13a234881909abd2d50b10f67eb0ffa529b07a529aa"
                        })
                    )

                    conversation_history.extend([item.to_input_item() for item in product_agent_result_temp.new_items])

                    product_agent_result = {
                        "output_text": product_agent_result_temp.final_output_as(str)
                    }
                    end_result = {
                        "is_consulting_completed": False,
                        "interest_category": "",
                        "is_proposal_sent": False,
                        "followup_message": product_agent_result["output_text"]
                    }
                    return end_result
                elif classifier_agent_result["output_parsed"]["interest_area"] == "usecase":
                    usecase_agent_result_temp = await Runner.run(
                        usecase_agent,
                        input=[
                            *conversation_history
                        ],
                        run_config=RunConfig(trace_metadata={
                            "__trace_source__": "agent-builder",
                            "workflow_id": "wf_69b8c13a234881909abd2d50b10f67eb0ffa529b07a529aa"
                        })
                    )

                    conversation_history.extend([item.to_input_item() for item in usecase_agent_result_temp.new_items])

                    usecase_agent_result = {
                        "output_text": usecase_agent_result_temp.final_output_as(str)
                    }
                    end_result = {
                        "is_consulting_completed": False,
                        "interest_category": "",
                        "is_proposal_sent": False,
                        "followup_message": usecase_agent_result["output_text"]
                    }
                    return end_result
                elif classifier_agent_result["output_parsed"]["interest_area"] == "tech":
                    tech_agent_result_temp = await Runner.run(
                        tech_agent,
                        input=[
                            *conversation_history
                        ],
                        run_config=RunConfig(trace_metadata={
                            "__trace_source__": "agent-builder",
                            "workflow_id": "wf_69b8c13a234881909abd2d50b10f67eb0ffa529b07a529aa"
                        })
                    )

                    conversation_history.extend([item.to_input_item() for item in tech_agent_result_temp.new_items])

                    tech_agent_result = {
                        "output_text": tech_agent_result_temp.final_output_as(str)
                    }
                    end_result = {
                        "is_consulting_completed": False,
                        "interest_category": "",
                        "is_proposal_sent": False,
                        "followup_message": tech_agent_result["output_text"]
                    }
                    return end_result
                elif classifier_agent_result["output_parsed"]["interest_area"] == "smalltalk":
                    smalltalk_agent_result_temp = await Runner.run(
                        smalltalk_agent,
                        input=[
                            *conversation_history
                        ],
                        run_config=RunConfig(trace_metadata={
                            "__trace_source__": "agent-builder",
                            "workflow_id": "wf_69b8c13a234881909abd2d50b10f67eb0ffa529b07a529aa"
                        })
                    )

                    conversation_history.extend([item.to_input_item() for item in smalltalk_agent_result_temp.new_items])

                    smalltalk_agent_result = {
                        "output_text": smalltalk_agent_result_temp.final_output.json(),
                        "output_parsed": smalltalk_agent_result_temp.final_output.model_dump()
                    }
                    end_result = {
                        "is_consulting_completed": False,
                        "interest_category": "",
                        "is_proposal_sent": False,
                        "followup_message": smalltalk_agent_result["output_text"]
                    }
                    return end_result
                else:
                    proposal_agent_result_temp = await Runner.run(
                        proposal_agent,
                        input=[
                            *conversation_history
                        ],
                        run_config=RunConfig(trace_metadata={
                            "__trace_source__": "agent-builder",
                            "workflow_id": "wf_69b8c13a234881909abd2d50b10f67eb0ffa529b07a529aa"
                        })
                    )

                    conversation_history.extend([item.to_input_item() for item in proposal_agent_result_temp.new_items])

                    proposal_agent_result = {
                        "output_text": proposal_agent_result_temp.final_output_as(str)
                    }
                    end_result = {
                        "is_consulting_completed": False,
                        "interest_category": "",
                        "is_proposal_sent": False,
                        "followup_message": proposal_agent_result["output_text"]
                    }
                    return end_result
        except Exception as guardrails_error:
            guardrails_errorresult = {
                "message": getattr(guardrails_error, "message", "Unknown error"),
            }
            end_result = {
                "failed": False,
                "reason": ""
            }
            return end_result
