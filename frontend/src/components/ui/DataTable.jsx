import Button from './Button';
import styles from './DataTable.module.css';

export default function DataTable({
  columns, rows = [], rowKey = 'id', caption = 'Данни', loading = false,
  error, onRetry, emptyMessage = 'Няма налични записи.'
}) {
  if (loading) return <div className={styles.state} role="status">Зареждане…</div>;
  if (error) return <div className={styles.state} role="alert"><p>{error}</p>{onRetry && <Button variant="secondary" onClick={onRetry}>Опитай отново</Button>}</div>;
  const keyFor = typeof rowKey === 'function' ? rowKey : (row) => row[rowKey];
  return (
    <div className={styles.wrapper} role="region" aria-label={caption} tabIndex={0}>
      <table className={styles.table}>
        <caption>{caption}</caption>
        <thead><tr>{columns.map((column) => <th key={column.key} scope="col"
          className={column.align === 'right' ? styles.numeric : undefined}>{column.header}</th>)}</tr></thead>
        <tbody>
          {rows.length === 0
            ? <tr><td colSpan={columns.length} className={styles.empty}>{emptyMessage}</td></tr>
            : rows.map((row) => <tr key={keyFor(row)}>{columns.map((column) =>
              <td key={column.key} className={column.align === 'right' ? styles.numeric : undefined}>
                {column.render ? column.render(row) : (row[column.key] ?? '—')}
              </td>)}</tr>)}
        </tbody>
      </table>
    </div>
  );
}
