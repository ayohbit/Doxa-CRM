"use client";

import { RefreshCw, ChevronRight } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import AuthGuard from "@/components/auth-guard";
import Charts from "@/components/charts";
import {
  getDashboardAdSets,
  getDashboardDailySeries,
  getDashboardFunnel,
  getDashboardKpis,
  getDashboardTeam,
  getMe,
} from "@/lib/api";
import { fmtMoney } from "@/lib/format";
import type { DailyPoint, DashboardKpis, FunnelStep, TeamUser, User } from "@/lib/types";

const ranges = [
  { label: "7D", days: 7 },
  { label: "14D", days: 14 },
  { label: "30D", days: 30 },
  { label: "90D", days: 90 },
  { label: "YTD", days: 365 },
];

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
        <span className="text-[11px] font-medium uppercase tracking-wide text-[var(--text-muted)]">{label}</span>
        <span className="mt-1 text-2xl font-bold">{value}</span>
        {cost && <span className="mt-0.5 text-[12px] text-[var(--text-secondary)]">{cost}</span>}
      </div>
      {!last && (
        <div className="flex flex-col items-center justify-center gap-0.5 text-[var(--text-muted)]">
          {pctToNext && <span className="text-[11px] font-semibold text-[var(--good-text)]">{pctToNext}</span>}
          <ChevronRight size={14} />
        </div>
      )}
    </>
  );
}

function BigTile({ label, value, tone }: { label: string; value: string; tone: string }) {
  return (
    <div className="rounded-lg border border-[var(--hairline)] bg-[var(--surface-1)] p-4 shadow-sm">
      <span className="text-[11px] font-medium uppercase tracking-wide text-[var(--text-muted)]">{label}</span>
      <p className="mt-1 text-3xl font-bold" style={{ color: tone }}>{value}</p>
    </div>
  );
}

function pctBetween(current: number, next: number) {
  if (current === 0) return undefined;
  return `${Math.round((next / current) * 100)}%`;
}

