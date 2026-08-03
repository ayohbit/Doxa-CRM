"use client";

import { useEffect, useState } from "react";
import { ApiError, createOpportunity, listContacts } from "@/lib/api";
import type { Contact, Stage } from "@/lib/types";

type Props = {
  stages: Stage[];
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
  return "Não foi possível criar a oportunidade.";
}

export default function AddOpportunityModal({ stages, onClose, onCreated }: Props) {
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [contactId, setContactId] = useState("");
  const [stageSlug, setStageSlug] = useState("new-lead");
  const [value, setValue] = useState("");
  const [adSet, setAdSet] = useState("");
  const [revenueMonthly, setRevenueMonthly] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listContacts({ size: 200 })
      .then((page) => {
        setContacts(page.content);
        if (page.content.length > 0) {
          setContactId(page.content[0].id);
        }
      })
      .catch(() => setError("Não foi possível carregar contatos."));
  }, []);

  useEffect(() => {
    if (stages.length > 0 && !stages.some((s) => s.id === stageSlug)) {
      setStageSlug(stages[0].id);
    }
  }, [stages, stageSlug]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!contactId) {
      setError("Selecione um contato.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await createOpportunity({
        contactId,
        stageSlug,
        value: value ? Number(value) : undefined,
        adSet: adSet || undefined,
        revenueMonthly: revenueMonthly || undefined,
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
          <h2 className="text-[15px] font-semibold">Add opportunity</h2>
          <button type="button" onClick={onClose} className="text-[13px] text-[var(--text-muted)]">
            Fechar
          </button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-3">
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Contato *
            <select
              required
              value={contactId}
              onChange={(e) => setContactId(e.target.value)}
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            >
              {contacts.length === 0 && <option value="">Nenhum contato — crie um em Contacts</option>}
              {contacts.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} {c.email ? `(${c.email})` : ""}
                </option>
              ))}
            </select>
          </label>
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Estágio inicial *
            <select
              required
              value={stageSlug}
              onChange={(e) => setStageSlug(e.target.value)}
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            >
              {stages.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Valor (USD)
            <input
              type="number"
              min={0}
              step="0.01"
              value={value}
              onChange={(e) => setValue(e.target.value)}
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Ad Set
            <input
              value={adSet}
              onChange={(e) => setAdSet(e.target.value)}
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Revenue Monthly
            <input
              value={revenueMonthly}
              onChange={(e) => setRevenueMonthly(e.target.value)}
              placeholder="$10k - $25k/mo"
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          {error && <p className="text-[12px] text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading || !contactId}
            className="w-full rounded bg-[var(--brand)] py-2 text-[13px] font-medium text-white disabled:opacity-60"
          >
            {loading ? "Salvando..." : "Criar oportunidade"}
          </button>
        </form>
      </div>
    </div>
  );
}
