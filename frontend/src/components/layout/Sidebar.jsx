import { useEffect, useRef } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { navigationItems } from '../../app/navigation';
import NavigationIcon from './NavigationIcon';
import styles from './AppLayout.module.css';

export default function Sidebar({ isMobile, open, onClose }) {
  const sidebarRef = useRef(null);
  const closeRef = useRef(null);

  useEffect(() => {
    if (!isMobile || !open) return undefined;
    const previouslyFocused = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    closeRef.current?.focus();
    return () => {
      document.body.style.overflow = previousOverflow;
      previouslyFocused?.focus();
    };
  }, [isMobile, open]);

  const handleKeyDown = (event) => {
    if (!isMobile || !open) return;
    if (event.key === 'Escape') {
      event.preventDefault();
      onClose();
    }
    if (event.key === 'Tab') {
      const controls = sidebarRef.current.querySelectorAll('a[href], button:not([disabled])');
      const first = controls[0];
      const last = controls[controls.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
  };

  return (
    <aside id="primary-sidebar" ref={sidebarRef} className={styles.sidebar} hidden={isMobile && !open}
      role={isMobile && open ? 'dialog' : undefined} aria-modal={isMobile && open ? true : undefined}
      aria-label={isMobile ? 'Основно меню' : 'Работно пространство'} onKeyDown={handleKeyDown}>
      <div className={styles.brandRow}>
        <Link to="/dashboard" className={styles.brand} onClick={isMobile ? onClose : undefined}>
          <span className={styles.brandMark}><NavigationIcon name="shipments" /></span>
          <span>Smart<span className={styles.brandAccent}>Shipment</span><small>TRANSPORT MANAGEMENT</small></span>
        </Link>
        {isMobile && <button ref={closeRef} type="button" className={styles.iconButton} aria-label="Затвори навигацията" onClick={onClose}><NavigationIcon name="close" /></button>}
      </div>
      <p className={styles.navLabel}>РАБОТНО ПРОСТРАНСТВО</p>
      <nav aria-label="Основна навигация" className={styles.navigation}>
        {navigationItems.map(({ path, label, icon }) => (
          <NavLink key={path} to={path} end={path === '/dashboard'} onClick={isMobile ? onClose : undefined} className={({ isActive }) => `${styles.navItem} ${isActive ? styles.navActive : ''}`}>
            <NavigationIcon name={icon} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
      <div className={styles.sidebarFooter}>
        <span className={styles.footerMark}><NavigationIcon name="vehicles" /></span>
        <div><strong>Контрол на транспорта</strong><small>Пратки · Курсове · Екипаж</small></div>
      </div>
    </aside>
  );
}
