"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth, PORTAL_ROLES } from "@/lib/auth";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Spinner } from "@/components/ui/Spinner";

export default function OpsLayout({ children }: { children: React.ReactNode }) {
  const { session, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    if (!session || !PORTAL_ROLES.ops.includes(session.roleCode)) {
      router.replace("/login");
    }
  }, [session, loading, router]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-bg-base">
        <Spinner />
      </div>
    );
  }

  if (!session || !PORTAL_ROLES.ops.includes(session.roleCode)) {
    return null;
  }

  return (
    <div className="flex h-screen bg-bg-base">
      <Sidebar />
      <div className="flex-1 flex flex-col overflow-hidden">
        <Header title="운영사 어드민" />
        <main className="flex-1 overflow-y-auto p-6">{children}</main>
      </div>
    </div>
  );
}
