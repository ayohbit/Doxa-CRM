"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { BookOpen, KeyRound, LogOut, Network, SunMoon } from "lucide-react";
import { useEffect, useState } from "react";
import { clearToken } from "@/lib/auth";
import type { CurrentUser } from "@/lib/api";

const items = [
  { label: "API Docs", href: "/", icon: BookOpen },
  { label: "Tokens", href: "/tokens", icon: KeyRound, admin: true },
  { label: "MCP Server", href: "/mcp", icon: Network },
];

export default function Sidebar({ user }: { user: CurrentUser }) {
  const pathname = usePathname();
  const router = useRouter();
  const [dark, setDark] = useState(false);

  useEffect(() => {
    const stored = window.localStorage.getItem("doxa-theme");
    const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    setDark(stored ? stored === "dark" : prefersDark);
  }, []);

  useEffect(() => {
    const theme = dark ? "dark" : "light";
    document.documentElement.setAttribute("data-theme", theme);
    window.localStorage.setItem("doxa-theme", theme);
  }, [dark]);

  return (
    <aside className="flex h-screen w-[230px] shrink-0 flex-col border-r border-[var(--grid)] bg-[var(--sidebar-bg)]">
      <div className="flex h-[65px] items-center gap-2 border-b border-[var(--grid)] px-5">
        <div className="flex h-8 w-8 items-center justify-center rounded bg-[var(--brand)] text-sm font-bold text-white">
          D
        </div>
        <div>
          <div className="text-[13px] font-semibold text-[var(--sidebar-text)]">DOXA CRM</div>
          <div className="text-[10px] text-[var(--sidebar-text-muted)]">Developer Portal</div>
        </div>
      </div>
      <div className="mx-3 mt-4 rounded-md bg-[var(--sidebar-hover)] px-3 py-2">
        <p className="truncate text-[12px] font-medium text-[var(--sidebar-text)]">
          {user.companyName}
        </p>
        <p className="mt-0.5 text-[10px] text-[var(--sidebar-text-muted)]">Workspace</p>
      </div>
      <nav className="mt-4 flex-1 space-y-1 px-3">
        <p className="mb-2 px-3 text-[10px] font-semibold uppercase tracking-wide text-[var(--text-muted)]">
          Developer tools
        </p>
        {items
          .filter((item) => !item.admin || user.role === "ADMIN")
          .map(({ label, href, icon: Icon }) => {
            const active = pathname === href || (href !== "/" && pathname.startsWith(href));
            return (
              <Link
                key={href}
                href={href}
                className={`flex items-center gap-2.5 rounded-md px-3 py-2 text-[13px] transition-colors ${
                  active
                    ? "bg-[var(--sidebar-active)] font-medium text-[var(--sidebar-active-text)]"
                    : "text-[var(--sidebar-text)] hover:bg-[var(--sidebar-hover)]"
                }`}
              >
                <Icon size={15} />
                {label}
              </Link>
            );
          })}
      </nav>
      <div className="border-t border-[var(--grid)] p-3">
        <button
          type="button"
          onClick={() => setDark((value) => !value)}
          className="mb-1 flex w-full items-center gap-2.5 rounded-md px-3 py-2 text-[12px] text-[var(--sidebar-text)] hover:bg-[var(--sidebar-hover)]"
        >
          <SunMoon size={15} />
          {dark ? "Modo claro" : "Modo escuro"}
        </button>
        <button
          type="button"
          onClick={() => {
            clearToken();
            router.replace("/login");
          }}
          className="flex w-full items-center gap-2.5 rounded-md px-3 py-2 text-[12px] text-[var(--sidebar-text)] hover:bg-[var(--sidebar-hover)]"
        >
          <LogOut size={15} />
          Sair
        </button>
      </div>
    </aside>
  );
}
