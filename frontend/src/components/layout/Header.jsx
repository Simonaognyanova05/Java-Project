import { Link } from 'react-router-dom';
import NavigationIcon from './NavigationIcon';
import styles from './AppLayout.module.css';

export default function Header({ metadata, isMobile, menuOpen, onToggleMenu, menuButtonRef }) {
  return (
    <header className={styles.header}>
      {isMobile && <button ref={menuButtonRef} type="button" className={styles.iconButton} aria-label="Отвори навигацията" aria-controls="primary-sidebar" aria-expanded={menuOpen} onClick={onToggleMenu}><NavigationIcon name="menu" /></button>}
      <nav aria-label="Път до текущата страница" className={styles.breadcrumbs}>
        <Link to={metadata.parent}>{metadata.section}</Link>
        <span aria-hidden="true">/</span>
        <span aria-current="page">{metadata.title}</span>
      </nav>
      <span className={styles.headerLabel}>ДИСПЕЧЕРСКИ ЦЕНТЪР</span>
    </header>
  );
}
