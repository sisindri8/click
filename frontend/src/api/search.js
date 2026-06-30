// All API calls go through Vite's dev proxy (/api → localhost:9090)
// In production, set VITE_API_BASE to your deployed Gateway URL
const API_BASE = import.meta.env.VITE_API_BASE ?? '';

export async function search(query) {
  const res = await fetch(`${API_BASE}/api/v1/search`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query }),
  });

  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error(error.message ?? `Server error ${res.status}`);
  }

  return res.json();
}
