"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import type { RoleCode } from "@/lib/types";

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

export default function RootPage() {
  const { session, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    if (!session) {
      router.replace("/login");
    } else {
      router.replace(ROLE_DEFAULT_PATH[session.roleCode] ?? "/ops");
    }
  }, [session, loading, router]);

  return null;
}
