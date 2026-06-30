import styles from './VideoList.module.css';

function formatDuration(seconds) {
  if (!seconds) return '';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function formatAge(iso) {
  if (!iso) return '';
  const days = Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000);
  if (days < 7) return `${days}d ago`;
  if (days < 30) return `${Math.floor(days / 7)}w ago`;
  if (days < 365) return `${Math.floor(days / 30)}mo ago`;
  return `${Math.floor(days / 365)}y ago`;
}

export default function VideoList({ videos }) {
  if (!videos?.length) return null;

  return (
    <section className={styles.root}>
      <h2 className={styles.heading}>Videos analyzed</h2>
      <ul className={styles.list}>
        {videos.map(v => (
          <li key={v.videoId}>
            <a
              href={v.url}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.item}
            >
              {v.thumbnailUrl && (
                <img
                  src={v.thumbnailUrl}
                  alt=""
                  className={styles.thumb}
                  loading="lazy"
                  width={88}
                  height={49}
                />
              )}
              <div className={styles.info}>
                <div className={styles.title}>{v.title}</div>
                <div className={styles.meta}>
                  <span>{v.channelName}</span>
                  {v.durationSeconds && <span>· {formatDuration(v.durationSeconds)}</span>}
                  {v.publishedAt && <span>· {formatAge(v.publishedAt)}</span>}
                </div>
              </div>
              <span className={styles.score} title="Relevance score">
                ↑{v.rankScore}
              </span>
            </a>
          </li>
        ))}
      </ul>
    </section>
  );
}
