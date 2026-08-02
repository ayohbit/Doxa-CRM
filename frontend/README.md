# Spreed CRM

CRM demo com dados mockados (estilo GoHighLevel), construído com Next.js 16 (App Router), Tailwind CSS 4, Recharts e lucide-react.

## Páginas

- `/` — **Dashboard**: faixa de KPIs do funil (Ad Spend → Leads → Triage → SC Booked → SC Shown → Closes), tiles de Cash Collected / Revenue / ROAS, gráfico diário de Spend · CC · Revenue e gráfico de drop-off do funil.
- `/opportunities` — **Kanban de oportunidades** do "Ads Pipeline" com 10 estágios, cards com Ad Set, Revenue Monthly, data de criação e ações rápidas.
- `/contacts` — **Tabela de contatos** com tags e busca.

## Rodando

```bash
npm install
npm run dev      # http://localhost:3000
```

## Dados

Todos os dados são mockados e determinísticos em `lib/data.ts` — edite lá para mudar estágios, oportunidades, KPIs e séries dos gráficos.
