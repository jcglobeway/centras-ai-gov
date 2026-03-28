"use client";

import { useEffect, useRef, useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import { Button } from "@/components/ui/Button";

interface OrgItem {
  id: string;
  name: string;
}
interface ServiceItem {
  id: string;
  name: string;
}
interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
}

function getSessionId(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("sessionId");
}

export default function SimulatorPage() {
  const [orgId, setOrgId] = useState("");
  const [serviceId, setServiceId] = useState("");
  const [chatSessionId, setChatSessionId] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  const { data: orgs } = useSWR<{ items: OrgItem[] }>(
    "/api/admin/organizations?page_size=50",
    fetcher
  );
  const { data: services } = useSWR<{ items: ServiceItem[] }>(
    orgId ? `/api/admin/organizations/${orgId}/services` : null,
    fetcher
  );

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isLoading]);

  async function startSession() {
    setStarting(true);
    try {
      const res = await fetch("/api/admin/simulator/sessions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Admin-Session-Id": getSessionId() ?? "",
        },
        body: JSON.stringify({ organizationId: orgId, serviceId }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const { sessionId: id } = await res.json();
      setChatSessionId(id);
    } catch {
      alert("세션 생성에 실패했습니다. 백엔드를 확인하세요.");
    } finally {
      setStarting(false);
    }
  }

  function resetSession() {
    setChatSessionId(null);
    setOrgId("");
    setServiceId("");
    setMessages([]);
    setInput("");
  }

  async function sendMessage(e: React.FormEvent) {
    e.preventDefault();
    if (!input.trim() || isLoading) return;

    const userMsg: Message = {
      id: `user_${Date.now()}`,
      role: "user",
      content: input.trim(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setIsLoading(true);

    const assistantId = `asst_${Date.now()}`;
    setMessages((prev) => [
      ...prev,
      { id: assistantId, role: "assistant", content: "" },
    ]);

    try {
      const res = await fetch("/api/simulator/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          messages: [...messages, userMsg].map((m) => ({
            role: m.role,
            content: m.content,
          })),
          organizationId: orgId,
          serviceId,
          sessionId: chatSessionId,
        }),
      });

      if (!res.body) throw new Error("응답 본문이 없습니다.");

      const reader = res.body.getReader();
      const decoder = new TextDecoder();

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const text = decoder.decode(value, { stream: true });
        setMessages((prev) =>
          prev.map((m) =>
            m.id === assistantId ? { ...m, content: m.content + text } : m
          )
        );
      }
    } catch {
      setMessages((prev) =>
        prev.map((m) =>
          m.id === assistantId
            ? { ...m, content: "답변 생성 중 오류가 발생했습니다." }
            : m
        )
      );
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="flex flex-col h-full">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-text-primary font-semibold text-base">
            챗봇 시뮬레이터
          </h1>
          <p className="text-text-muted text-xs mt-0.5">
            RAG 파이프라인을 실시간으로 테스트합니다
          </p>
        </div>
        {chatSessionId && (
          <Button onClick={resetSession} variant="secondary">
            대화 초기화
          </Button>
        )}
      </div>

      {!chatSessionId ? (
        <div className="flex gap-3 items-end">
          <div className="flex flex-col gap-1">
            <label className="text-xs text-text-muted">기관</label>
            <select
              value={orgId}
              onChange={(e) => {
                setOrgId(e.target.value);
                setServiceId("");
              }}
              className="bg-bg-surface border border-bg-border rounded px-3 py-2 text-sm text-text-primary min-w-[160px]"
            >
              <option value="">선택하세요</option>
              {orgs?.items?.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.name}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-xs text-text-muted">서비스</label>
            <select
              value={serviceId}
              onChange={(e) => setServiceId(e.target.value)}
              disabled={!orgId}
              className="bg-bg-surface border border-bg-border rounded px-3 py-2 text-sm text-text-primary min-w-[160px] disabled:opacity-50"
            >
              <option value="">선택하세요</option>
              {services?.items?.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>
          <Button
            onClick={startSession}
            disabled={!orgId || !serviceId || starting}
          >
            {starting ? "시작 중..." : "대화 시작"}
          </Button>
        </div>
      ) : (
        <div className="flex-1 flex flex-col overflow-hidden bg-bg-surface border border-bg-border rounded-xl">
          <div className="px-4 py-2 border-b border-bg-border flex items-center gap-2">
            <span className="text-xs text-text-muted">세션</span>
            <code className="text-xs text-accent">{chatSessionId}</code>
          </div>

          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {messages.length === 0 && (
              <p className="text-center text-text-muted text-sm mt-8">
                질문을 입력해 RAG 파이프라인을 테스트하세요
              </p>
            )}
            {messages.map((m) => (
              <div
                key={m.id}
                className={`flex ${m.role === "user" ? "justify-end" : "justify-start"}`}
              >
                <div
                  className={`max-w-[75%] rounded-2xl px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap ${
                    m.role === "user"
                      ? "bg-accent text-white rounded-br-sm"
                      : "bg-bg-elevated border border-bg-border text-text-primary rounded-bl-sm"
                  }`}
                >
                  {m.content || (
                    <span className="text-text-muted text-xs">생성 중...</span>
                  )}
                </div>
              </div>
            ))}
            {isLoading &&
              messages.at(-1)?.role === "user" && (
                <div className="flex justify-start">
                  <div className="bg-bg-elevated border border-bg-border rounded-2xl rounded-bl-sm px-4 py-3">
                    <div className="flex gap-1 items-center">
                      <span className="w-1.5 h-1.5 bg-text-muted rounded-full animate-bounce [animation-delay:0ms]" />
                      <span className="w-1.5 h-1.5 bg-text-muted rounded-full animate-bounce [animation-delay:150ms]" />
                      <span className="w-1.5 h-1.5 bg-text-muted rounded-full animate-bounce [animation-delay:300ms]" />
                    </div>
                  </div>
                </div>
              )}
            <div ref={bottomRef} />
          </div>

          <form
            onSubmit={sendMessage}
            className="flex gap-2 p-3 border-t border-bg-border"
          >
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="질문을 입력하세요..."
              disabled={isLoading}
              className="flex-1 bg-bg-base border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent disabled:opacity-50"
            />
            <Button type="submit" disabled={isLoading || !input.trim()}>
              전송
            </Button>
          </form>
        </div>
      )}
    </div>
  );
}
