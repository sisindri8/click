import { useEffect, useRef } from 'react';
import styles from './CompareModal.module.css';

const CONFIDENCE_LABELS = {
  HIGH: { label: 'High confidence', cls: styles.confHigh },
  MEDIUM: { label: 'Medium confidence', cls: styles.confMedium },
  LOW: { label: 'Low confidence', cls: styles.confLow },
};

export default function CompareModal({ cards, onClose, onRemove }) {
  const dialogRef = useRef(null);

  // Close on Escape
  useEffect(() => {
    const handler = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onClose]);

  // Lock background scroll while open
  useEffect(() => {
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = prev; };
  }, []);

  if (!cards || cards.length === 0) return null;

  const maxReasons = Math.max(...cards.map(c => c.keyReasons?.length ?? 0), 0);

  return (
    <div
      className={styles.overlay}
      onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className={styles.dialog} role="dialog" aria-modal="true" aria-label="Compare picks" ref={dialogRef}>
        <header className={styles.header}>
          <h2 className={styles.title}>Compare picks</h2>
          <button type="button" className={styles.closeBtn} onClick={onClose} aria-label="Close comparison">
            ✕
          </button>
        </header>

        <div className={styles.scrollArea}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th className={styles.rowLabelCol} scope="col">
                  <span className={styles.cornerLabel}>{cards.length} picks</span>
                </th>
                {cards.map((card, i) => (
                  <th key={i} className={styles.productCol} scope="col">
                    <button
                      type="button"
                      className={styles.removeFromCompare}
                      onClick={() => onRemove(card)}
                      aria-label={`Remove ${card.productName} from comparison`}
                    >
                      ×
                    </button>
                    <div className={styles.colEmoji} aria-hidden="true">{card.emoji}</div>
                    <div className={styles.colCategory}>{card.category}</div>
                    <div className={styles.colProductName}>{card.productName}</div>
                  </th>
                ))}
              </tr>
            </thead>

            <tbody>
              <tr>
                <th scope="row" className={styles.rowLabel}>Price</th>
                {cards.map((card, i) => (
                  <td key={i} className={styles.priceCell}>{card.price ?? '—'}</td>
                ))}
              </tr>

              <tr>
                <th scope="row" className={styles.rowLabel}>Confidence</th>
                {cards.map((card, i) => {
                  const conf = CONFIDENCE_LABELS[card.confidence] ?? CONFIDENCE_LABELS.MEDIUM;
                  return (
                    <td key={i}>
                      <span className={[styles.conf, conf.cls].join(' ')} title={conf.label}>
                        {card.confidence ?? 'MEDIUM'}
                      </span>
                    </td>
                  );
                })}
              </tr>

              {Array.from({ length: maxReasons }).map((_, reasonIdx) => (
                <tr key={reasonIdx}>
                  <th scope="row" className={styles.rowLabel}>
                    {reasonIdx === 0 ? 'Why it won' : ''}
                  </th>
                  {cards.map((card, i) => (
                    <td key={i} className={styles.reasonCell}>
                      {card.keyReasons?.[reasonIdx] ? (
                        <span className={styles.reasonItem}>→ {card.keyReasons[reasonIdx]}</span>
                      ) : (
                        <span className={styles.reasonEmpty}>—</span>
                      )}
                    </td>
                  ))}
                </tr>
              ))}

              <tr>
                <th scope="row" className={styles.rowLabel}>Verdict</th>
                {cards.map((card, i) => (
                  <td key={i} className={styles.verdictCell}>
                    {card.verdict ? `“${card.verdict}”` : '—'}
                  </td>
                ))}
              </tr>

              <tr>
                <th scope="row" className={styles.rowLabel}>Source channel</th>
                {cards.map((card, i) => (
                  <td key={i} className={styles.channelCell}>{card.channelName ?? 'Unknown channel'}</td>
                ))}
              </tr>

              <tr>
                <th scope="row" className={styles.rowLabel}>Links</th>
                {cards.map((card, i) => (
                  <td key={i}>
                    <div className={styles.linkStack}>
                      {card.buyUrl && (
                        <a href={card.buyUrl} target="_blank" rel="noopener noreferrer sponsored" className={styles.buyBtn}>
                          Buy ↗
                        </a>
                      )}
                      {card.videoUrl ? (
                        <a href={card.videoUrl} target="_blank" rel="noopener noreferrer" className={styles.watchBtn}>
                          Watch ↗
                        </a>
                      ) : (
                        <span className={styles.noLink}>No video link</span>
                      )}
                    </div>
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
