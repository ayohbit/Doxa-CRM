import { clearToken, getToken } from "./auth";
import type {
  Contact,
  DailyPoint,
  DashboardKpis,
  FunnelStep,
  LoginResponse,
  Opportunity,
  Page,
  PipelineBoard,
  TeamUser,
  User,
} from "./types";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(init.headers);

  if (!headers.has("Content-Type") && init.body) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_URL}${path}`, { ...init, headers });

  if (response.status === 401) {
    clearToken();
    if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
      window.location.href = "/login";
    }
    throw new ApiError(401, "Unauthorized");
  }

  if (!response.ok) {
    const text = await response.text();
    throw new ApiError(response.status, text || response.statusText);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export function login(email: string, password: string) {
  return request<LoginResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function getMe() {
  return request<User>("/api/auth/me");
}

export function getPipelineBoard() {
  return request<PipelineBoard>("/api/pipelines/board");
}

export function listOpportunities(params: {
  stageSlug?: string;
  q?: string;
  page?: number;
  size?: number;
}) {
  const search = new URLSearchParams();
  if (params.stageSlug) search.set("stageSlug", params.stageSlug);
  if (params.q) search.set("q", params.q);
  search.set("page", String(params.page ?? 0));
  search.set("size", String(params.size ?? 50));
  return request<Page<Opportunity>>(`/api/opportunities?${search}`);
}

export function listContacts(params: { q?: string; page?: number; size?: number }) {
  const search = new URLSearchParams();
  if (params.q) search.set("q", params.q);
  search.set("page", String(params.page ?? 0));
  search.set("size", String(params.size ?? 50));
  return request<Page<Contact>>(`/api/contacts?${search}`);
}

function dashboardQuery(params: {
  periodDays: number;
  assignedUserId?: string;
  adSet?: string;
}) {
  const search = new URLSearchParams();
  search.set("periodDays", String(params.periodDays));
  if (params.assignedUserId) search.set("assignedUserId", params.assignedUserId);
  if (params.adSet) search.set("adSet", params.adSet);
  return search.toString();
}

export function getDashboardKpis(params: {
  periodDays: number;
  assignedUserId?: string;
  adSet?: string;
}) {
  return request<DashboardKpis>(`/api/dashboard/kpis?${dashboardQuery(params)}`);
}

export function getDashboardDailySeries(params: {
  periodDays: number;
  assignedUserId?: string;
  adSet?: string;
}) {
  return request<DailyPoint[]>(`/api/dashboard/daily-series?${dashboardQuery(params)}`);
}

export function getDashboardFunnel(params: {
  periodDays: number;
  assignedUserId?: string;
  adSet?: string;
}) {
  return request<FunnelStep[]>(`/api/dashboard/funnel?${dashboardQuery(params)}`);
}

export function getDashboardAdSets() {
  return request<string[]>("/api/dashboard/ad-sets");
}

export function getDashboardTeam() {
  return request<TeamUser[]>("/api/dashboard/team");
}
