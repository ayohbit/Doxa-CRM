"use client";

import { useMemo, useState } from "react";
import { Check, Clipboard, Clock3, Play, Search } from "lucide-react";
import { API_URL } from "@/lib/api";
import { API_ENDPOINTS, type ApiEndpoint } from "@/lib/catalog";

const methodStyles: Record<ApiEndpoint["method"], string> = {
  GET: "bg-blue-100 text-blue-700",
  POST: "bg-emerald-100 text-emerald-700",
  PUT: "bg-amber-100 text-amber-800",
  PATCH: "bg-violet-100 text-violet-700",
  DELETE: "bg-red-100 text-red-700",
};

async function webhookSignature(secret: string, rawBody: string) {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(rawBody));
  const hex = Array.from(new Uint8Array(signature), (byte) => byte.toString(16).padStart(2, "0")).join("");
  return `sha256=${hex}`;
}

export default function ApiDocsPage() {
  const [selectedId, setSelectedId] = useState(API_ENDPOINTS[0].id);
  const [search, setSearch] = useState("");
  const [token, setToken] = useState("");
  const [route, setRoute] = useState(API_ENDPOINTS[0].path);
  const [body, setBody] = useState("");
  const [query, setQuery] = useState("");
  const [result, setResult] = useState("");
  const [status, setStatus] = useState<number | null>(null);
  const [duration, setDuration] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");

  const filtered = useMemo(() => API_ENDPOINTS.filter((endpoint) => {
    const text = `${endpoint.group} ${endpoint.path} ${endpoint.summary}`.toLowerCase();
    return text.includes(search.toLowerCase());
  }), [search]);
  const groups = [...new Set(filtered.map((endpoint) => endpoint.group))];
  const selected = API_ENDPOINTS.find((endpoint) => endpoint.id === selectedId) ?? API_ENDPOINTS[0];

  function choose(endpoint: ApiEndpoint) {
    setSelectedId(endpoint.id);
    setRoute(endpoint.path);
    setBody(endpoint.body ?? "");
    setQuery("");
    setResult("");
    setStatus(null);
    setNotice("");
  }

  async function tryRequest() {
    setNotice("");
    setResult("");
    setStatus(null);
    if (selected.authType !== "none" && !token.trim()) {
      setNotice(selected.authType === "webhook"
        ? "Informe o webhook secret da licença para gerar a assinatura."
        : "Informe um token de acesso para testar o endpoint.");
      return;
    }
    if (!route.startsWith("/api/") || route.startsWith("//")) {
      setNotice("A rota deve pertencer à API deste ambiente e começar com /api/.");
      return;
    }

    let payload: unknown;
    if (body.trim()) {
      try {
        payload = JSON.parse(body);
      } catch {
        setNotice("O body precisa ser um JSON válido.");
        return;
      }
    }
    const params = new URLSearchParams(query);
    const url = new URL(`${API_URL}${route}`);
    params.forEach((value, key) => url.searchParams.set(key, value));

    setBusy(true);
    const started = performance.now();
    try {
      const response = await fetch(url, {
        method: selected.method,
        headers: await requestHeaders(token.trim(), payload),
        ...(payload === undefined ? {} : { body: JSON.stringify(payload) }),
      });
      setStatus(response.status);
      const text = await response.text();
      try {
        setResult(JSON.stringify(JSON.parse(text), null, 2));
      } catch {
        setResult(text || (response.status === 204 ? "No Content" : response.statusText));
      }
    } catch (reason) {
      setNotice(reason instanceof Error ? reason.message : "Não foi possível conectar à API.");
    } finally {
      setDuration(Math.round(performance.now() - started));
      setBusy(false);
    }
  }

  async function requestHeaders(credential: string, payload: unknown) {
    const headers: Record<string, string> = {};
    if (selected.authType === "bearer" || !selected.authType) {
      headers.Authorization = `Bearer ${credential}`;
    } else if (selected.authType === "webhook" && payload !== undefined) {
      headers["X-Broker-Signature"] = await webhookSignature(credential, JSON.stringify(payload));
    }
    if (payload !== undefined) headers["Content-Type"] = "application/json";
    return headers;
  }

  async function copySnippet(language: "curl" | "fetch" | "python") {
    const requestUrl = `${API_URL}${route}${query ? `?${new URLSearchParams(query)}` : ""}`;
    let payload: unknown;
    try {
      payload = body.trim() ? JSON.parse(body) : {};
    } catch {
      setNotice("Corrija o JSON body antes de copiar o snippet.");
      return;
    }
    const authHeader = selected.authType === "webhook"
      ? "X-Broker-Signature: sha256=<HMAC-SHA256 do body com o webhook secret>"
      : "Authorization: Bearer YOUR_API_TOKEN";
    const pythonAuth = selected.authType === "webhook"
      ? `import hashlib\nimport hmac\nimport json\n\npayload = json.loads(${JSON.stringify(JSON.stringify(payload))})\nraw_body = json.dumps(payload, separators=(",", ":"), ensure_ascii=False)\nsecret = "YOUR_WEBHOOK_SECRET"\nsignature = "sha256=" + hmac.new(secret.encode(), raw_body.encode(), hashlib.sha256).hexdigest()\nresponse = requests.post("${requestUrl}", data=raw_body, headers={"X-Broker-Signature": signature, "Content-Type": "application/json"})`
      : `response = requests.${selected.method.toLowerCase()}(\n    "${requestUrl}",\n    headers={"Authorization": "Bearer YOUR_API_TOKEN"},${selected.method === "GET" || selected.method === "DELETE" ? "" : `\n    json=${JSON.stringify(payload, null, 4)},`}\n)`;
    const snippets = {
      curl: `curl -X ${selected.method} '${requestUrl}' \\\n  -H '${authHeader}' \\\n  -H 'Content-Type: application/json'${selected.method === "GET" || selected.method === "DELETE" ? "" : ` \\\n  -d '${JSON.stringify(payload).replaceAll("'", "'\\''")}'`}`,
      fetch: selected.authType === "webhook"
        ? `const rawBody = JSON.stringify(${JSON.stringify(payload, null, 2)});\nconst signature = "sha256=<HMAC-SHA256 do rawBody com seu webhook secret>";\nconst response = await fetch("${requestUrl}", {\n  method: "POST",\n  headers: { "X-Broker-Signature": signature, "Content-Type": "application/json" },\n  body: rawBody,\n});`
        : `const response = await fetch("${requestUrl}", {\n  method: "${selected.method}",\n  headers: {\n    Authorization: "Bearer YOUR_API_TOKEN",\n    "Content-Type": "application/json",\n  },${selected.method === "GET" || selected.method === "DELETE" ? "" : `\n  body: JSON.stringify(${JSON.stringify(payload, null, 2)}),`}\n});\nconst data = await response.json();`,
      python: `import requests\n\n${pythonAuth}\nprint(response.status_code, response.text)`,
    };
    try {
      await navigator.clipboard.writeText(snippets[language]);
      setNotice(`Snippet ${language} copiado.`);
    } catch {
      setNotice("Não foi possível copiar. Verifique a permissão da área de transferência.");
    }
  }

  return (
    <div className="p-6 lg:p-8">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-wide text-[var(--brand)]">Developer tools</p>
          <h1 className="mt-1 text-2xl font-semibold">API Documentation</h1>
          <p className="mt-1 text-[13px] text-[var(--text-secondary)]">Explore, teste e integre com os recursos do DOXA CRM.</p>
        </div>
        <div className="rounded-md border border-[var(--grid)] bg-white px-3 py-2 font-mono text-[12px] text-[var(--text-secondary)]">
          Base URL <span className="ml-2 text-[var(--brand)]">{API_URL}</span>
        </div>
      </div>

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.05fr)_minmax(420px,0.95fr)]">
        <section className="min-w-0 rounded-xl border border-[var(--grid)] bg-white">
          <div className="border-b border-[var(--grid)] p-4">
            <div className="relative">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Buscar recurso ou rota..."
                className="w-full rounded-md border border-[var(--grid)] bg-transparent py-2 pl-9 pr-3 text-[12px] outline-none focus:border-[var(--brand)]"
              />
            </div>
            <div className="mt-3 flex gap-2 text-[11px] text-[var(--text-muted)]">
              <span>{API_ENDPOINTS.length} endpoints documentados</span>
              <span>·</span>
              <span>Autenticação Bearer</span>
            </div>
          </div>
          <div className="max-h-[calc(100vh-270px)] overflow-y-auto p-3">
            {groups.map((group) => (
              <div key={group} className="mb-4">
                <h2 className="px-2 pb-1 text-[10px] font-semibold uppercase tracking-wide text-[var(--text-muted)]">{group}</h2>
                <div className="space-y-1">
                  {filtered.filter((endpoint) => endpoint.group === group).map((endpoint) => (
                    <button
                      type="button"
                      key={endpoint.id}
                      onClick={() => choose(endpoint)}
                      className={`w-full rounded-md border px-3 py-2.5 text-left transition-colors ${
                        selected.id === endpoint.id
                          ? "border-blue-200 bg-[var(--sidebar-active)]"
                          : "border-transparent hover:bg-[var(--sidebar-hover)]"
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <span className={`w-[58px] rounded px-1.5 py-0.5 text-center text-[10px] font-bold ${methodStyles[endpoint.method]}`}>
                          {endpoint.method}
                        </span>
                        <code className="truncate text-[11px] font-medium">{endpoint.path}</code>
                      </div>
                      <p className="mt-1.5 pl-[66px] text-[11px] text-[var(--text-muted)]">{endpoint.summary}</p>
                    </button>
                  ))}
                </div>
              </div>
            ))}
            {filtered.length === 0 && <p className="p-5 text-center text-[12px] text-[var(--text-muted)]">Nenhum endpoint encontrado.</p>}
          </div>
        </section>

        <section className="min-w-0 space-y-4">
          <div className="rounded-xl border border-[var(--grid)] bg-white p-5">
            <div className="flex flex-wrap items-center gap-2">
              <span className={`rounded px-2 py-1 text-[10px] font-bold ${methodStyles[selected.method]}`}>{selected.method}</span>
              <code className="text-[13px] font-semibold">{selected.path}</code>
            </div>
            <p className="mt-2 text-[12px] text-[var(--text-secondary)]">{selected.summary}</p>
            <div className="mt-4 flex flex-wrap gap-1.5">
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-[10px] text-slate-600">{selected.authType === "webhook" ? "HMAC SHA-256" : selected.authType === "none" ? "Público" : "Authorization: Bearer"}</span>
              <span className="rounded-full bg-violet-100 px-2.5 py-1 text-[10px] text-violet-700">Scope: {selected.scope}</span>
            </div>
            {selected.headers && <div className="mt-4"><h3 className="mb-1 text-[11px] font-semibold">Headers</h3><p className="text-[11px] text-[var(--text-muted)]">{selected.headers.join(" · ")}</p></div>}
            {selected.query && <div className="mt-4"><h3 className="mb-1 text-[11px] font-semibold">Query parameters</h3><p className="text-[11px] text-[var(--text-muted)]">{selected.query.join(" · ")}</p></div>}
            {selected.body && <div className="mt-4"><h3 className="mb-1 text-[11px] font-semibold">Body example</h3><pre className="overflow-x-auto rounded-md bg-slate-950 p-3 text-[10px] text-slate-100">{JSON.stringify(JSON.parse(selected.body), null, 2)}</pre></div>}
            <div className="mt-4"><h3 className="mb-1 text-[11px] font-semibold">Response example</h3><pre className="overflow-x-auto rounded-md bg-slate-950 p-3 text-[10px] text-slate-100">{selected.response}</pre></div>
            <div className="mt-4 flex flex-wrap gap-2">
              {(["curl", "fetch", "python"] as const).map((lang) => (
                <button key={lang} type="button" onClick={() => void copySnippet(lang)} className="flex items-center gap-1 rounded-md border border-[var(--grid)] px-2.5 py-1.5 text-[10px] hover:bg-[var(--sidebar-hover)]">
                  <Clipboard size={12} /> Copiar {lang === "fetch" ? "fetch" : lang}
                </button>
              ))}
            </div>
          </div>

          <div className="rounded-xl border border-[var(--grid)] bg-white p-5">
            <div className="mb-4 flex items-center justify-between">
              <div><h2 className="text-[14px] font-semibold">Try it out</h2><p className="mt-0.5 text-[11px] text-[var(--text-muted)]">A requisição será enviada para o backend configurado.</p></div>
              <span className="flex items-center gap-1 text-[10px] text-[var(--text-muted)]"><Clock3 size={12} /> Real API</span>
            </div>
            {selected.authType !== "none" && <label className="block text-[11px] font-medium">{selected.authType === "webhook" ? "Webhook secret" : "Access token"}
              <input value={token} onChange={(event) => setToken(event.target.value)} type="password" autoComplete="off" placeholder={selected.authType === "webhook" ? "whsec_..." : "doxa_live_..."} className="mt-1 w-full rounded-md border border-[var(--grid)] bg-transparent px-3 py-2 font-mono text-[11px] outline-none focus:border-[var(--brand)]" />
            </label>
            }
            <label className="mt-3 block text-[11px] font-medium">Request path
              <input value={route} onChange={(event) => setRoute(event.target.value)} className="mt-1 w-full rounded-md border border-[var(--grid)] bg-transparent px-3 py-2 font-mono text-[11px] outline-none focus:border-[var(--brand)]" />
            </label>
            {selected.query && <label className="mt-3 block text-[11px] font-medium">Query string
              <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="page=0&size=20" className="mt-1 w-full rounded-md border border-[var(--grid)] bg-transparent px-3 py-2 font-mono text-[11px] outline-none focus:border-[var(--brand)]" />
            </label>}
            {selected.method !== "GET" && selected.method !== "DELETE" && <label className="mt-3 block text-[11px] font-medium">JSON body
              <textarea rows={5} value={body} onChange={(event) => setBody(event.target.value)} placeholder="{}" className="mt-1 w-full rounded-md border border-[var(--grid)] bg-transparent p-3 font-mono text-[11px] outline-none focus:border-[var(--brand)]" />
            </label>}
            {notice && <p role="status" className="mt-3 text-[11px] text-amber-700">{notice}</p>}
            <button type="button" onClick={() => void tryRequest()} disabled={busy} className="mt-4 flex items-center gap-2 rounded-md bg-[var(--brand)] px-4 py-2 text-[11px] font-medium text-white hover:opacity-90 disabled:opacity-60">
              <Play size={13} /> {busy ? "Enviando..." : "Enviar requisição"}
            </button>
            {result && <div className="mt-4 overflow-hidden rounded-md border border-[var(--grid)]">
              <div className="flex items-center justify-between bg-[var(--sidebar-hover)] px-3 py-2 text-[10px]">
                <span className="flex items-center gap-1 font-semibold"><Check size={12} className={status && status < 400 ? "text-emerald-600" : "text-red-600"} /> Status {status}</span>
                <span>{duration} ms</span>
              </div>
              <pre className="max-h-64 overflow-auto bg-slate-950 p-3 text-[10px] text-slate-100">{result}</pre>
            </div>}
          </div>
        </section>
      </div>
    </div>
  );
}
