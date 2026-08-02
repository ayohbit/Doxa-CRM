import { RefreshCw, ChevronRight, ChevronDown } from "lucide-react";
import { kpis, fmtMoney } from "@/lib/data";
import Charts from "@/components/charts";

const ranges = ["7D", "14D", "30D", "90D", "YTD", "Custom"];

function FunnelKpi({
  label,
  value,
  cost,
  pctToNext,
  last,
}: {
  label: string;
  value: string;
  cost?: string;
  pctToNext?: string;
  last?: boolean;
}) {
  return (
    <>
      <div className="flex flex-1 flex-col items-center px-4 py-4">
        <span className="text-[11px] font-medium uppercase tracking-wide text-[var(--text-muted)]">
          {label}
        </span>
        <span className="mt-1 text-2xl font-bold">{value}</span>
        {cost && <span className="mt-0.5 text-[12px] text-[var(--text-secondary)]">{cost}</span>}
      </div>
      {!last && (
        <div className="flex flex-col items-center justify-center gap-0.5 text-[var(--text-muted)]">
          {pctToNext && (
            <span className="text-[11px] font-semibold text-[var(--good-text)]">{pctToNext}</span>
          )}
          <ChevronRight size={14} />
        </div>
      )}
    </>
  );
}

function BigTile({ label, value, tone }: { label: string; value: string; tone: string }) {
  return (
    <div className="rounded-lg border border-[var(--hairline)] bg-[var(--surface-1)] p-4 shadow-sm">
      <span className="text-[11px] font-medium uppercase tracking-wide text-[var(--text-muted)]">
        {label}
      </span>
      <p className="mt-1 text-3xl font-bold" style={{ color: tone }}>
        {value}
      </p>
    </div>
  );
}

export default function DashboardPage() {
  return (
    <div className="mx-auto max-w-[1200px] p-5">
      {/* Range selector */}
      <div className="flex items-center justify-between">
        <div className="flex gap-1.5">
          {ranges.map((r) => (
            <button
              key={r}
              className={`rounded-md border px-3 py-1 text-[12.5px] font-medium ${
                r === "30D"
                  ? "border-[var(--brand)] bg-blue-50 text-[var(--brand)]"
                  : "border-[var(--grid)] bg-white text-[var(--text-secondary)] hover:bg-neutral-50"
              }`}
            >
              {r}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-3">
          <button className="flex items-center gap-1.5 rounded-md border border-[var(--grid)] bg-white px-3 py-1 text-[12.5px] text-[var(--text-secondary)] hover:bg-neutral-50">
            <RefreshCw size={12} /> Refresh
          </button>
          <span className="flex items-center gap-1.5 text-[11.5px] text-[var(--text-muted)]">
            <span className="h-2 w-2 rounded-full bg-[var(--good)]" /> 8m ago
          </span>
        </div>
      </div>

      <div className="mt-4 flex items-baseline gap-2">
        <h1 className="text-lg font-semibold">Dashboard</h1>
        <span className="text-[12.5px] text-[var(--text-muted)]">KPI overview</span>
      </div>

      {/* Filters */}
      <div className="mt-3 flex flex-wrap items-center gap-2">
        {["All Campaigns", "All Ad Sets", "All Creatives"].map((f) => (
          <button
            key={f}
            className="flex min-w-[200px] items-center justify-between rounded-md border border-[var(--grid)] bg-white px-3 py-1.5 text-[13px] text-[var(--text-secondary)] hover:bg-neutral-50"
          >
            {f}
            <ChevronDown size={14} className="text-[var(--text-muted)]" />
          </button>
        ))}
        <button className="text-[12.5px] text-[var(--text-muted)] hover:text-[var(--text-primary)]">
          Clear
        </button>
      </div>

      {/* Funnel KPI strip */}
      <div className="mt-4 flex items-stretch rounded-lg border border-[var(--hairline)] bg-[var(--surface-1)] shadow-sm">
        <FunnelKpi label="Ad Spend" value={fmtMoney(kpis.adSpend)} />
        <FunnelKpi label="Leads" value={String(kpis.leads)} cost={`$${kpis.costPerLead}`} pctToNext="93%" />
        <FunnelKpi label="Triage" value={String(kpis.triage)} cost={`$${kpis.costPerTriage}`} pctToNext="93%" />
        <FunnelKpi label="SC Booked" value={String(kpis.scBooked)} cost={`$${kpis.costPerScBooked}`} pctToNext="69%" />
        <FunnelKpi label="SC Shown" value={String(kpis.scShown)} cost={`$${kpis.costPerScShown}`} pctToNext="11%" />
        <FunnelKpi label="Closes" value={String(kpis.closes)} cost={fmtMoney(kpis.costPerClose)} last />
      </div>

      {/* Money tiles */}
      <div className="mt-4 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <BigTile label="Cash Collected" value={fmtMoney(kpis.cashCollected)} tone="var(--good-text)" />
        <BigTile label="Revenue" value={fmtMoney(kpis.revenue)} tone="var(--good-text)" />
        <BigTile label="ROAS (CC)" value={`${kpis.roasCC.toFixed(2)}x`} tone="var(--series-3)" />
        <BigTile label="ROAS (Revenue)" value={`${kpis.roasRevenue.toFixed(2)}x`} tone="var(--series-3)" />
      </div>

      <Charts />
    </div>
  );
}
