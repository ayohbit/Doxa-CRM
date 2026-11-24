"use client";

import { useEffect, useMemo, useState } from "react";
import { Check, Clipboard, ExternalLink, Network, Radio, Server } from "lucide-react";
import { API_URL } from "@/lib/api";

const tools = [
  {
    name: "search_contacts",
    description: "Search contacts visible to the authenticated user.",
    scope: "contacts:read",
    schema: { type: "object", properties: { query: { type: "string", description: "Name, email or phone query" } }, additionalProperties: false },
  },
  {
    name: "list_opportunities",
    description: "List opportunities visible to the authenticated user.",
    scope: "opportunities:read",
    schema: { type: "object", properties: { query: { type: "string" }, stageSlug: { type: "string" }, status: { type: "string", enum: ["OPEN", "WON", "LOST"] } }, additionalProperties: false },
  },
  {
    name: "update_opportunity_stage",
    description: "Move an opportunity to a different stage in the authenticated license.",
    scope: "opportunities:write",
    schema: { type: "object", properties: { opportunityId: { type: "string", format: "uuid" }, stageSlug: { type: "string" } }, required: ["opportunityId", "stageSlug"], additionalProperties: false },
  },
  {
    name: "get_dashboard_kpis",
    description: "Get aggregate dashboard KPIs for a period of up to 365 days.",
    scope: "dashboard:read",
    schema: { type: "object", properties: { periodDays: { type: "integer", minimum: 1, maximum: 365 } }, additionalProperties: false },
  },
];

const resource = {
  uri: "crm://dashboard/overview",
  name: "CRM dashboard overview",
  description: "Aggregate dashboard KPIs for the last 30 days.",
  requiredScope: "dashboard:read",
};

type Client = "claude" | "cursor" | "vscode";

