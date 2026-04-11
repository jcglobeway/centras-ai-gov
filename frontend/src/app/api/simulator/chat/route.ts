const RAG_URL = process.env.RAG_ORCHESTRATOR_URL ?? "http://localhost:8090";
const ADMIN_URL = process.env.ADMIN_API_URL ?? "http://localhost:8081";

export async function POST(req: Request) {
  const body = await req.json();
  const messages: { role: string; content: string }[] = body.messages ?? [];
  const questionText = messages.at(-1)?.content ?? "";
  const conversationHistory = messages.slice(0, -1);
  const organizationId: string = body.organizationId ?? "";
  const serviceId: string = body.serviceId ?? "";
  const sessionId: string = body.sessionId ?? "";
  const adminSessionId: string = body.adminSessionId ?? "";

  // 1. admin API에 question 생성 → 실제 questionId 확보
  let questionId: string = `sim_q_${Date.now()}`;
  try {
    const qRes = await fetch(`${ADMIN_URL}/admin/questions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Admin-Session-Id": adminSessionId,
      },
      body: JSON.stringify({
        organizationId,
        serviceId,
        chatSessionId: sessionId,
        questionText,
        questionIntentLabel: null,
        channel: "simulator",
      }),
    });
    if (qRes.ok) {
      const qData = await qRes.json();
      questionId = qData.questionId;
    }
  } catch {
    // question 생성 실패 시 임시 ID로 계속 진행
  }

  // 2. RAG 오케스트레이터 호출
  let ragRes: Response;
  try {
    ragRes = await fetch(`${RAG_URL}/generate/stream`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        question_id: questionId,
        question_text: questionText,
        organization_id: organizationId,
        service_id: serviceId,
        conversation_history: conversationHistory,
      }),
    });
  } catch {
    return new Response("RAG 오케스트레이터에 연결할 수 없습니다.", {
      headers: { "Content-Type": "text/plain; charset=utf-8" },
    });
  }

  if (!ragRes.body) {
    return new Response("RAG 응답이 없습니다.", {
      headers: { "Content-Type": "text/plain; charset=utf-8" },
    });
  }

  const capturedQuestionId = questionId;
  const capturedSessionId = adminSessionId;
  const transformed = new ReadableStream<Uint8Array>({
    async start(controller) {
      const reader = ragRes.body!.getReader();
      const decoder = new TextDecoder();
      const encoder = new TextEncoder();
      let buffer = "";
      let accText = "";

      try {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split("\n");
          buffer = lines.pop() ?? "";

          for (const line of lines) {
            if (!line.trim()) continue;
            try {
              const evt = JSON.parse(line);
              if (typeof evt.content === "string") {
                accText += evt.content;
              }
              if (evt.done) {
                evt.question_id = evt.question_id ?? capturedQuestionId;
                await fetch(`${ADMIN_URL}/admin/answers`, {
                  method: "POST",
                  headers: {
                    "Content-Type": "application/json",
                    "X-Admin-Session-Id": capturedSessionId,
                  },
                  body: JSON.stringify({
                    questionId: capturedQuestionId,
                    answerText: accText,
                    answerStatus: evt.answer_status ?? "answered",
                    responseTimeMs: evt.response_time_ms ?? null,
                    citationCount: evt.citation_count ?? 0,
                    fallbackReasonCode: null,
                  }),
                }).catch(() => {});
              }
              controller.enqueue(encoder.encode(JSON.stringify(evt) + "\n"));
            } catch {
              // JSON 파싱 불가 라인은 원문 전달
              controller.enqueue(encoder.encode(line + "\n"));
            }
          }
        }
      } catch {
        // 스트림 오류는 무시하고 종료
      } finally {
        controller.close();
        reader.releaseLock();
      }
    },
  });

  return new Response(transformed, {
    headers: { "Content-Type": "application/x-ndjson; charset=utf-8" },
  });
}
