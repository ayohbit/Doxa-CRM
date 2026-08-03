"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { login } from "@/lib/api";
import { setToken } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("admin@demo.doxa.com");
  const [password, setPassword] = useState("password123");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const response = await login(email, password);
      setToken(response.token);
      router.push("/opportunities");
      router.refresh();
    } catch {
      setError("Email ou senha inválidos.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--page)] p-6">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-xl border border-[var(--grid)] bg-white p-8 shadow-sm"
      >
        <h1 className="text-xl font-semibold">DOXA CRM</h1>
        <p className="mt-1 text-[13px] text-[var(--text-secondary)]">Entre com sua conta</p>

        <label className="mt-6 block text-[12px] font-medium text-[var(--text-secondary)]">
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="mt-1 w-full rounded-md border border-[var(--grid)] px-3 py-2 text-[13px] outline-none focus:border-[var(--brand)]"
            required
          />
        </label>

        <label className="mt-4 block text-[12px] font-medium text-[var(--text-secondary)]">
          Senha
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 w-full rounded-md border border-[var(--grid)] px-3 py-2 text-[13px] outline-none focus:border-[var(--brand)]"
            required
          />
        </label>

        {error && <p className="mt-4 text-[12px] text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={loading}
          className="mt-6 w-full rounded-md bg-[var(--brand)] py-2.5 text-[13px] font-medium text-white hover:opacity-90 disabled:opacity-60"
        >
          {loading ? "Entrando..." : "Entrar"}
        </button>

        <p className="mt-4 text-[11px] text-[var(--text-muted)]">
          Demo: admin@demo.doxa.com / password123
        </p>
      </form>
    </div>
  );
}
