import { clearToken, getToken } from "./auth";

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number
  ) {
    super(message);
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const response = await fetch(`${API_URL}${path}`, { ...init, headers });
  if (response.status === 401) {
    clearToken();
    if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
      window.location.assign("/login");
    }
    throw new ApiError("Sessão expirada. Entre novamente.", response.status);
  }
  if (!response.ok) {
    const body = await response.text();
    throw new ApiError(body || response.statusText, response.status);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export type CurrentUser = {
  id: string;
  email: string;
  role: "ADMIN" | "CLOSER" | "SDR" | string;
  companyName: string;
};

export type ApiToken = {
  id: string;
  name: string;
  tokenPrefix: string;
  scopes: string[];
  createdAt: string;
  expiresAt: string | null;
  lastUsedAt: string | null;
  revokedAt: string | null;
  status: "ACTIVE" | "EXPIRED" | "REVOKED";
};

export async function login(email: string, password: string) {
  return apiRequest<{ token: string }>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function getCurrentUser() {
  return apiRequest<CurrentUser>("/api/auth/me");
}

export function listApiTokens() {
  return apiRequest<ApiToken[]>("/api/developer/tokens");
}

export function createApiToken(body: { name: string; scopes: string[]; expiresAt: string | null }) {
  return apiRequest<{ token: ApiToken; secret: string }>("/api/developer/tokens", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function revokeApiToken(id: string) {
  return apiRequest<ApiToken>(`/api/developer/tokens/${id}`, { method: "DELETE" });
}

export function inspectApiToken(token: string) {
  return apiRequest<{
    status: string;
    owner: string;
    name: string;
    scopes: string[];
    createdAt: string;
    expiresAt: string | null;
    lastUsedAt: string | null;
  }>("/api/developer/tokens/inspect", {
    method: "POST",
    body: JSON.stringify({ token }),
  });
}
