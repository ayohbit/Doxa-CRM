"use client";

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
import { useCallback, useEffect, useState } from "react";
import AuthGuard from "@/components/auth-guard";
import OpportunityCard from "@/components/opportunity-card";
import { getPipelineBoard, listOpportunities } from "@/lib/api";
import { fmtMoneyFull } from "@/lib/format";
import type { Opportunity, Stage } from "@/lib/types";

export default function OpportunitiesPage() {
  const [pipelineName, setPipelineName] = useState("Ads Pipeline");
  const [totalOpportunities, setTotalOpportunities] = useState(0);
  const [stages, setStages] = useState<Stage[]>([]);
  const [opportunitiesByStage, setOpportunitiesByStage] = useState<Record<string, Opportunity[]>>({});
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadBoard = useCallback(async (query: string) => {
    setLoading(true);
    setError(null);
    try {
      const board = await getPipelineBoard();
      setPipelineName(board.pipelineName);
      setTotalOpportunities(board.totalOpportunities);
      setStages(board.stages);

      const results = await Promise.all(
        board.stages.map((stage) =>
          listOpportunities({ stageSlug: stage.id, q: query || undefined, size: 50 }).then(
            (page) => [stage.id, page.content] as const
          )
        )
      );

      setOpportunitiesByStage(Object.fromEntries(results));
    } catch {
      setError("Não foi possível carregar as oportunidades.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => loadBoard(search), search ? 300 : 0);
    return () => clearTimeout(timer);
  }, [search, loadBoard]);

  return (
    <AuthGuard>
      <div className="flex h-full flex-col bg-white">
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

        <div className="flex items-center gap-3 border-b border-[var(--hairline)] px-5 py-3">
          <button className="flex items-center gap-2 rounded-md border border-[var(--grid)] px-3 py-1.5 text-[13px] font-medium hover:bg-neutral-50">
            {pipelineName}
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
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-44 bg-transparent text-[12.5px] outline-none placeholder:text-[var(--text-muted)]"
              />
            </div>
            <button className="flex items-center gap-1.5 text-[12.5px] text-[var(--text-secondary)] hover:text-[var(--text-primary)]">
              <Settings2 size={13} /> Manage fields
            </button>
          </div>
        </div>

        {error && (
          <div className="border-b border-red-200 bg-red-50 px-5 py-2 text-[13px] text-red-700">
            {error}
          </div>
        )}

        {loading && (
          <div className="flex flex-1 items-center justify-center text-[13px] text-[var(--text-muted)]">
            Carregando oportunidades...
          </div>
        )}

        {!loading && (
          <div className="flex min-h-0 flex-1 gap-3 overflow-x-auto bg-[var(--page)] p-4">
            {stages.map((stage) => {
              const cards = opportunitiesByStage[stage.id] ?? [];
              const total = stage.opportunityCount;
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
                        {fmtMoneyFull(Number(stage.monetaryValue))}
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
        )}
      </div>
    </AuthGuard>
  );
}
