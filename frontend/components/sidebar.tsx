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
} from "lucide-react";

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
      ? "bg-[var(--sidebar-active)] text-white font-medium"
      : "text-slate-300 hover:bg-[var(--sidebar-hover)] hover:text-white"
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
  return (
    <aside className="flex h-screen w-[230px] shrink-0 flex-col bg-[var(--sidebar-bg)]">
      <div className="flex items-center justify-center border-b border-white/10 px-4 py-4">
        <div className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded bg-[var(--brand)] text-sm font-bold text-white">
            S
          </div>
          <span className="text-sm font-semibold text-white">Spreed CRM</span>
        </div>
      </div>

      <button
        type="button"
        className="mx-3 mt-3 flex items-center justify-between rounded-md bg-[var(--sidebar-hover)] px-3 py-2 text-left"
      >
        <div className="min-w-0">
          <div className="truncate text-[13px] font-medium text-white">Spreed AI</div>
          <div className="truncate text-[11px] text-slate-400">Orlando, FL</div>
        </div>
        <ChevronsUpDown size={14} className="text-slate-400" />
      </button>

      <div className="mx-3 mt-3 flex items-center gap-2 rounded-md bg-[var(--sidebar-hover)] px-3 py-1.5">
        <Search size={13} className="text-slate-400" />
        <span className="text-[12px] text-slate-400">Search</span>
        <span className="ml-auto rounded border border-slate-600 px-1 text-[10px] text-slate-400">
          ⌘K
        </span>
      </div>

      <nav className="mt-3 flex-1 space-y-px overflow-y-auto px-3 pb-3">
        {mainItems.map((item) => (
          <NavRow key={item.label} item={item} active={item.href === pathname} />
        ))}
        <div className="!my-3 border-t border-white/10" />
        {secondaryItems.map((item) => (
          <NavRow key={item.label} item={item} active={false} />
        ))}
      </nav>

      <div className="border-t border-white/10 px-3 py-3">
        <NavRow item={{ label: "Settings", icon: Settings }} active={false} />
      </div>
    </aside>
  );
}
