"use client";

import {
  ResponsiveContainer,
  LineChart,
  Line,
  BarChart,
  Bar,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  LabelList,
} from "recharts";
import type { DailyPoint, FunnelStep } from "@/lib/types";

const C = {
  blue: "#2a78d6",
  aqua: "#1baf7a",
  yellow: "#eda100",
  violet: "#4a3aa7",
  red: "#e34948",
  grid: "#e1e0d9",
  muted: "#898781",
  ink: "#0b0b0b",
};

const funnelColors = [C.blue, C.aqua, C.yellow, C.violet, C.red];

function money(v: number) {
  return v >= 1000 ? `$${(v / 1000).toFixed(2)}k` : `$${v}`;
}

const tooltipStyle = {
  background: "#fcfcfb",
  border: `1px solid ${C.grid}`,
  borderRadius: 6,
  fontSize: 12,
  color: C.ink,
};

type ChartsProps = {
  dailySeries: DailyPoint[];
  funnel: FunnelStep[];
};

export default function Charts({ dailySeries, funnel }: ChartsProps) {
  return (
    <div className="mt-4 grid grid-cols-1 gap-4 pb-8 lg:grid-cols-2">
      <div className="rounded-lg border border-[var(--hairline)] bg-[var(--surface-1)] p-4 shadow-sm">
        <h2 className="text-[11px] font-medium uppercase tracking-wide text-[var(--text-muted)]">
          Daily Spend · CC · Revenue
        </h2>
        <div className="mt-3 h-64">
          <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 520, height: 256 }}>
            <LineChart data={dailySeries} margin={{ top: 4, right: 8, left: 4, bottom: 0 }}>
              <CartesianGrid stroke={C.grid} strokeWidth={1} vertical={false} />
              <XAxis
                dataKey="day"
                tick={{ fontSize: 11, fill: C.muted }}
                tickLine={false}
                axisLine={{ stroke: C.grid }}
                interval={5}
              />
              <YAxis
                tick={{ fontSize: 11, fill: C.muted }}
                tickLine={false}
                axisLine={false}
                tickFormatter={money}
                width={54}
              />
              <Tooltip
                contentStyle={tooltipStyle}
                formatter={(v) => money(Number(v))}
                cursor={{ stroke: C.muted, strokeDasharray: "3 3" }}
              />
              <Legend iconType="plainline" wrapperStyle={{ fontSize: 12, color: C.ink }} />
              <Line type="monotone" dataKey="spend" name="Spend" stroke={C.blue} strokeWidth={2} isAnimationActive={false} dot={false} activeDot={{ r: 4 }} />
              <Line type="monotone" dataKey="cashCollected" name="CC" stroke={C.aqua} strokeWidth={2} isAnimationActive={false} dot={false} activeDot={{ r: 4 }} />
              <Line type="monotone" dataKey="revenue" name="Revenue" stroke={C.violet} strokeWidth={2} isAnimationActive={false} strokeDasharray="5 3" dot={false} activeDot={{ r: 4 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="rounded-lg border border-[var(--hairline)] bg-[var(--surface-1)] p-4 shadow-sm">
        <h2 className="text-[11px] font-medium uppercase tracking-wide text-[var(--text-muted)]">
          Funnel Drop-off (% of Leads)
        </h2>
        <div className="mt-3 h-64">
          <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 520, height: 256 }}>
            <BarChart data={funnel} layout="vertical" margin={{ top: 4, right: 40, left: 4, bottom: 0 }} barCategoryGap="28%">
              <CartesianGrid stroke={C.grid} strokeWidth={1} horizontal={false} />
              <XAxis type="number" domain={[0, 100]} tick={{ fontSize: 11, fill: C.muted }} tickLine={false} axisLine={{ stroke: C.grid }} tickFormatter={(v) => `${v}%`} />
              <YAxis type="category" dataKey="stage" tick={{ fontSize: 11.5, fill: C.ink }} tickLine={false} axisLine={false} width={78} />
              <Tooltip contentStyle={tooltipStyle} formatter={(v, _n, item) => [`${v}% (${item.payload.count})`, "of leads"]} cursor={{ fill: "rgba(11,11,11,0.04)" }} />
              <Bar dataKey="pct" radius={[0, 4, 4, 0]} maxBarSize={22} isAnimationActive={false}>
                {funnel.map((f, i) => (
                  <Cell key={f.stage} fill={funnelColors[i]} />
                ))}
                <LabelList dataKey="pct" position="right" formatter={(v) => `${v}%`} style={{ fontSize: 11, fill: C.ink }} />
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
