"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { login } from "@/lib/api";
import { setToken } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const session = await login(email, password);
      setToken(session.token);
      router.replace("/");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Não foi possível entrar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <form onSubmit={submit} className="w-full max-w-sm rounded-xl border border-[var(--grid)] bg-white p-8 shadow-sm">
        <div className="mb-6 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-[var(--brand)] font-bold text-white">D</div>
          <div>
            <h1 className="text-lg font-semibold">DOXA Developer Portal</h1>
            <p className="text-[12px] text-[var(--text-muted)]">Use sua conta do CRM</p>
          </div>
        </div>
        <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
          E-mail
          <input
            type="email"
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            className="mt-1 w-full rounded-md border border-[var(--grid)] bg-transparent px-3 py-2 text-[13px] outline-none focus:border-[var(--brand)]"
          />
        </label>
        <label className="mt-4 block text-[12px] font-medium text-[var(--text-secondary)]">
          Senha
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            className="mt-1 w-full rounded-md border border-[var(--grid)] bg-transparent px-3 py-2 text-[13px] outline-none focus:border-[var(--brand)]"
          />
        </label>
        {error && <p role="alert" className="mt-4 text-[12px] text-red-600">{error}</p>}
        <button
          disabled={busy}
          className="mt-6 w-full rounded-md bg-[var(--brand)] py-2.5 text-[13px] font-medium text-white hover:opacity-90 disabled:opacity-60"
        >
          {busy ? "Entrando..." : "Entrar"}
        </button>
        <p className="mt-4 text-[11px] text-[var(--text-muted)]">
          A gestão de tokens e a configuração MCP exigem perfil Admin.
        </p>
      </form>
    </div>
  );
}
