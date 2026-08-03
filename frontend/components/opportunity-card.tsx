"use client";

import { useState } from "react";
import {
  Phone,
  MessageSquare,
  Tag,
  FileText,
  CheckSquare,
  CalendarDays,
} from "lucide-react";
import type { Opportunity } from "@/lib/types";
import OpportunityActionModal, { type ModalKind } from "@/components/opportunity-action-modal";

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-1 text-[11.5px] leading-5">
      <span className="shrink-0 font-medium text-[var(--text-secondary)]">{label}:</span>
      <span className="truncate text-[var(--text-muted)]">{value}</span>
    </div>
  );
}

function ActionButton({
  title,
  onClick,
  children,
}: {
  title: string;
  onClick: (e: React.MouseEvent) => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      title={title}
      onClick={onClick}
      className="text-[var(--text-muted)] hover:text-[var(--brand)]"
    >
      {children}
    </button>
  );
}

export default function OpportunityCard({ opp }: { opp: Opportunity }) {
  const [modal, setModal] = useState<ModalKind>(null);
  const [toast, setToast] = useState<string | null>(null);

  function stop(e: React.MouseEvent) {
    e.stopPropagation();
  }

  function openWhatsApp(e: React.MouseEvent) {
    stop(e);
    const url = opp.whatsAppUrl;
    if (!url) {
      setToast("Telefone indisponível para WhatsApp.");
      return;
    }
    window.open(url, "_blank", "noopener,noreferrer");
  }

  function openPhone(e: React.MouseEvent) {
    stop(e);
    const tel = opp.phoneE164 || opp.phone;
    if (!tel) {
      setToast("Telefone indisponível.");
      return;
    }
    window.location.href = `tel:${tel}`;
  }

  return (
    <>
      <article className="relative cursor-pointer rounded-md border border-[var(--grid)] bg-white p-3 shadow-sm transition-shadow hover:shadow-md">
        <h3 className="text-[13px] font-semibold text-[var(--text-primary)]">{opp.name}</h3>
        <div className="mt-1.5">
          {opp.adSet && <Row label="Ad Set" value={opp.adSet} />}
          {opp.revenueMonthly && <Row label="Revenue Monthly" value={opp.revenueMonthly} />}
          <Row label="Created on" value={opp.createdOn} />
          {opp.hasWrapUp && (
            <p className="mt-1 text-[10px] font-medium text-emerald-600">Wrap-up registrado</p>
          )}
          {opp.callScore != null && (
            <p className="text-[10px] font-medium text-[var(--brand)]">
              Call score: {opp.callScore}/5
            </p>
          )}
        </div>
        <div className="mt-2 flex items-center gap-2.5 border-t border-[var(--grid)] pt-2">
          <ActionButton title="Ligar" onClick={openPhone}>
            <Phone size={13} />
          </ActionButton>
          <ActionButton title="WhatsApp" onClick={openWhatsApp}>
            <MessageSquare size={13} />
          </ActionButton>
          <ActionButton
            title="Enviar e-mail"
            onClick={(e) => {
              stop(e);
              setModal("email");
            }}
          >
            <Tag size={13} />
          </ActionButton>
          <ActionButton
            title="Análise Fathom"
            onClick={(e) => {
              stop(e);
              setModal("fathom");
            }}
          >
            <FileText size={13} />
          </ActionButton>
          <ActionButton
            title="Wrap-up pós-call"
            onClick={(e) => {
              stop(e);
              setModal("wrapUp");
            }}
          >
            <CheckSquare size={13} />
          </ActionButton>
          <ActionButton
            title="Convite Google Calendar"
            onClick={(e) => {
              stop(e);
              setModal("calendar");
            }}
          >
            <CalendarDays size={13} />
          </ActionButton>
        </div>
        {toast && (
          <p className="mt-2 text-[11px] text-amber-700" onAnimationEnd={() => setToast(null)}>
            {toast}
          </p>
        )}
      </article>

      {modal && (
        <OpportunityActionModal
          opp={opp}
          kind={modal}
          onClose={() => setModal(null)}
          onSuccess={(message) => setToast(message)}
        />
      )}
    </>
  );
}
