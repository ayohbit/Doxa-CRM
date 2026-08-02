import { Phone, Sparkles, Megaphone, Bell, HelpCircle } from "lucide-react";

export default function Topbar() {
  return (
    <header className="flex h-12 shrink-0 items-center justify-end gap-2 border-b border-[var(--hairline)] bg-white px-4">
      <button className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-500 text-white hover:bg-emerald-600">
        <Phone size={14} />
      </button>
      <button className="flex items-center gap-1.5 rounded-full bg-[var(--brand)] px-3 py-1.5 text-[12px] font-medium text-white hover:opacity-90">
        <Sparkles size={13} />
        Ask AI
      </button>
      <button className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 hover:bg-emerald-200">
        <Megaphone size={14} />
      </button>
      <button className="relative flex h-8 w-8 items-center justify-center rounded-full bg-orange-100 text-orange-600 hover:bg-orange-200">
        <Bell size={14} />
        <span className="absolute -right-0.5 -top-0.5 h-2 w-2 rounded-full bg-red-500" />
      </button>
      <button className="flex h-8 w-8 items-center justify-center rounded-full bg-sky-100 text-sky-700 hover:bg-sky-200">
        <HelpCircle size={14} />
      </button>
      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-600 text-[12px] font-semibold text-white">
        GB
      </div>
    </header>
  );
}
