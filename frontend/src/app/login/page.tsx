"use client";

import { useState } from "react";
import { useAuth } from "@/lib/auth";

export default function LoginPage() {
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
    } catch {
      setError("이메일 또는 비밀번호가 올바르지 않습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-bg-base">
      <div className="w-full max-w-sm">
        {/* logo / title */}
        <div className="mb-8 text-center">
          <div className="inline-flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded bg-accent flex items-center justify-center">
              <span className="text-white font-bold text-sm">C</span>
            </div>
            <span className="text-text-primary font-semibold text-lg">
              Centras AI Gov
            </span>
          </div>
          <p className="text-text-muted text-sm">어드민 포털</p>
        </div>

        {/* card */}
        <div className="rounded-lg p-6" style={{ background: "var(--card-bg)", border: "1px solid var(--card-border)" }}>
          <h1 className="text-text-primary font-semibold text-base mb-6">
            로그인
          </h1>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-text-secondary text-xs mb-1.5">
                이메일
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                placeholder="admin@example.com"
                className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2.5
                           text-text-primary text-sm placeholder:text-text-muted
                           focus:outline-none focus:border-accent transition-colors rounded-md"
              />
            </div>

            <div>
              <label className="block text-text-secondary text-xs mb-1.5">
                비밀번호
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
                placeholder="••••••••"
                className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2.5
                           text-text-primary text-sm placeholder:text-text-muted
                           focus:outline-none focus:border-accent transition-colors rounded-md"
              />
            </div>

            {error && (
              <p className="text-error text-xs">{error}</p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-accent hover:bg-accent-hover disabled:opacity-50
                         text-white font-inter font-[510] text-sm rounded-md py-2.5
                         transition-colors"
            >
              {loading ? "로그인 중..." : "로그인"}
            </button>
          </form>
        </div>

        <p className="text-center text-text-muted text-xs mt-4">
          Centras AI Gov © 2026
        </p>
      </div>
    </div>
  );
}
