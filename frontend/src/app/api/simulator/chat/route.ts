const RAG_URL = process.env.RAG_ORCHESTRATOR_URL ?? "http://localhost:8090";

export async function POST(req: Request) {
  const body = await req.json();
  const messages: { role: string; content: string }[] = body.messages ?? [];
  const questionText = messages.at(-1)?.content ?? "";
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
      }),
    });
  } catch {
    return new Response("RAG 오케스트레이터에 연결할 수 없습니다.", {
      headers: { "Content-Type": "text/plain; charset=utf-8" },
    });
  }

  // NDJSON 스트림에서 content 토큰만 추출해 plain text stream으로 변환
  const textStream = ragRes.body!.pipeThrough(
    new TransformStream<Uint8Array, Uint8Array>({
      transform(chunk, controller) {
        const text = new TextDecoder().decode(chunk);
        for (const line of text.split("\n").filter(Boolean)) {
          try {
            const parsed = JSON.parse(line);
            if (typeof parsed.content === "string") {
              controller.enqueue(new TextEncoder().encode(parsed.content));
            }
          } catch {
            // non-JSON line 무시
          }
        }
      },
    })
  );

  return new Response(textStream, {
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "X-Vercel-AI-Data-Stream": "v1",
    },
  });
}
