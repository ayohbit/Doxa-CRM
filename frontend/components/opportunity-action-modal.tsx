"use client";

import { useEffect, useState } from "react";
import {
  getGoogleAuthUrl,
  getIntegrationStatus,
  sendCalendarInvite,
  sendContactEmail,
  submitCallAnalysis,
  submitWrapUp,
} from "@/lib/api";
import type { Opportunity } from "@/lib/types";

type ModalKind = "wrapUp" | "fathom" | "calendar" | "email" | null;

type Props = {
  opp: Opportunity;
  kind: ModalKind;
  onClose: () => void;
  onSuccess: (message: string) => void;
};

function ModalShell({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4">
      <div className="w-full max-w-md rounded-lg bg-white p-5 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-[15px] font-semibold text-[var(--text-primary)]">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-[13px] text-[var(--text-muted)] hover:text-[var(--text-primary)]"
          >
            Fechar
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export default function OpportunityActionModal({ opp, kind, onClose, onSuccess }: Props) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [googleConnected, setGoogleConnected] = useState(false);
  const [googleConfigured, setGoogleConfigured] = useState(false);

  const [outcome, setOutcome] = useState("vai pensar");
  const [objection, setObjection] = useState("");
  const [nextStep, setNextStep] = useState("");
  const [fathomUrl, setFathomUrl] = useState("");
  const [startAt, setStartAt] = useState("");
  const [duration, setDuration] = useState("30");
  const [emailSubject, setEmailSubject] = useState(`Follow-up — ${opp.name}`);
  const [emailBody, setEmailBody] = useState("");

  useEffect(() => {
    if (kind === "calendar" || kind === "email") {
      getIntegrationStatus()
        .then((status) => {
          setGoogleConnected(status.googleConnected);
          setGoogleConfigured(status.googleConfigured);
        })
        .catch(() => setError("Não foi possível verificar integrações Google."));
    }
  }, [kind]);

  if (!kind) return null;

  async function connectGoogle() {
    try {
      const { authUrl } = await getGoogleAuthUrl();
      window.location.href = authUrl;
    } catch {
      setError("Google OAuth não está configurado no servidor.");
    }
  }

  async function handleWrapUp(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await submitWrapUp(opp.id, { outcome, objection, nextStep });
      onSuccess("Wrap-up salvo.");
      onClose();
    } catch {
      setError("Falha ao salvar wrap-up.");
    } finally {
      setLoading(false);
    }
  }

  async function handleFathom(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const result = await submitCallAnalysis(opp.id, fathomUrl);
      onSuccess(`Análise concluída — score ${result.aiScore}/5.`);
      onClose();
    } catch {
      setError("Falha na análise Fathom.");
    } finally {
      setLoading(false);
    }
  }

  async function handleCalendar(e: React.FormEvent) {
    e.preventDefault();
    if (!googleConnected) {
      setError("Conecte sua conta Google primeiro.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const result = await sendCalendarInvite(opp.id, {
        startAt: new Date(startAt).toISOString(),
        durationMinutes: Number(duration),
        title: `Meeting with ${opp.name}`,
      });
      onSuccess(result.message);
      onClose();
    } catch {
      setError("Falha ao criar convite no Google Calendar.");
    } finally {
      setLoading(false);
    }
  }

  async function handleEmail(e: React.FormEvent) {
    e.preventDefault();
    if (!googleConnected) {
      setError("Conecte sua conta Google primeiro.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const result = await sendContactEmail(opp.contactId, {
        subject: emailSubject,
        body: emailBody,
      });
      onSuccess(result.message);
      onClose();
    } catch {
      setError("Falha ao enviar e-mail.");
    } finally {
      setLoading(false);
    }
  }

  if (kind === "wrapUp") {
    return (
      <ModalShell title="Wrap-up pós-call" onClose={onClose}>
        <form onSubmit={handleWrapUp} className="space-y-3">
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Resultado
            <select
              value={outcome}
              onChange={(e) => setOutcome(e.target.value)}
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            >
              <option value="fechou">Fechou</option>
              <option value="vai pensar">Vai pensar</option>
              <option value="não é fit">Não é fit</option>
              <option value="remarcou">Remarcou</option>
            </select>
          </label>
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Principal objeção
            <textarea
              value={objection}
              onChange={(e) => setObjection(e.target.value)}
              rows={2}
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Próximo passo
            <textarea
              value={nextStep}
              onChange={(e) => setNextStep(e.target.value)}
              rows={2}
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          {error && <p className="text-[12px] text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded bg-[var(--brand)] py-2 text-[13px] font-medium text-white disabled:opacity-60"
          >
            {loading ? "Salvando..." : "Salvar wrap-up"}
          </button>
        </form>
      </ModalShell>
    );
  }

  if (kind === "fathom") {
    return (
      <ModalShell title="Análise Fathom" onClose={onClose}>
        <form onSubmit={handleFathom} className="space-y-3">
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            URL da gravação Fathom
            <input
              value={fathomUrl}
              onChange={(e) => setFathomUrl(e.target.value)}
              placeholder="https://fathom.video/share/..."
              required
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          {opp.callScore != null && (
            <p className="text-[12px] text-[var(--text-muted)]">
              Score atual: {opp.callScore}/5
            </p>
          )}
          {error && <p className="text-[12px] text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded bg-[var(--brand)] py-2 text-[13px] font-medium text-white disabled:opacity-60"
          >
            {loading ? "Analisando..." : "Rodar análise"}
          </button>
        </form>
      </ModalShell>
    );
  }

  if (kind === "calendar") {
    return (
      <ModalShell title="Convite Google Calendar" onClose={onClose}>
        {!googleConnected && googleConfigured && (
          <button
            type="button"
            onClick={connectGoogle}
            className="mb-3 w-full rounded border border-[var(--brand)] py-2 text-[13px] font-medium text-[var(--brand)]"
          >
            Conectar Google
          </button>
        )}
        {!googleConfigured && (
          <p className="mb-3 text-[12px] text-[var(--text-muted)]">
            Configure GOOGLE_CLIENT_ID e GOOGLE_CLIENT_SECRET no backend.
          </p>
        )}
        <form onSubmit={handleCalendar} className="space-y-3">
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Data e hora
            <input
              type="datetime-local"
              value={startAt}
              onChange={(e) => setStartAt(e.target.value)}
              required
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Duração (min)
            <input
              type="number"
              min={15}
              value={duration}
              onChange={(e) => setDuration(e.target.value)}
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          <p className="text-[11px] text-[var(--text-muted)]">
            Convite será enviado para {opp.email || "e-mail do contato"}.
          </p>
          {error && <p className="text-[12px] text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading || !googleConnected}
            className="w-full rounded bg-[var(--brand)] py-2 text-[13px] font-medium text-white disabled:opacity-60"
          >
            {loading ? "Criando..." : "Enviar convite"}
          </button>
        </form>
      </ModalShell>
    );
  }

  if (kind === "email") {
    return (
      <ModalShell title="Enviar e-mail (Gmail)" onClose={onClose}>
        {!googleConnected && googleConfigured && (
          <button
            type="button"
            onClick={connectGoogle}
            className="mb-3 w-full rounded border border-[var(--brand)] py-2 text-[13px] font-medium text-[var(--brand)]"
          >
            Conectar Google
          </button>
        )}
        <form onSubmit={handleEmail} className="space-y-3">
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Assunto
            <input
              value={emailSubject}
              onChange={(e) => setEmailSubject(e.target.value)}
              required
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          <label className="block text-[12px] font-medium text-[var(--text-secondary)]">
            Mensagem
            <textarea
              value={emailBody}
              onChange={(e) => setEmailBody(e.target.value)}
              rows={4}
              required
              className="mt-1 w-full rounded border border-[var(--grid)] px-2 py-1.5 text-[13px]"
            />
          </label>
          {error && <p className="text-[12px] text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading || !googleConnected}
            className="w-full rounded bg-[var(--brand)] py-2 text-[13px] font-medium text-white disabled:opacity-60"
          >
            {loading ? "Enviando..." : "Enviar e-mail"}
          </button>
        </form>
      </ModalShell>
    );
  }

  return null;
}

export type { ModalKind };
