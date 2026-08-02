import {
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Filter,
  ArrowUpDown,
  Search,
  Settings2,
  Download,
  Plus,
  LayoutGrid,
  List,
  ListFilter,
  ClipboardList,
} from "lucide-react";
import {
  PIPELINE_NAME,
  stages,
  stageTotals,
  opportunities,
  totalOpportunities,
  fmtMoneyFull,
} from "@/lib/data";
import OpportunityCard from "@/components/opportunity-card";

export default function OpportunitiesPage() {
  return (
    <div className="flex h-full flex-col bg-white">
      {/* Page header */}
      <div className="border-b border-[var(--hairline)] px-5 pt-4">
        <div className="flex items-center gap-6">
          <h1 className="text-lg font-semibold">Opportunities</h1>
          <nav className="flex gap-5 text-[13px]">
            <span className="border-b-2 border-[var(--brand)] pb-2 font-medium text-[var(--brand)]">
              Opportunities
            </span>
            <span className="pb-2 text-[var(--text-secondary)]">Pipelines</span>
            <span className="pb-2 text-[var(--text-secondary)]">Bulk Actions</span>
          </nav>
        </div>
      </div>

      {/* Toolbar */}
      <div className="flex items-center gap-3 border-b border-[var(--hairline)] px-5 py-3">
        <button className="flex items-center gap-2 rounded-md border border-[var(--grid)] px-3 py-1.5 text-[13px] font-medium hover:bg-neutral-50">
          {PIPELINE_NAME}
          <ChevronDown size={14} className="text-[var(--text-muted)]" />
        </button>
        <span className="rounded-full bg-blue-50 px-2.5 py-1 text-[12px] font-medium text-[var(--brand)]">
          {totalOpportunities.toLocaleString("en-US")} opportunities
        </span>
        <div className="ml-auto flex items-center gap-2">
          <div className="flex overflow-hidden rounded-md border border-[var(--grid)]">
            <button className="border-r border-[var(--grid)] bg-blue-50 p-1.5 text-[var(--brand)]">
              <LayoutGrid size={14} />
            </button>
            <button className="p-1.5 text-[var(--text-muted)] hover:bg-neutral-50">
              <List size={14} />
            </button>
          </div>
          <button className="flex items-center gap-1.5 rounded-md border border-[var(--grid)] px-3 py-1.5 text-[13px] text-[var(--text-secondary)] hover:bg-neutral-50">
            <Download size={13} />
            Import
          </button>
          <button className="flex items-center gap-1.5 rounded-md bg-[var(--brand)] px-3 py-1.5 text-[13px] font-medium text-white hover:opacity-90">
            <Plus size={14} />
            Add opportunity
          </button>
        </div>
      </div>

      {/* Filters row */}
      <div className="flex items-center gap-2 border-b border-[var(--hairline)] px-5 py-2.5">
        <div className="flex items-center gap-4 text-[12.5px] text-[var(--text-secondary)]">
          <span className="flex items-center gap-1.5 border-b-2 border-[var(--brand)] pb-1 font-medium text-[var(--text-primary)]">
            <ListFilter size={13} /> Open opportunities
          </span>
          <span className="flex items-center gap-1.5 pb-1">
            <ClipboardList size={13} /> Leads Since 17/06
          </span>
          <span className="flex items-center gap-1.5 pb-1">
            <ClipboardList size={13} /> Leads Since 1704…
          </span>
          <span className="flex items-center gap-1 pb-1 text-[var(--brand)]">
            <Plus size={12} /> List
          </span>
        </div>
        <div className="ml-6 flex items-center gap-2">
          <button className="flex items-center gap-1.5 rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-[12px] text-[var(--brand)]">
            <Filter size={12} /> Advanced filters (1)
          </button>
          <button className="flex items-center gap-1.5 rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-[12px] text-[var(--brand)]">
            <ArrowUpDown size={12} /> Sort (1)
          </button>
        </div>
        <div className="ml-auto flex items-center gap-3">
          <div className="flex items-center gap-2 rounded-md border border-[var(--grid)] px-3 py-1.5">
            <Search size={13} className="text-[var(--text-muted)]" />
            <input
              placeholder="Search opportunities"
              className="w-44 bg-transparent text-[12.5px] outline-none placeholder:text-[var(--text-muted)]"
            />
          </div>
          <button className="flex items-center gap-1.5 text-[12.5px] text-[var(--text-secondary)] hover:text-[var(--text-primary)]">
            <Settings2 size={13} /> Manage fields
          </button>
        </div>
      </div>

      {/* Board */}
      <div className="flex min-h-0 flex-1 gap-3 overflow-x-auto bg-[var(--page)] p-4">
        {stages.map((stage) => {
          const cards = opportunities.filter((o) => o.stageId === stage.id);
          const total = stageTotals[stage.id] ?? cards.length;
          return (
            <section
              key={stage.id}
              className="flex w-[290px] shrink-0 flex-col rounded-lg"
            >
              <header className="rounded-t-lg border-b-2 border-[var(--brand)] bg-white px-3 py-2.5 shadow-sm">
                <div className="flex items-center justify-between gap-2">
                  <h2 className="truncate text-[13px] font-semibold">{stage.name}</h2>
                  <span className="flex shrink-0 text-[var(--text-muted)]">
                    <ChevronLeft size={14} />
                    <ChevronRight size={14} />
                  </span>
                </div>
                <p className="mt-0.5 text-[11.5px] text-[var(--text-muted)]">
                  {total.toLocaleString("en-US")} opportunities ·{" "}
                  <span className="font-medium text-[var(--text-secondary)]">
                    {fmtMoneyFull(stage.monetaryValue)}
                  </span>
                </p>
              </header>
              <div className="flex min-h-0 flex-1 flex-col gap-2 overflow-y-auto p-2">
                {cards.map((o) => (
                  <OpportunityCard key={o.id} opp={o} />
                ))}
                {cards.length === 0 && (
                  <p className="mt-6 text-center text-[12px] text-[var(--text-muted)]">
                    No opportunities
                  </p>
                )}
              </div>
            </section>
          );
        })}
      </div>
    </div>
  );
}