export default function McpPage() {
  const [online, setOnline] = useState<boolean | null>(null);
  const [secret, setSecret] = useState("");
  const [client, setClient] = useState<Client>("claude");
  const [copied, setCopied] = useState(false);
  const [copyError, setCopyError] = useState("");
  const mcpUrl = process.env.NEXT_PUBLIC_MCP_URL ?? `${API_URL.replace(/\/$/, "")}/mcp`;
  const config = useMemo(() => {
    const key = client === "vscode" ? "servers" : "mcpServers";
    return JSON.stringify({
      [key]: {
        "doxa-crm": {
          type: "http",
          url: mcpUrl,
          headers: { Authorization: `Bearer ${secret || "PASTE_YOUR_API_TOKEN"}` },
        },
      },
    }, null, 2);
  }, [client, mcpUrl, secret]);

  useEffect(() => {
    const controller = new AbortController();
    fetch(`${API_URL}/api/health`, { signal: controller.signal })
      .then((response) => setOnline(response.ok))
      .catch(() => setOnline(false));
    return () => controller.abort();
  }, []);

  async function copyConfig() {
    try {
      await navigator.clipboard.writeText(config);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setCopied(false);
      setCopyError("Não foi possível copiar. Selecione e copie o JSON manualmente.");
    }
  }

  return (
    <div className="p-6 lg:p-8">
      <div className="mb-6">
        <p className="text-[11px] font-semibold uppercase tracking-wide text-[var(--brand)]">AI integrations</p>
        <h1 className="mt-1 text-2xl font-semibold">Model Context Protocol</h1>
        <p className="mt-1 text-[13px] text-[var(--text-secondary)]">Conecte assistentes ao CRM usando tokens com acesso limitado por escopo.</p>
      </div>

      <div className="grid gap-5 lg:grid-cols-[minmax(0,1.1fr)_minmax(340px,0.9fr)]">
        <section className="space-y-5">
          <div className="rounded-xl border border-[var(--grid)] bg-white p-5">
            <div className="flex items-start justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-50 text-[var(--brand)]"><Server size={19} /></div>
                <div><h2 className="text-[14px] font-semibold">Servidor DOXA CRM</h2><p className="mt-0.5 text-[11px] text-[var(--text-muted)]">Streamable HTTP · JSON-RPC 2.0</p></div>
              </div>
              <span className={`flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[10px] font-semibold ${online ? "bg-emerald-100 text-emerald-700" : online === false ? "bg-red-100 text-red-700" : "bg-slate-100 text-slate-500"}`}>
                <span className={`h-1.5 w-1.5 rounded-full ${online ? "bg-emerald-500" : online === false ? "bg-red-500" : "bg-slate-400"}`} />
                {online ? "Online" : online === false ? "Offline" : "Verificando"}
              </span>
            </div>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <div className="rounded-md bg-[var(--sidebar-hover)] p-3"><p className="text-[10px] text-[var(--text-muted)]">Endpoint MCP</p><code className="mt-1 block break-all text-[11px]">{mcpUrl}</code></div>
              <div className="rounded-md bg-[var(--sidebar-hover)] p-3"><p className="text-[10px] text-[var(--text-muted)]">Autenticação</p><code className="mt-1 block text-[11px]">Authorization: Bearer doxa_live_...</code></div>
            </div>
            <p className="mt-3 text-[10px] text-[var(--text-muted)]">O indicador consulta a disponibilidade da API CRM. MCP aceita requisições HTTP POST autenticadas com tokens de API ativos.</p>
          </div>

          <div className="rounded-xl border border-[var(--grid)] bg-white p-5">
            <div className="mb-4 flex items-center gap-2"><Network size={15} className="text-[var(--brand)]" /><h2 className="text-[14px] font-semibold">Tools disponíveis</h2></div>
            <div className="space-y-3">
              {tools.map((tool) => <div key={tool.name} className="rounded-lg border border-[var(--grid)] p-3">
                <div className="flex flex-wrap items-center justify-between gap-2"><code className="text-[12px] font-semibold">{tool.name}</code><span className="rounded-full bg-violet-100 px-2 py-1 text-[9px] text-violet-700">{tool.scope}</span></div>
                <p className="mt-1 text-[11px] text-[var(--text-secondary)]">{tool.description}</p>
                <pre className="mt-2 overflow-x-auto rounded bg-slate-950 p-2 text-[9px] text-slate-100">{JSON.stringify(tool.schema, null, 2)}</pre>
              </div>)}
            </div>
          </div>

          <div className="rounded-xl border border-[var(--grid)] bg-white p-5">
            <div className="mb-3 flex items-center gap-2"><Radio size={15} className="text-[var(--brand)]" /><h2 className="text-[14px] font-semibold">Resources disponíveis</h2></div>
            <div className="rounded-lg border border-[var(--grid)] p-3">
              <code className="text-[12px] font-semibold">{resource.uri}</code>
              <p className="mt-1 text-[11px] text-[var(--text-secondary)]">{resource.description}</p>
              <span className="mt-2 inline-block rounded-full bg-violet-100 px-2 py-1 text-[9px] text-violet-700">Escopo: {resource.requiredScope}</span>
            </div>
          </div>
        </section>

        <section className="h-fit rounded-xl border border-[var(--grid)] bg-white p-5">
          <div className="mb-4 flex items-center gap-2"><Clipboard size={15} className="text-[var(--brand)]" /><h2 className="text-[14px] font-semibold">Gerar configuração</h2></div>
          <label className="block text-[11px] font-medium">Cliente MCP
            <select value={client} onChange={(event) => setClient(event.target.value as Client)} className="mt-1 w-full rounded-md border border-[var(--grid)] bg-white px-3 py-2 text-[12px]">
              <option value="claude">Claude Desktop / Claude Code</option>
              <option value="cursor">Cursor</option>
              <option value="vscode">VS Code</option>
            </select>
          </label>
          <label className="mt-4 block text-[11px] font-medium">Token de API
            <input type="password" autoComplete="off" value={secret} onChange={(event) => setSecret(event.target.value)} placeholder="Cole aqui o token criado pelo Admin" className="mt-1 w-full rounded-md border border-[var(--grid)] bg-transparent px-3 py-2 font-mono text-[11px] outline-none focus:border-[var(--brand)]" />
          </label>
          <div className="mt-4 flex items-center justify-between">
            <h3 className="text-[11px] font-semibold">Bloco de configuração</h3>
            <button type="button" onClick={() => void copyConfig()} className="flex items-center gap-1 rounded-md border border-[var(--grid)] px-2.5 py-1.5 text-[10px] hover:bg-[var(--sidebar-hover)]">{copied ? <Check size={12} /> : <Clipboard size={12} />}{copied ? "Copiado" : "Copiar JSON"}</button>
          </div>
          <pre className="mt-2 overflow-x-auto rounded-md bg-slate-950 p-3 text-[10px] leading-5 text-slate-100">{config}</pre>
          {copyError && <p role="alert" className="mt-2 text-[10px] text-red-600">{copyError}</p>}
          <div className="mt-4 rounded-md border border-blue-100 bg-blue-50 p-3 text-[10px] leading-4 text-blue-900">
            <p className="font-semibold">Permissões necessárias</p>
            <p className="mt-1">Inclua <code>mcp:access</code> e os escopos das ferramentas desejadas (por exemplo, <code>contacts:read</code>, <code>opportunities:read</code> ou <code>dashboard:read</code>) ao criar o token. O endpoint ainda aplica as permissões e o isolamento por licença do CRM.</p>
          </div>
          <a href="/tokens" className="mt-4 inline-flex items-center gap-1 text-[11px] font-medium text-[var(--brand)] hover:underline">Gerenciar tokens <ExternalLink size={12} /></a>
        </section>
      </div>
    </div>
  );
}
