import styles from './ProductCard.module.css';

const CONFIDENCE_LABELS = {
  HIGH: { label: 'High confidence', cls: styles.confHigh },
  MEDIUM: { label: 'Medium confidence', cls: styles.confMedium },
  LOW: { label: 'Low confidence', cls: styles.confLow },
};

export default function ProductCard({ card, compareSelected = false, compareDisabled = false, onToggleCompare }) {
  const conf = CONFIDENCE_LABELS[card.confidence] ?? CONFIDENCE_LABELS.MEDIUM;

  return (
    <article className={[styles.card, compareSelected ? styles.cardSelected : ''].join(' ').trim()}>
      {onToggleCompare && (
        <label
          className={styles.compareToggle}
          title={compareDisabled && !compareSelected ? 'You can compare up to 4 picks at once' : undefined}
        >
          <input
            type="checkbox"
            checked={compareSelected}
            disabled={compareDisabled && !compareSelected}
            onChange={() => onToggleCompare(card)}
          />
          <span className={styles.compareCheck} aria-hidden="true" />
          <span className={styles.compareLabel}>Compare</span>
        </label>
      )}

      <header className={styles.header}>
        <div className={styles.category}>
          <span className={styles.emoji} aria-hidden="true">{card.emoji}</span>
          <span className={styles.catLabel}>{card.category}</span>
        </div>
        <span className={[styles.conf, conf.cls].join(' ')} title={conf.label}>
          {card.confidence}
        </span>
      </header>

      <div className={styles.productName}>{card.productName}</div>

      {card.price && (
        <div className={styles.price}>{card.price}</div>
      )}

      {card.keyReasons?.length > 0 && (
        <ul className={styles.reasons} aria-label="Key reasons">
          {card.keyReasons.map((reason, i) => (
            <li key={i}>{reason}</li>
          ))}
        </ul>
      )}

      {card.verdict && (
        <blockquote className={styles.verdict}>
          {card.verdict}
        </blockquote>
      )}

      {card.buyUrl && (
        <a
          href={card.buyUrl}
          target="_blank"
          rel="noopener noreferrer sponsored"
          className={styles.buyBtn}
        >
          Buy on Amazon ↗
        </a>
      )}

      <footer className={styles.footer}>
        <span className={styles.channel} title="Source channel">
          {card.channelName ?? 'Unknown channel'}
        </span>
        {card.videoUrl ? (
          <a
            href={card.videoUrl}
            target="_blank"
            rel="noopener noreferrer"
            className={styles.watchBtn}
          >
            Watch review ↗
          </a>
        ) : (
          <span className={styles.noLink}>No link</span>
        )}
      </footer>
    </article>
  );
}
