"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Sparkles,
  Rocket,
  LayoutDashboard,
  MessageSquare,
  Calendar,
  Users,
  Target,
  CreditCard,
  Bot,
  Megaphone,
  Workflow,
  Globe,
  BadgeCheck,
  FolderOpen,
  Star,
  Settings,
  Search,
  ChevronsUpDown,
  Sun,
  Moon,
} from "lucide-react";
import { useEffect, useState } from "react";

type Item = {
  label: string;
  href?: string;
  icon: React.ComponentType<{ size?: number; className?: string }>;
  badge?: string;
};

const mainItems: Item[] = [
  { label: "Ask AI", icon: Sparkles },
  { label: "Launchpad", icon: Rocket },
  { label: "Dashboard", href: "/", icon: LayoutDashboard },
  { label: "Conversations", icon: MessageSquare },
  { label: "Calendars", icon: Calendar },
  { label: "Contacts", href: "/contacts", icon: Users },
  { label: "Opportunities", href: "/opportunities", icon: Target },
  { label: "Payments", icon: CreditCard },
];

const secondaryItems: Item[] = [
  { label: "AI Studio", icon: Bot, badge: "Beta" },
  { label: "AI Agents", icon: Bot },
  { label: "Marketing", icon: Megaphone },
  { label: "Automation", icon: Workflow },
  { label: "Sites", icon: Globe },
  { label: "Memberships", icon: BadgeCheck },
  { label: "Media Storage", icon: FolderOpen },
  { label: "Reputation", icon: Star },
];

function NavRow({ item, active }: { item: Item; active: boolean }) {
  const Icon = item.icon;
  const cls = `flex items-center gap-2.5 rounded-md px-3 py-[7px] text-[13px] transition-colors ${
    active
      ? "bg-[var(--sidebar-active)] text-[var(--sidebar-active-text)] font-medium"
      : "text-[var(--sidebar-text)] hover:bg-[var(--sidebar-hover)] hover:text-[var(--sidebar-active-text)]"
  }`;
  const body = (
    <>
      <Icon size={15} className="shrink-0" />
      <span className="truncate">{item.label}</span>
      {item.badge && (
        <span className="ml-auto rounded bg-amber-400 px-1.5 py-px text-[10px] font-semibold text-slate-900">
          {item.badge}
        </span>
      )}
    </>
  );
  if (item.href) {
    return (
      <Link href={item.href} className={cls}>
        {body}
      </Link>
    );
  }
  return (
    <button type="button" className={`${cls} w-full cursor-default opacity-80`}>
      {body}
    </button>
  );
}

export default function Sidebar() {
  const pathname = usePathname();
  const [isDarkTheme, setIsDarkTheme] = useState(false);

  useEffect(() => {
    const savedTheme = window.localStorage.getItem("doxa-theme");
    const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    setIsDarkTheme(savedTheme ? savedTheme === "dark" : prefersDark);
  }, []);

  useEffect(() => {
    const theme = isDarkTheme ? "dark" : "light";
    document.documentElement.setAttribute("data-theme", theme);
    window.localStorage.setItem("doxa-theme", theme);
  }, [isDarkTheme]);

  return (
    <aside className="flex h-screen w-[230px] shrink-0 flex-col border-r border-[var(--grid)] bg-[var(--sidebar-bg)]">
      <div className="flex shrink-0 items-center justify-center border-b border-[var(--grid)] px-4 py-4">
        <div className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded bg-[var(--brand)] text-sm font-bold text-white">
            S
          </div>
          <span className="text-sm font-semibold text-[var(--sidebar-text)]">Spreed CRM</span>
        </div>
      </div>

      <button
        type="button"
        className="mx-3 mt-3 flex shrink-0 items-center justify-between rounded-md bg-[var(--sidebar-hover)] px-3 py-2 text-left"
      >
        <div className="min-w-0">
          <div className="truncate text-[13px] font-medium text-[var(--sidebar-text)]">Spreed AI</div>
          <div className="truncate text-[11px] text-[var(--sidebar-text-muted)]">Orlando, FL</div>
        </div>
        <ChevronsUpDown size={14} className="text-[var(--sidebar-text-muted)]" />
      </button>

      <div className="mx-3 mt-3 flex shrink-0 items-center gap-2 rounded-md bg-[var(--sidebar-hover)] px-3 py-1.5">
        <Search size={13} className="text-[var(--sidebar-text-muted)]" />
        <span className="text-[12px] text-[var(--sidebar-text-muted)]">Search</span>
        <span className="ml-auto rounded border border-[var(--grid)] px-1 text-[10px] text-[var(--sidebar-text-muted)]">
          ⌘K
        </span>
      </div>

      <nav className="mt-3 min-h-0 flex-1 space-y-px overflow-y-auto px-3 pb-3">
        {mainItems.map((item) => (
          <NavRow key={item.label} item={item} active={item.href === pathname} />
        ))}
        <div className="!my-3 border-t border-[var(--grid)]" />
        {secondaryItems.map((item) => (
          <NavRow key={item.label} item={item} active={false} />
        ))}
      </nav>

      <div className="flex shrink-0 items-center gap-1 border-t border-[var(--grid)] px-3 py-3">
        <button
          type="button"
          className="flex min-w-0 flex-1 items-center gap-2.5 rounded-md px-3 py-[7px] text-[13px] text-[var(--sidebar-text)] opacity-80 transition-colors hover:bg-[var(--sidebar-hover)]"
        >
          <Settings size={15} className="shrink-0" />
          <span className="truncate">Settings</span>
        </button>

        <button
          type="button"
          role="switch"
          aria-checked={isDarkTheme}
          aria-label={isDarkTheme ? "Ativar modo claro" : "Ativar modo escuro"}
          title={isDarkTheme ? "Modo claro" : "Modo escuro"}
          onClick={() => setIsDarkTheme((current) => !current)}
          className={`relative flex h-7 w-12 shrink-0 items-center rounded-full border transition-colors ${
            isDarkTheme
              ? "border-slate-600 bg-slate-700"
              : "border-[var(--grid)] bg-slate-200"
          }`}
        >
          <span
            className={`absolute flex h-5 w-5 items-center justify-center rounded-full bg-white shadow-sm transition-transform ${
              isDarkTheme ? "translate-x-[24px] text-slate-700" : "translate-x-[3px] text-amber-500"
            }`}
          >
            {isDarkTheme ? <Moon size={11} /> : <Sun size={11} />}
          </span>
        </button>
      </div>
    </aside>
  );
}
