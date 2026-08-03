"use client";

import { useState } from "react";
import { ApiError, createContact } from "@/lib/api";

type Props = {
  onClose: () => void;
  onCreated: () => void;
};

function parseError(err: unknown) {
  if (err instanceof ApiError) {
    try {
      const json = JSON.parse(err.message) as { detail?: string };
      return json.detail ?? err.message;
    } catch {
      return err.message;
    }
  }
  return "Não foi possível criar o contato.";
}

export default function AddContactModal({ onClose, onCreated }: Props) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [tags, setTags] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const tagList = tags
        .split(",")
        .map((t) => t.trim())
        .filter(Boolean);
      await createContact({
        name,
        email: email || undefined,
        phone: phone || undefined,
        tags: tagList.length ? tagList : undefined,
      });
      onCreated();
      onClose();
    } catch (err) {
      setError(parseError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4">
      <div className="w-full max-w-md rounded-lg bg-white p-5 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-[15px] font-semibold">Add contact</h2>
          <button type="button" onClick={onClose} className="text-[13px] text-[var(--text-muted)]">
            Fechar
          </button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-3">
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Nome *
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            E-mail
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Telefone
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="+14075551234"
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          <p className="text-[11px] text-[var(--text-muted)]">
            Informe e-mail ou telefone (pelo menos um).
          </p>
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Tags (separadas por vírgula)
            <input
              value={tags}
              onChange={(e) => setTags(e.target.value)}
              placeholder="ads-lead, hot"
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          {error && <p className="text-[12px] text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading || (!email && !phone)}
            className="w-full rounded bg-[var(--brand)] py-2 text-[13px] font-medium text-white disabled:opacity-60"
          >
            {loading ? "Salvando..." : "Criar contato"}
          </button>
        </form>
      </div>
    </div>
  );
}
