import styles from './CompareBar.module.css';

const MAX_COMPARE = 4;

export default function CompareBar({ selected, onRemove, onClear, onCompare }) {
  if (selected.length === 0) return null;

  return (
    <div className={styles.bar} role="region" aria-label="Comparison tray">
      <div className={styles.inner}>
        <div className={styles.chips}>
          {selected.map((card, i) => (
            <span key={i} className={styles.chip}>
              <span className={styles.chipEmoji} aria-hidden="true">{card.emoji}</span>
              {card.productName}
              <button
                type="button"
                className={styles.chipRemove}
                onClick={() => onRemove(card)}
                aria-label={`Remove ${card.productName} from comparison`}
              >
                ×
              </button>
            </span>
          ))}
          {Array.from({ length: MAX_COMPARE - selected.length }).map((_, i) => (
            <span key={`empty-${i}`} className={styles.chipEmpty} aria-hidden="true" />
          ))}
        </div>

        <div className={styles.actions}>
          <button type="button" className={styles.clearBtn} onClick={onClear}>
            Clear
          </button>
          <button
            type="button"
            className={styles.compareBtn}
            onClick={onCompare}
            disabled={selected.length < 2}
          >
            Compare {selected.length}/{MAX_COMPARE}
          </button>
        </div>
      </div>
    </div>
  );
}
