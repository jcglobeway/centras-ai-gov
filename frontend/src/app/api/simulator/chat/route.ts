export const runtime = "edge";

const RAG_URL = process.env.RAG_ORCHESTRATOR_URL ?? "http://localhost:8090";

export async function POST(req: Request) {
  const body = await req.json();
  const messages: { role: string; content: string }[] = body.messages ?? [];
  const questionText = messages.at(-1)?.content ?? "";
  const conversationHistory = messages.slice(0, -1); // 마지막 질문 제외한 이전 대화
  const organizationId: string = body.organizationId ?? "";
  const serviceId: string = body.serviceId ?? "";

  let ragRes: Response;
  try {
    ragRes = await fetch(`${RAG_URL}/generate/stream`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        question_id: `sim_q_${Date.now()}`,
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

  // NDJSON 스트림을 그대로 전달 — 프론트엔드가 content/done 패킷을 직접 파싱
  return new Response(ragRes.body, {
    headers: { "Content-Type": "application/x-ndjson; charset=utf-8" },
  });
}
