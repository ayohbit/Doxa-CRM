// Mocked CRM data — deterministic (no Math.random) so SSR/CSR render identically.

export type Stage = {
  id: string;
  name: string;
  monetaryValue: number;
};

export type Opportunity = {
  id: string;
  name: string;
  stageId: string;
  adSet?: string;
  revenueMonthly?: string;
  createdOn: string;
  value: number;
  email: string;
  phone: string;
  unreadMessages?: number;
};

export const PIPELINE_NAME = "Ads Pipeline";

export const stages: Stage[] = [
  { id: "form-no-booking", name: "Form Filled | No Booking", monetaryValue: 0 },
  { id: "form-no-answer", name: "Form Filled | No Answer", monetaryValue: 0 },
  { id: "new-lead", name: "New Lead", monetaryValue: 0 },
  { id: "early-triage", name: "Early Triage – No Answer", monetaryValue: 0 },
  { id: "waiting-reply", name: "#1 Waiting for Reply", monetaryValue: 0 },
  { id: "triage-not-qualified", name: "Triage Completed | Not Qualified", monetaryValue: 0 },
  { id: "triage-no-show", name: "Triage No Show", monetaryValue: 0 },
  { id: "qualified", name: "Qualified for Sales Call", monetaryValue: 0 },
  { id: "reconnect-1", name: "Reconnect #1", monetaryValue: 210000 },
  { id: "reconnect-2", name: "Reconnect #2", monetaryValue: 0 },
];

const adSets = [
  "P2 | Broad | US & CAN | RE #1",
  "P2 | Broad | US & CAN | RE #2",
  "P2 | Broad | US & CAN | Doctors #1",
  "P2 | Broad | CA(US) | Doctors #2",
  "AUTO - Broad [Hannah]",
  "AUTO - Broad",
  "paid",
];

const revenues = [
  "Less than $5k/mo",
  "$5k - $10k/mo",
  "$10k - $25k/mo",
  "$25k - $50k/mo",
  "$50k+/mo",
];

const firstNames = [
  "Kumar", "Duncan", "Dina", "Shahrokh", "Alexandra", "Faris", "Crystal",
  "Samuel", "Miguel", "Az", "Benjie", "Tatanisha", "Islam", "Amna", "Kadim",
  "Theresa", "Moises", "Joe", "Charles", "Bruno", "Nada", "Jay", "Neil",
  "Hannah", "Marcus", "Priya", "Diego", "Fatima", "Oliver", "Sofia", "Andre",
  "Keisha", "Tomás", "Ingrid", "Rashid", "Elena", "Victor", "Amara", "Felix",
  "Yuki", "Omar", "Bianca", "Trevor", "Leila", "Gustavo", "Wei", "Nadia",
  "Pedro", "Chloe", "Ibrahim",
];

const lastNames = [
  "Kumar", "Reyes", "Lebowitz", "Poormehr", "Quinones", "Abusharif",
  "Broussard", "Success", "Leite", "Az", "Benas", "Funches", "Bashir",
  "Khan", "Ahmed", "Le", "Issa", "Bradley", "Dow", "Caraponale", "Noorzay",
  "Mehta", "Regal", "Silva", "Thompson", "Patel", "Martinez", "Hassan",
  "Brooks", "Costa", "Williams", "Johnson", "Ferreira", "Larsen", "Ali",
  "Petrova", "Nguyen", "Okafor", "Weber", "Tanaka", "Farouk", "Rossi",
  "Mitchell", "Haddad", "Barbosa", "Chen", "Karimi", "Almeida", "Dubois",
  "Mansour",
];

// Roughly mirrors the reference pipeline distribution, capped for a usable demo.
const stageCounts: Record<string, number> = {
  "form-no-booking": 8,
  "form-no-answer": 6,
  "new-lead": 1,
  "early-triage": 0,
  "waiting-reply": 0,
  "triage-not-qualified": 9,
  "triage-no-show": 9,
  "qualified": 1,
  "reconnect-1": 7,
  "reconnect-2": 5,
};

// Real totals shown in the column headers (the board renders a sample).
export const stageTotals: Record<string, number> = {
  "form-no-booking": 22,
  "form-no-answer": 6,
  "new-lead": 1,
  "early-triage": 0,
  "waiting-reply": 0,
  "triage-not-qualified": 190,
  "triage-no-show": 289,
  "qualified": 1,
  "reconnect-1": 125,
  "reconnect-2": 14,
};