export default function DashboardPage() {
  const [periodDays, setPeriodDays] = useState(30);
  const [assignedUserId, setAssignedUserId] = useState<string>("");
  const [adSet, setAdSet] = useState<string>("");
  const [kpis, setKpis] = useState<DashboardKpis | null>(null);
  const [dailySeries, setDailySeries] = useState<DailyPoint[]>([]);
  const [funnel, setFunnel] = useState<FunnelStep[]>([]);
  const [adSets, setAdSets] = useState<string[]>([]);
  const [team, setTeam] = useState<TeamUser[]>([]);
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const query = useMemo(
    () => ({
      periodDays,
      assignedUserId: assignedUserId || undefined,
      adSet: adSet || undefined,
    }),
    [periodDays, assignedUserId, adSet]
  );

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [kpiData, dailyData, funnelData] = await Promise.all([
        getDashboardKpis(query),
        getDashboardDailySeries(query),
        getDashboardFunnel(query),
      ]);
      setKpis(kpiData);
      setDailySeries(dailyData);
      setFunnel(funnelData);
    } catch {
      setError("Não foi possível carregar o dashboard.");
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    async function bootstrap() {
      try {
        const me = await getMe();
        setUser(me);
        const sets = await getDashboardAdSets();
        setAdSets(sets);
        if (me.role === "ADMIN") {
          const members = await getDashboardTeam();
          setTeam(members.filter((m) => m.role === "CLOSER" || m.role === "SDR"));
        }
      } catch {
        // filters remain optional
      }
    }
    bootstrap();
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const showTeamFilter = user?.role === "ADMIN";

  return (
    <AuthGuard>
      <div className="mx-auto max-w-[1200px] p-5">
        <div className="flex items-center justify-between">
          <div className="flex gap-1.5">
            {ranges.map((r) => (
              <button
                key={r.label}
                onClick={() => setPeriodDays(r.days)}
                className={`rounded-md border px-3 py-1 text-[12.5px] font-medium ${
                  periodDays === r.days
                    ? "border-[var(--brand)] bg-blue-50 text-[var(--brand)]"
                    : "border-[var(--grid)] bg-white text-[var(--text-secondary)] hover:bg-neutral-50"
                }`}
              >
                {r.label}
              </button>
            ))}
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={loadDashboard}
              className="flex items-center gap-1.5 rounded-md border border-[var(--grid)] bg-white px-3 py-1 text-[12.5px] text-[var(--text-secondary)] hover:bg-neutral-50"
            >
              <RefreshCw size={12} /> Refresh
            </button>
          </div>
        </div>

        <div className="mt-4 flex items-baseline gap-2">
          <h1 className="text-lg font-semibold">Dashboard</h1>
          <span className="text-[12.5px] text-[var(--text-muted)]">KPI overview · dados reais</span>
        </div>

        <div className="mt-3 flex flex-wrap items-center gap-2">
          {showTeamFilter && (
            <select
              value={assignedUserId}
              onChange={(e) => setAssignedUserId(e.target.value)}
              className="min-w-[200px] rounded-md border border-[var(--grid)] bg-white px-3 py-1.5 text-[13px] text-[var(--text-secondary)]"
            >
              <option value="">All team members</option>
              {team.map((member) => (
                <option key={member.id} value={member.id}>
                  {member.email} ({member.role})
                </option>
              ))}
            </select>
          )}

          <select
            value={adSet}
            onChange={(e) => setAdSet(e.target.value)}
            className="min-w-[200px] rounded-md border border-[var(--grid)] bg-white px-3 py-1.5 text-[13px] text-[var(--text-secondary)]"
          >
            <option value="">All Ad Sets</option>
            {adSets.map((set) => (
              <option key={set} value={set}>
                {set}
              </option>
            ))}
          </select>

          <button
            onClick={() => {
              setAssignedUserId("");
              setAdSet("");
            }}
            className="text-[12.5px] text-[var(--text-muted)] hover:text-[var(--text-primary)]"
          >
            Clear
          </button>
        </div>

        {error && (
          <div className="mt-4 rounded-md border border-red-200 bg-red-50 px-4 py-2 text-[13px] text-red-700">
            {error}
          </div>
        )}

        {loading || !kpis ? (
          <p className="mt-8 text-center text-[13px] text-[var(--text-muted)]">Carregando dashboard...</p>
        ) : (
          <>
            <div className="mt-4 flex items-stretch rounded-lg border border-[var(--hairline)] bg-[var(--surface-1)] shadow-sm">
              <FunnelKpi label="Ad Spend" value={fmtMoney(Number(kpis.adSpend))} />
              <FunnelKpi label="Leads" value={String(kpis.leads)} cost={`$${kpis.costPerLead}`} pctToNext={pctBetween(kpis.leads, kpis.triage)} />
              <FunnelKpi label="Triage" value={String(kpis.triage)} cost={`$${kpis.costPerTriage}`} pctToNext={pctBetween(kpis.triage, kpis.scBooked)} />
              <FunnelKpi label="SC Booked" value={String(kpis.scBooked)} cost={`$${kpis.costPerScBooked}`} pctToNext={pctBetween(kpis.scBooked, kpis.scShown)} />
              <FunnelKpi label="SC Shown" value={String(kpis.scShown)} cost={`$${kpis.costPerScShown}`} pctToNext={pctBetween(kpis.scShown, kpis.closes)} />
              <FunnelKpi label="Closes" value={String(kpis.closes)} cost={fmtMoney(Number(kpis.costPerClose))} last />
            </div>

            <div className="mt-4 grid grid-cols-2 gap-4 lg:grid-cols-4">
              <BigTile label="Cash Collected" value={fmtMoney(Number(kpis.cashCollected))} tone="var(--good-text)" />
              <BigTile label="Revenue" value={fmtMoney(Number(kpis.revenue))} tone="var(--good-text)" />
              <BigTile label="ROAS (CC)" value={`${Number(kpis.roasCc).toFixed(2)}x`} tone="var(--series-3)" />
              <BigTile label="ROAS (Revenue)" value={`${Number(kpis.roasRevenue).toFixed(2)}x`} tone="var(--series-3)" />
            </div>

            <Charts dailySeries={dailySeries} funnel={funnel} />
          </>
        )}
      </div>
    </AuthGuard>
  );
}
