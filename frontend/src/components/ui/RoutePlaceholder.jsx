import { NavLink } from 'react-router-dom';
import { navigationItems } from '../../app/navigation';
import styles from './RoutePlaceholder.module.css';

export default function RoutePlaceholder({ title, description, children }) {
  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <span className={styles.brand}>Smart Shipment</span>
        <span className={styles.tag}>В подготовка</span>
      </header>
      <nav className={styles.navigation} aria-label="Основна навигация">
        {navigationItems.map(({ path, label }) => (
          <NavLink key={path} to={path} className={({ isActive }) => isActive ? styles.active : undefined}>
            {label}
          </NavLink>
        ))}
      </nav>
      <section className={styles.panel}>
        <h1>{title}</h1>
        <p>{description}</p>
        {children && <div className={styles.actions}>{children}</div>}
      </section>
    </main>
  );
}
