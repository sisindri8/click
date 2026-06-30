import { PIPELINE_STEPS } from '../hooks/useSearch';
import styles from './PipelineProgress.module.css';

export default function PipelineProgress({ activeStep, doneSteps }) {
  return (
    <div className={styles.root} role="status" aria-live="polite" aria-label="Search progress">
      <div className={styles.label}>Analyzing reviews</div>
      <ol className={styles.steps}>
        {PIPELINE_STEPS.map(step => {
          const isDone = doneSteps.has(step.id);
          const isActive = activeStep === step.id;
          return (
            <li
              key={step.id}
              className={[
                styles.step,
                isActive ? styles.active : '',
                isDone ? styles.done : '',
              ].join(' ')}
            >
              <span className={styles.dot} aria-hidden="true">
                {isDone ? '✓' : isActive ? <span className={styles.spinner} /> : step.id}
              </span>
              <span className={styles.text}>{step.label}</span>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
