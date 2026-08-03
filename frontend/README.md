# DOXA CRM — Frontend

Next.js 16 (App Router) conectado à API real em `http://localhost:8080`.

## Rodar

```bash
npm install
npm run dev
```

Opcional — `.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Páginas

- `/login` — autenticação JWT
- `/` — Dashboard (KPIs e gráficos da API)
- `/opportunities` — Kanban/lista, busca, criar oportunidade, integrações nos cards
- `/contacts` — tabela, busca, criar contato

Setup completo (banco + backend + frontend): veja o [README na raiz do projeto](../README.md).
