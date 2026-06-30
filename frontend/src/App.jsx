import { useState, useMemo } from 'react';
import { useSearch } from './hooks/useSearch';
import AmbientBackground from './components/AmbientBackground';
import SearchBar from './components/SearchBar';
import PipelineProgress from './components/PipelineProgress';
import ProductCard from './components/ProductCard';
import VideoList from './components/VideoList';
import CompareBar from './components/CompareBar';
import CompareModal from './components/CompareModal';
import styles from './App.module.css';

const MAX_COMPARE = 4;

export default function App() {
  const { status, activeStep, doneSteps, results, error, runSearch } = useSearch();
  const [compareList, setCompareList] = useState([]);
  const [compareOpen, setCompareOpen] = useState(false);

  const isLoading = status === 'loading';
  const hasResults = status === 'success' && results;

  // Identify cards by category + productName since the API doesn't send IDs
  const cardKey = (card) => `${card.category}__${card.productName}`;

  const toggleCompare = (card) => {
    setCompareList((prev) => {
      const exists = prev.some((c) => cardKey(c) === cardKey(card));
      if (exists) return prev.filter((c) => cardKey(c) !== cardKey(card));
      if (prev.length >= MAX_COMPARE) return prev;
      return [...prev, card];
    });
  };

  const removeFromCompare = (card) => {
    setCompareList((prev) => prev.filter((c) => cardKey(c) !== cardKey(card)));
  };

  const clearCompare = () => setCompareList([]);

  const compareKeySet = useMemo(
    () => new Set(compareList.map(cardKey)),
    [compareList]
  );

  return (
    <>
      <AmbientBackground />

      <div className={styles.page}>

        {/* Header */}
        <header className={styles.header}>
          <div className={styles.logo}>
            click<span className={styles.logoDot}>.</span>
          </div>
          <div className={styles.tagline}>
            Powered by trusted Telugu tech reviews
          </div>
        </header>

        {/* Hero */}
        <section className={styles.hero}>
          <div className={styles.eyebrow}>AI-curated from YouTube</div>
          <h1 className={styles.headline}>
            Find the best tech,<br />
            <span className={styles.gradientText}>trusted by real reviewers</span>
          </h1>
          <p className={styles.sub}>
            Search across 5 trusted Telugu tech channels. Get curated picks — not ads.
          </p>

          <SearchBar onSearch={runSearch} disabled={isLoading} />
        </section>

        {/* Pipeline progress */}
        {isLoading && (
          <div className={styles.progressWrap}>
            <PipelineProgress activeStep={activeStep} doneSteps={doneSteps} />
          </div>
        )}

        {/* Error state */}
        {status === 'error' && (
          <div className={styles.errorBanner} role="alert">
            <span className={styles.errorIcon}>⚠</span>
            <div>
              <strong>Search failed</strong>
              <p>{error}</p>
            </div>
          </div>
        )}

        {/* Results */}
        {hasResults && (
          <section className={styles.results}>
            <div className={styles.resultsHeader}>
              <h2 className={styles.resultsTitle}>
                {results.curatedPicks?.length > 0
                  ? `Top picks for "${results.originalQuery}"`
                  : `Results for "${results.originalQuery}"`}
              </h2>
              <span className={styles.resultsMeta}>
                {results.totalUniqueVideosFound ?? 0} videos analyzed
                {results.detectedCategory && ` · ${results.detectedCategory}`}
              </span>
            </div>

            {results.curatedPicks?.length > 0 ? (
              <div className={styles.cardsGrid}>
                {results.curatedPicks.map((card, i) => (
                  <ProductCard
                    key={i}
                    card={card}
                    compareSelected={compareKeySet.has(cardKey(card))}
                    compareDisabled={compareList.length >= MAX_COMPARE}
                    onToggleCompare={results.curatedPicks.length > 1 ? toggleCompare : undefined}
                  />
                ))}
              </div>
            ) : (
              <div className={styles.emptyState}>
                <div className={styles.emptyIcon}>🔍</div>
                <h3>No picks found</h3>
                <p>
                  Found {results.totalUniqueVideosFound ?? 0} videos but couldn't
                  extract product recommendations. Try a more specific query like
                  "best phones under 30k".
                </p>
              </div>
            )}

            <VideoList videos={results.videos} />
          </section>
        )}

        {/* Footer */}
        <footer className={styles.footer}>
          <p>click. · Built with Spring Boot, Claude AI & Whisper · Telugu tech reviews only</p>
        </footer>

      </div>

      <CompareBar
        selected={compareList}
        onRemove={removeFromCompare}
        onClear={clearCompare}
        onCompare={() => setCompareOpen(true)}
      />

      {compareOpen && (
        <CompareModal
          cards={compareList}
          onClose={() => setCompareOpen(false)}
          onRemove={removeFromCompare}
        />
      )}
    </>
  );
}