function pad(n: number) {
  return n.toString().padStart(2, "0");
}

function makeDate(seed: number): string {
  const months = ["Apr", "May", "Jun"];
  const month = months[seed % 3];
  const day = (seed * 7) % 28 + 1;
  const hour = (seed * 5) % 12 + 1;
  const min = (seed * 13) % 60;
  const ampm = seed % 2 === 0 ? "am" : "pm";
  return `${month} ${day} 2026, ${hour}:${pad(min)}${ampm} (GMT-4)`;
}

function buildOpportunities(): Opportunity[] {
  const out: Opportunity[] = [];
  let i = 0;
  for (const stage of stages) {
    const count = stageCounts[stage.id] ?? 0;
    for (let j = 0; j < count; j++) {
      const fn = firstNames[i % firstNames.length];
      const ln = lastNames[(i * 3 + 7) % lastNames.length];
      const name = `${fn} ${ln}`;
      out.push({
        id: `opp-${i}`,
        name,
        stageId: stage.id,
        adSet: i % 3 !== 1 ? adSets[i % adSets.length] : undefined,
        revenueMonthly: i % 4 !== 2 ? revenues[(i * 2) % revenues.length] : undefined,
        createdOn: makeDate(i + 3),
        value: stage.id === "reconnect-1" ? 30000 : stage.id === "qualified" ? 15000 : 0,
        email: `${fn.toLowerCase()}.${ln.toLowerCase().replace(/[^a-z]/g, "")}@example.com`,
        phone: `+1 (407) 555-0${pad((i * 37) % 100)}${i % 10}`,
        unreadMessages: i % 5 === 0 ? (i % 3) + 1 : undefined,
      });
      i++;
    }
  }
  return out;
}

export const opportunities: Opportunity[] = buildOpportunities();

export const totalOpportunities = 1421;

// ---------- Dashboard data ----------

export const kpis = {
  adSpend: 2370,
  leads: 15,
  costPerLead: 158,
  triage: 14,
  costPerTriage: 169,
  scBooked: 13,
  costPerScBooked: 182,
  scShown: 9,
  costPerScShown: 263,
  closes: 1,
  costPerClose: 2370,
  cashCollected: 5700,
  revenue: 5700,
  roasCC: 2.4,
  roasRevenue: 2.4,
};

export type DailyPoint = {
  day: string;
  spend: number;
  cashCollected: number;
  revenue: number;
};

export const dailySeries: DailyPoint[] = Array.from({ length: 30 }, (_, i) => {
  const d = new Date(2026, 5, 13 + i); // Jun 13 → Jul 12, 2026
  const label = d.toLocaleDateString("en-US", { month: "short", day: "numeric" });
  // Smooth deterministic curves: spend hovers ~$80/day, one big collection spike.
  const spend = Math.round(60 + 35 * Math.abs(Math.sin(i * 0.7)) + (i % 4) * 6);
  const peak = Math.exp(-((i - 14) ** 2) / 12);
  const cashCollected = Math.round(5700 * peak * 0.98);
  const revenue = cashCollected;
  return { day: label, spend, cashCollected, revenue };
});

export type FunnelStep = { stage: string; pct: number; count: number };

export const funnel: FunnelStep[] = [
  { stage: "Leads", pct: 100, count: 15 },
  { stage: "Triage", pct: 93, count: 14 },
  { stage: "SC Booked", pct: 87, count: 13 },
  { stage: "SC Shown", pct: 60, count: 9 },
  { stage: "Closes", pct: 7, count: 1 },
];

// ---------- Contacts ----------

export type Contact = {
  id: string;
  name: string;
  email: string;
  phone: string;
  tags: string[];
  created: string;
};

const tagPool = ["ads-lead", "triage-done", "qualified", "no-show", "reconnect", "hot"];

export const contacts: Contact[] = opportunities.slice(0, 30).map((o, i) => ({
  id: `c-${i}`,
  name: o.name,
  email: o.email,
  phone: o.phone,
  tags: [tagPool[i % tagPool.length], ...(i % 3 === 0 ? [tagPool[(i + 2) % tagPool.length]] : [])],
  created: o.createdOn.split(",")[0],
}));

export function fmtMoney(n: number): string {
  if (n >= 1000) return `$${(n / 1000).toFixed(2)}k`;
  return `$${n}`;
}

export function fmtMoneyFull(n: number): string {
  return `$${n.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}
