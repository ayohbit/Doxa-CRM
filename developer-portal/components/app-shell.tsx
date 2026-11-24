"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import Sidebar from "@/components/sidebar";
import { clearToken, isAuthenticated } from "@/lib/auth";
import { getCurrentUser, type CurrentUser } from "@/lib/api";

export default function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(pathname !== "/login");

  useEffect(() => {
    if (pathname === "/login") {
      setLoading(false);
      return;
    }
    if (!isAuthenticated()) {
      router.replace("/login");
      return;
    }
    getCurrentUser()
      .then(setUser)
      .catch(() => {
        clearToken();
        router.replace("/login");
      })
      .finally(() => setLoading(false));
  }, [pathname, router]);

  if (pathname === "/login") return <>{children}</>;
  if (loading || !user) {
    return (
      <div className="flex h-full items-center justify-center text-[13px] text-[var(--text-muted)]">
        Verificando acesso...
      </div>
    );
  }

  return (
    <div className="flex h-full">
      <Sidebar user={user} />
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-12 shrink-0 items-center justify-end border-b border-[var(--grid)] bg-white px-5">
          <div className="flex items-center gap-2 text-[12px] text-[var(--text-secondary)]">
            <span className="h-2 w-2 rounded-full bg-emerald-500" />
            {user.email}
          </div>
        </header>
        <main className="min-h-0 flex-1 overflow-auto">{children}</main>
      </div>
    </div>
  );
}
