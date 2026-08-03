export type Stage = {
  id: string;
  name: string;
  monetaryValue: number;
  opportunityCount: number;
  totalValue: number;
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

export type Contact = {
  id: string;
  name: string;
  email: string;
  phone: string;
  tags: string[];
  created: string;
};

export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type PipelineBoard = {
  pipelineName: string;
  totalOpportunities: number;
  stages: Stage[];
};

export type User = {
  id: string;
  licenseId: string;
  email: string;
  role: string;
  companyName: string;
};

export type LoginResponse = {
  token: string;
  tokenType: string;
  user: User;
};

export type DashboardKpis = {
  adSpend: number;
  leads: number;
  costPerLead: number;
  triage: number;
  costPerTriage: number;
  scBooked: number;
  costPerScBooked: number;
  scShown: number;
  costPerScShown: number;
  closes: number;
  costPerClose: number;
  cashCollected: number;
  revenue: number;
  roasCc: number;
  roasRevenue: number;
};

export type DailyPoint = {
  day: string;
  spend: number;
  cashCollected: number;
  revenue: number;
};

export type FunnelStep = {
  stage: string;
  pct: number;
  count: number;
};

export type TeamUser = {
  id: string;
  email: string;
  role: string;
};
