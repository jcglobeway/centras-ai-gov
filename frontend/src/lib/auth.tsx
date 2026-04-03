"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from "react";
import { useRouter } from "next/navigation";
import { SWRConfig } from "swr";
import type { SessionInfo, RoleCode } from "./types";
import { authApi } from "./api";

interface AuthContextValue {
  session: SessionInfo | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// 역할별 기본 랜딩 경로
const ROLE_DEFAULT_PATH: Record<RoleCode, string> = {
  super_admin: "/ops",
  ops_admin: "/ops",
  client_admin: "/client",
  client_org_admin: "/client",
  client_viewer: "/client",
  qa_admin: "/qa",
  qa_manager: "/qa",
  knowledge_editor: "/qa",
};

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [session, setSession] = useState<SessionInfo | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem("session");
    if (stored) {
      try {
        const parsed: SessionInfo = JSON.parse(stored);
        if (new Date(parsed.expiresAt) > new Date()) {
          setSession(parsed);
        } else {
          localStorage.removeItem("session");
          localStorage.removeItem("sessionId");
        }
      } catch {
        localStorage.removeItem("session");
      }
    }
    setLoading(false);
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await authApi.login({ email, password });

      // 백엔드 응답 → 내부 SessionInfo로 변환
      const sessionInfo: SessionInfo = {
        sessionId: res.session.token,
        userId: res.user.id,
        email: res.user.email,
        displayName: res.user.displayName,
        roleCode: res.authorization.primaryRole,
        organizationId: res.authorization.organizationScope[0] ?? null,
        organizationIds: res.authorization.organizationScope,
        expiresAt: res.session.expiresAt,
      };

      localStorage.setItem("sessionId", sessionInfo.sessionId);
      localStorage.setItem("session", JSON.stringify(sessionInfo));
      setSession(sessionInfo);
      router.push(ROLE_DEFAULT_PATH[sessionInfo.roleCode]);
    },
    [router]
  );

  const logout = useCallback(async () => {
    await authApi.logout().catch(() => {});
    localStorage.removeItem("sessionId");
    localStorage.removeItem("session");
    setSession(null);
    router.push("/login");
  }, [router]);

  return (
    <SWRConfig value={{ keepPreviousData: true, revalidateOnFocus: false }}>
      <AuthContext.Provider value={{ session, loading, login, logout }}>
        {children}
      </AuthContext.Provider>
    </SWRConfig>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

// 포털별 허용 역할
export const PORTAL_ROLES: Record<string, RoleCode[]> = {
  ops: ["super_admin", "ops_admin"],
  client: ["super_admin", "client_admin", "client_org_admin", "client_viewer"],
  qa: ["super_admin", "qa_admin", "qa_manager", "knowledge_editor"],
};
