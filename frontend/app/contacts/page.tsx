"use client";

import { Search, Plus, Download } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import AuthGuard from "@/components/auth-guard";
import AddContactModal from "@/components/add-contact-modal";
import { listContacts } from "@/lib/api";
import type { Contact } from "@/lib/types";

const tagColors: Record<string, string> = {
  "ads-lead": "bg-blue-50 text-blue-700",
  "triage-done": "bg-emerald-50 text-emerald-700",
  qualified: "bg-violet-50 text-violet-700",
  "no-show": "bg-red-50 text-red-700",
  reconnect: "bg-amber-50 text-amber-700",
  hot: "bg-orange-50 text-orange-700",
};

export default function ContactsPage() {
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [total, setTotal] = useState(0);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const loadContacts = useCallback(async (query: string) => {
    setLoading(true);
    setError(null);
    try {
      const page = await listContacts({ q: query || undefined, size: 100 });
      setContacts(page.content);
      setTotal(page.totalElements);
    } catch {
      setError("Não foi possível carregar os contatos.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => loadContacts(search), search ? 300 : 0);
    return () => clearTimeout(timer);
  }, [search, loadContacts]);

  return (
    <AuthGuard>
      <div className="flex h-full flex-col bg-white">
        <div className="flex items-center gap-3 border-b border-[var(--hairline)] px-5 py-4">
          <h1 className="text-lg font-semibold">Contacts</h1>
          <span className="rounded-full bg-blue-50 px-2.5 py-1 text-[12px] font-medium text-[var(--brand)]">
            {total} contacts
          </span>
          <div className="ml-auto flex items-center gap-2">
            <div className="flex items-center gap-2 rounded-md border border-[var(--grid)] px-3 py-1.5">
              <Search size={13} className="text-[var(--text-muted)]" />
              <input
                placeholder="Search contacts"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-44 bg-transparent text-[12.5px] outline-none placeholder:text-[var(--text-muted)]"
              />
            </div>
            <button className="flex items-center gap-1.5 rounded-md border border-[var(--grid)] px-3 py-1.5 text-[13px] text-[var(--text-secondary)] hover:bg-neutral-50">
              <Download size={13} /> Import
            </button>
            <button
              type="button"
              onClick={() => setShowAddModal(true)}
              className="flex items-center gap-1.5 rounded-md bg-[var(--brand)] px-3 py-1.5 text-[13px] font-medium text-white hover:opacity-90"
            >
              <Plus size={14} /> Add contact
            </button>
          </div>
        </div>

        {toast && (
          <div className="border-b border-emerald-200 bg-emerald-50 px-5 py-2 text-[13px] text-emerald-800">
            {toast}
          </div>
        )}

        {error && (
          <div className="border-b border-red-200 bg-red-50 px-5 py-2 text-[13px] text-red-700">
            {error}
          </div>
        )}

        <div className="min-h-0 flex-1 overflow-auto">
          {loading ? (
            <p className="p-8 text-center text-[13px] text-[var(--text-muted)]">
              Carregando contatos...
            </p>
          ) : (
            <table className="w-full text-left text-[13px]">
              <thead className="sticky top-0 bg-[var(--page)] text-[11.5px] uppercase tracking-wide text-[var(--text-muted)]">
                <tr>
                  <th className="px-5 py-2.5 font-medium">Name</th>
                  <th className="px-5 py-2.5 font-medium">Email</th>
                  <th className="px-5 py-2.5 font-medium">Phone</th>
                  <th className="px-5 py-2.5 font-medium">Tags</th>
                  <th className="px-5 py-2.5 font-medium">Created</th>
                </tr>
              </thead>
              <tbody>
                {contacts.map((c) => (
                  <tr
                    key={c.id}
                    className="cursor-pointer border-b border-[var(--grid)] hover:bg-neutral-50"
                  >
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-2.5">
                        <span className="flex h-7 w-7 items-center justify-center rounded-full bg-blue-100 text-[11px] font-semibold text-blue-700">
                          {c.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}
                        </span>
                        <span className="font-medium">{c.name}</span>
                      </div>
                    </td>
                    <td className="px-5 py-3 text-[var(--text-secondary)]">{c.email}</td>
                    <td className="px-5 py-3 text-[var(--text-secondary)]">{c.phone}</td>
                    <td className="px-5 py-3">
                      <div className="flex flex-wrap gap-1">
                        {c.tags.map((t) => (
                          <span
                            key={t}
                            className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${tagColors[t] ?? "bg-neutral-100 text-neutral-700"}`}
                          >
                            {t}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-5 py-3 text-[var(--text-secondary)]">{c.created}</td>
                  </tr>
                ))}
                {contacts.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-5 py-8 text-center text-[var(--text-muted)]">
                      Nenhum contato encontrado.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {showAddModal && (
        <AddContactModal
          onClose={() => setShowAddModal(false)}
          onCreated={() => {
            setToast("Contato criado com sucesso.");
            loadContacts(search);
          }}
        />
      )}
    </AuthGuard>
  );
}
