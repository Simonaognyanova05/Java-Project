import styles from './RoutePlaceholder.module.css';

export default function RoutePlaceholder({ title, description, children }) {
  return (
    <section className={styles.panel}>
      <span className={styles.tag}>В подготовка</span>
      <h1>{title}</h1>
      <p>{description}</p>
      {children && <div className={styles.actions}>{children}</div>}
    </section>
  );
}
