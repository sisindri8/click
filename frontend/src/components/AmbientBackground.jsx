import styles from './AmbientBackground.module.css';

export default function AmbientBackground() {
  return (
    <div className={styles.root} aria-hidden="true">
      <div className={styles.orb1} />
      <div className={styles.orb2} />
      <div className={styles.orb3} />
    </div>
  );
}
