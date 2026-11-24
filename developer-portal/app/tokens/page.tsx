"use client";

import { FormEvent, useEffect, useState } from "react";
import { Check, Clipboard, KeyRound, Plus, ShieldAlert, Trash2 } from "lucide-react";
import {
  createApiToken,
  getCurrentUser,
  inspectApiToken,
  listApiTokens,
  revokeApiToken,
  type ApiToken,
  type CurrentUser,
} from "@/lib/api";

const scopes = [
  { id: "contacts:read", label: "Contacts · read" },
  { id: "contacts:write", label: "Contacts · write" },
  { id: "opportunities:read", label: "Opportunities · read" },
  { id: "opportunities:write", label: "Opportunities · write" },
  { id: "dashboard:read", label: "Dashboard · read" },
  { id: "integrations:read", label: "Integrations · read" },
  { id: "integrations:write", label: "Integrations · write" },
  { id: "mcp:access", label: "MCP · access" },
];

function dateLabel(value: string | null) {
  return value ? new Date(value).toLocaleString() : "—";
}

export default function TokensPage() {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [tokens, setTokens] = useState<ApiToken[]>([]);
  const [name, setName] = useState("");
  const [expiry, setExpiry] = useState("30");
  const [selectedScopes, setSelectedScopes] = useState<string[]>(["contacts:read", "opportunities:read", "mcp:access"]);
  const [secret, setSecret] = useState("");
  const [inspectValue, setInspectValue] = useState("");
  const [inspection, setInspection] = useState<Awaited<ReturnType<typeof inspectApiToken>> | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function refreshTokens() {
    setTokens(await listApiTokens());
  }

  useEffect(() => {
    getCurrentUser()
      .then(async (currentUser) => {
        setUser(currentUser);
        if (currentUser.role === "ADMIN") await refreshTokens();
      })
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Falha ao carregar o usuário."));
  }, []);

  function toggleScope(scope: string) {
    setSelectedScopes((current) => current.includes(scope)
      ? current.filter((value) => value !== scope)
      : [...current, scope]);
  }

  async function createToken(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setMessage("");
    if (!selectedScopes.length) {
      setError("Selecione ao menos um escopo.");
      return;
    }
    setBusy(true);
    try {
      const expiresAt = expiry === "never"
        ? null
        : new Date(Date.now() + Number(expiry) * 24 * 60 * 60 * 1000).toISOString();
      const created = await createApiToken({ name, scopes: selectedScopes, expiresAt });
      setSecret(created.secret);
      setName("");
      setMessage("Token criado. Copie agora: o segredo completo não poderá ser exibido novamente.");
      await refreshTokens();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Não foi possível criar o token.");
    } finally {
      setBusy(false);
    }
  }

  async function revoke(id: string) {
    if (!window.confirm("Revogar este token? A ação não pode ser desfeita.")) return;
    setError("");
    try {
      await revokeApiToken(id);
      await refreshTokens();
      setMessage("Token revogado.");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Não foi possível revogar o token.");
    }
  }

  async function inspect(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setInspection(null);
    try {
      setInspection(await inspectApiToken(inspectValue.trim()));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Não foi possível validar o token.");
    }
  }

  async function copy(value: string) {
    try {
      await navigator.clipboard.writeText(value);
      setMessage("Copiado para a área de transferência.");
    } catch {
      setError("A cópia falhou. Selecione e copie o valor manualmente.");
    }
  }

  if (!user) {
    return <div className="p-8 text-[13px] text-[var(--text-muted)]">{error || "Carregando..."}</div>;
  }
  if (user.role !== "ADMIN") {
    return (
      <div className="m-8 flex max-w-xl items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-5 text-amber-900">
        <ShieldAlert size={18} className="mt-0.5 shrink-0" />
        <div><h1 className="text-[14px] font-semibold">Acesso restrito</h1><p className="mt-1 text-[12px]">A gestão e inspeção de API tokens está disponível somente para Administradores.</p></div>
      </div>
    );
  }

  return (
    <div className="p-6 lg:p-8">
      <div className="mb-6">
        <p className="text-[11px] font-semibold uppercase tracking-wide text-[var(--brand)]">Administração</p>
        <h1 className="mt-1 text-2xl font-semibold">API tokens</h1>
        <p className="mt-1 text-[13px] text-[var(--text-secondary)]">Crie credenciais de integração com escopos e validade limitados.</p>
      </div>
      {(error || message) && <div role="status" className={`mb-4 rounded-md border px-3 py-2 text-[12px] ${error ? "border-red-200 bg-red-50 text-red-700" : "border-emerald-200 bg-emerald-50 text-emerald-800"}`}>{error || message}</div>}

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.1fr)_minmax(350px,0.9fr)]">
        <section className="rounded-xl border border-[var(--grid)] bg-white p-5">
          <div className="mb-4 flex items-center gap-2"><Plus size={16} className="text-[var(--brand)]" /><h2 className="text-[14px] font-semibold">Criar token</h2></div>
          <form onSubmit={createToken}>
            <label className="block text-[11px] font-medium">Nome do sistema consumidor
              <input value={name} onChange={(event) => setName(event.target.value)} required maxLength={120} placeholder="Ex.: ERP Financeiro" className="mt-1 w-full rounded-md border border-[var(--grid)] bg-transparent px-3 py-2 text-[12px] outline-none focus:border-[var(--brand)]" />
            </label>
            <fieldset className="mt-4">
              <legend className="text-[11px] font-medium">Escopos permitidos</legend>
              <div className="mt-2 grid gap-2 sm:grid-cols-2">
                {scopes.map((scope) => (
                  <label key={scope.id} className="flex items-center gap-2 rounded-md border border-[var(--grid)] px-3 py-2 text-[11px]">
                    <input type="checkbox" checked={selectedScopes.includes(scope.id)} onChange={() => toggleScope(scope.id)} className="accent-blue-600" />
                    {scope.label}
                  </label>
                ))}
              </div>
            </fieldset>
            <label className="mt-4 block text-[11px] font-medium">Expiração
              <select value={expiry} onChange={(event) => setExpiry(event.target.value)} className="mt-1 w-full rounded-md border border-[var(--grid)] bg-white px-3 py-2 text-[12px]">
                <option value="30">30 dias</option>
                <option value="90">90 dias</option>
                <option value="never">Sem expiração</option>
              </select>
            </label>
            <button disabled={busy} className="mt-4 rounded-md bg-[var(--brand)] px-4 py-2 text-[11px] font-medium text-white hover:opacity-90 disabled:opacity-60">
              {busy ? "Criando..." : "Gerar API token"}
            </button>
          </form>
          {secret && <div className="mt-4 rounded-lg border border-amber-300 bg-amber-50 p-4">
            <div className="flex items-center gap-2 text-[12px] font-semibold text-amber-900"><KeyRound size={14} /> Copie seu token agora</div>
            <p className="mt-1 text-[10px] text-amber-800">Ele será armazenado como hash e não poderá ser recuperado depois.</p>
            <div className="mt-3 flex gap-2">
              <input readOnly value={secret} className="min-w-0 flex-1 rounded border border-amber-300 bg-white px-2 py-2 font-mono text-[10px]" />
              <button type="button" onClick={() => void copy(secret)} className="flex items-center gap-1 rounded bg-amber-800 px-3 text-[10px] text-white"><Clipboard size={12} /> Copiar</button>
            </div>
            <button type="button" onClick={() => setSecret("")} className="mt-2 text-[10px] underline text-amber-900">Ocultar segredo</button>
          </div>}
        </section>

        <section className="rounded-xl border border-[var(--grid)] bg-white p-5">
          <div className="mb-4 flex items-center justify-between"><h2 className="text-[14px] font-semibold">Validar e inspecionar token</h2><ShieldAlert size={15} className="text-[var(--text-muted)]" /></div>
          <form onSubmit={inspect}>
            <label className="block text-[11px] font-medium">Token a validar
              <textarea rows={3} value={inspectValue} onChange={(event) => setInspectValue(event.target.value)} required placeholder="Cole o token completo..." className="mt-1 w-full rounded-md border border-[var(--grid)] bg-transparent p-3 font-mono text-[11px] outline-none focus:border-[var(--brand)]" />
            </label>
            <button className="mt-3 rounded-md border border-[var(--grid)] px-4 py-2 text-[11px] font-medium hover:bg-[var(--sidebar-hover)]">Validar token</button>
          </form>
          {inspection && <div className="mt-4 rounded-md border border-[var(--grid)] p-3 text-[11px]">
            <div className="flex items-center justify-between"><span className="font-semibold">{inspection.name}</span><span className={`rounded-full px-2 py-1 text-[10px] font-semibold ${inspection.status === "ACTIVE" ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700"}`}>{inspection.status}</span></div>
            <p className="mt-2 text-[var(--text-secondary)]">Proprietário: {inspection.owner}</p>
            <p className="mt-1 text-[var(--text-secondary)]">Criado: {dateLabel(inspection.createdAt)}</p>
            <p className="mt-1 text-[var(--text-secondary)]">Expira: {dateLabel(inspection.expiresAt)}</p>
            <p className="mt-1 text-[var(--text-secondary)]">Último uso: {dateLabel(inspection.lastUsedAt)}</p>
            <div className="mt-2 flex flex-wrap gap-1">{inspection.scopes.map((scope) => <span key={scope} className="rounded bg-violet-100 px-2 py-1 text-[9px] text-violet-700">{scope}</span>)}</div>
          </div>}
        </section>
      </div>

      <section className="mt-5 overflow-hidden rounded-xl border border-[var(--grid)] bg-white">
        <div className="flex items-center justify-between border-b border-[var(--grid)] px-5 py-4">
          <div><h2 className="text-[14px] font-semibold">Tokens da licença</h2><p className="mt-0.5 text-[11px] text-[var(--text-muted)]">O segredo completo não é armazenado nem exibido nesta lista.</p></div>
          <span className="rounded-full bg-[var(--sidebar-hover)] px-2.5 py-1 text-[10px]">{tokens.length} registros</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[780px] text-left text-[11px]">
            <thead className="bg-[var(--sidebar-hover)] text-[var(--text-muted)]"><tr>
              <th className="px-5 py-3 font-medium">Sistema</th><th className="px-4 py-3 font-medium">Prefixo</th><th className="px-4 py-3 font-medium">Criado</th><th className="px-4 py-3 font-medium">Último uso</th><th className="px-4 py-3 font-medium">Expiração</th><th className="px-4 py-3 font-medium">Status</th><th className="px-4 py-3 font-medium">Ação</th>
            </tr></thead>
            <tbody className="divide-y divide-[var(--grid)]">
              {tokens.map((item) => <tr key={item.id}>
                <td className="px-5 py-3 font-medium">{item.name}<div className="mt-1 flex flex-wrap gap-1">{item.scopes.slice(0, 3).map((scope) => <span key={scope} className="text-[9px] text-[var(--text-muted)]">{scope}</span>)}</div></td>
                <td className="px-4 py-3 font-mono">{item.tokenPrefix}</td><td className="px-4 py-3">{dateLabel(item.createdAt)}</td><td className="px-4 py-3">{dateLabel(item.lastUsedAt)}</td><td className="px-4 py-3">{dateLabel(item.expiresAt)}</td>
                <td className="px-4 py-3"><span className={`rounded-full px-2 py-1 text-[9px] font-semibold ${item.status === "ACTIVE" ? "bg-emerald-100 text-emerald-700" : item.status === "EXPIRED" ? "bg-amber-100 text-amber-800" : "bg-slate-100 text-slate-500"}`}>{item.status}</span></td>
                <td className="px-4 py-3">{item.status === "ACTIVE" && <button type="button" onClick={() => void revoke(item.id)} title="Revogar token" className="rounded p-1.5 text-red-600 hover:bg-red-50"><Trash2 size={14} /></button>}</td>
              </tr>)}
              {tokens.length === 0 && <tr><td colSpan={7} className="px-5 py-8 text-center text-[var(--text-muted)]">Nenhum token criado para esta licença.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>
      {message && <p role="status" className="mt-3 flex items-center gap-1 text-[11px] text-emerald-700"><Check size={12} /> {message}</p>}
    </div>
  );
}
