import {
  Phone,
  MessageSquare,
  Tag,
  FileText,
  CheckSquare,
  CalendarDays,
} from "lucide-react";
import type { Opportunity } from "@/lib/types";

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-1 text-[11.5px] leading-5">
      <span className="shrink-0 font-medium text-[var(--text-secondary)]">{label}:</span>
      <span className="truncate text-[var(--text-muted)]">{value}</span>
    </div>
  );
}

export default function OpportunityCard({ opp }: { opp: Opportunity }) {
  return (
    <article className="cursor-pointer rounded-md border border-[var(--grid)] bg-white p-3 shadow-sm transition-shadow hover:shadow-md">
      <h3 className="text-[13px] font-semibold text-[var(--text-primary)]">{opp.name}</h3>
      <div className="mt-1.5">
        {opp.adSet && <Row label="Ad Set" value={opp.adSet} />}
        {opp.revenueMonthly && <Row label="Revenue Monthly" value={opp.revenueMonthly} />}
        <Row label="Created on" value={opp.createdOn} />
      </div>
      <div className="mt-2 flex items-center gap-2.5 border-t border-[var(--grid)] pt-2 text-[var(--text-muted)]">
        <Phone size={13} className="hover:text-[var(--brand)]" />
        <span className="relative">
          <MessageSquare size={13} className="hover:text-[var(--brand)]" />
          {opp.unreadMessages && (
            <span className="absolute -right-1.5 -top-1.5 flex h-3.5 w-3.5 items-center justify-center rounded-full bg-[var(--brand)] text-[8.5px] font-bold text-white">
              {opp.unreadMessages}
            </span>
          )}
        </span>
        <Tag size={13} className="hover:text-[var(--brand)]" />
        <FileText size={13} className="hover:text-[var(--brand)]" />
        <CheckSquare size={13} className="hover:text-[var(--brand)]" />
        <CalendarDays size={13} className="hover:text-[var(--brand)]" />
      </div>
    </article>
  );
}
