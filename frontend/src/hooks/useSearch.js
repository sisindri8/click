import { useState, useCallback, useRef } from 'react';
import { search as searchApi } from '../api/search';

// The 5 pipeline steps shown to the user while the API runs
export const PIPELINE_STEPS = [
  { id: 1, label: 'Expanding your query into 5 variations' },
  { id: 2, label: 'Searching 5 trusted Telugu channels' },
  { id: 3, label: 'Fetching video transcripts via Whisper' },
  { id: 4, label: 'Extracting products with Claude AI' },
  { id: 5, label: 'Curating your top picks' },
];

// How long each step "feels" active before advancing to the next.
// The real API call may finish before or after - step 5 stays active
// until the response arrives.
const STEP_DURATIONS = [700, 1100, 1400, 900, 0];

export function useSearch() {
  const [status, setStatus] = useState('idle'); // idle | loading | success | error
  const [activeStep, setActiveStep] = useState(0);
  const [doneSteps, setDoneSteps] = useState(new Set());
  const [results, setResults] = useState(null);
  const [error, setError] = useState(null);
  const abortRef = useRef(null);

  const runSearch = useCallback(async (query) => {
    if (!query.trim()) return;

    // Reset state
    setStatus('loading');
    setActiveStep(1);
    setDoneSteps(new Set());
    setResults(null);
    setError(null);

    // Animate through steps 1-4 while API runs in parallel
    const stepTimer = (async () => {
      for (let i = 0; i < 4; i++) {
        await delay(STEP_DURATIONS[i]);
        setDoneSteps(prev => new Set([...prev, i + 1]));
        setActiveStep(i + 2);
      }
    })();

    try {
      const [data] = await Promise.all([
        searchApi(query),
        stepTimer,
      ]);

      // Finish final step
      setDoneSteps(new Set([1, 2, 3, 4, 5]));
      setActiveStep(0);
      await delay(300);

      setResults(data);
      setStatus('success');
    } catch (err) {
      setError(err.message ?? 'Something went wrong. Make sure all services are running.');
      setStatus('error');
      setActiveStep(0);
      setDoneSteps(new Set());
    }
  }, []);

  return { status, activeStep, doneSteps, results, error, runSearch };
}

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
