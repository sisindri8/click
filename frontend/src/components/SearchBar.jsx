import { useState, useRef } from 'react';
import styles from './SearchBar.module.css';

const EXAMPLES = [
  'best phones under 30k',
  'best laptop under 50k',
  'best earbuds under 2000',
  'best gaming phone under 25k',
  'best smartwatch under 10k',
];

export default function SearchBar({ onSearch, disabled }) {
  const [value, setValue] = useState('');
  const inputRef = useRef(null);

  function handleSubmit(e) {
    e.preventDefault();
    if (value.trim() && !disabled) onSearch(value.trim());
  }

  function setExample(q) {
    setValue(q);
    inputRef.current?.focus();
  }

  return (
    <div className={styles.root}>
      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.inputWrap}>
          <svg className={styles.icon} viewBox="0 0 20 20" fill="none" aria-hidden="true">
            <circle cx="9" cy="9" r="6" stroke="currentColor" strokeWidth="1.5"/>
            <path d="M15 15l-3-3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
          </svg>
          <input
            ref={inputRef}
            className={styles.input}
            type="text"
            value={value}
            onChange={e => setValue(e.target.value)}
            placeholder="best phones under 30k"
            disabled={disabled}
            autoComplete="off"
            aria-label="Search for tech products"
          />
        </div>
        <button
          type="submit"
          className={styles.btn}
          disabled={disabled || !value.trim()}
        >
          {disabled ? 'Searching…' : 'Search'}
        </button>
      </form>

      <div className={styles.examples} role="list" aria-label="Example searches">
        {EXAMPLES.map(q => (
          <button
            key={q}
            role="listitem"
            className={styles.chip}
            onClick={() => setExample(q)}
            disabled={disabled}
            type="button"
          >
            {q}
          </button>
        ))}
      </div>
    </div>
  );
}
