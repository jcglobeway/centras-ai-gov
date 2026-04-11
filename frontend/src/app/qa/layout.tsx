"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth, PORTAL_ROLES } from "@/lib/auth";
import { FilterProvider } from "@/lib/filter-context";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Breadcrumb } from "@/components/layout/Breadcrumb";
import { Spinner } from "@/components/ui/Spinner";

export default function QaLayout({ children }: { children: React.ReactNode }) {
  const { session, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    if (!session || !PORTAL_ROLES.qa.includes(session.roleCode)) {
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

  if (!session || !PORTAL_ROLES.qa.includes(session.roleCode)) {
    return null;
  }

  return (
    <FilterProvider>
      <div className="flex h-screen bg-bg-surface">
        <Sidebar />
        <div className="flex-1 flex flex-col overflow-hidden">
          <Header />
          <Breadcrumb />
          <main className="flex-1 overflow-y-auto p-8">
            <div className="max-w-[1400px]">{children}</div>
          </main>
        </div>
      </div>
    </FilterProvider>
  );
}
