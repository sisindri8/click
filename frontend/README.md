# Click Frontend

React + Vite single-page app for Click — search bar, live pipeline progress,
and curated product cards.

## Stack
- React 18 (no router — single page)
- Vite (dev server + build)
- CSS Modules (no Tailwind/UI library — hand-built design system)
- Plain `fetch` for API calls (no axios/react-query)

## Run locally (development)

Requires Node.js 18+ and the backend services running (see root README).

```bash
cd frontend
npm install
npm run dev
```

Opens at **http://localhost:3000**. Vite's dev server proxies all `/api/*`
requests to `http://localhost:9090` (your local API Gateway) — see
`vite.config.js` if your Gateway runs on a different port.

## Run via Docker Compose (recommended)

From the project root:
```bash
docker-compose up --build
```

Frontend will be available at **http://localhost:3000**, served by nginx,
which proxies `/api/*` to the `api-gateway` container internally — no CORS
configuration needed.

## Project structure

```
src/
├── api/
│   └── search.js          — fetch wrapper for POST /api/v1/search
├── components/
│   ├── AmbientBackground   — decorative gradient orbs
│   ├── SearchBar           — input + example query chips
│   ├── PipelineProgress    — live 5-step search progress
│   ├── ProductCard         — one curated pick
│   └── VideoList           — analyzed videos list
├── hooks/
│   └── useSearch.js        — search state machine (idle/loading/success/error)
├── styles/
│   └── global.css          — design tokens (colors, type, spacing)
├── App.jsx                 — page layout, wires everything together
└── main.jsx                — React entry point
```

## Design system

Dark theme with violet→pink gradient accents (`#7C4DFF` → `#E040FB`),
Space Grotesk for display type, Inter for body text, JetBrains Mono for
prices/scores. All tokens are CSS custom properties in `src/styles/global.css`
— change a color there and it updates everywhere.

## Build for production

```bash
npm run build
```
Outputs static files to `dist/`. Can be served by any static host (nginx,
Vercel, Netlify) — just make sure `/api/*` requests are proxied or
`VITE_API_BASE` is set to your deployed Gateway URL at build time:

```bash
VITE_API_BASE=https://your-gateway-url.com npm run build
```
