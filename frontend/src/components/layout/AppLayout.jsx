import { useEffect, useRef, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { getPageMetadata } from '../../app/pageMetadata';
import useMediaQuery from '../../hooks/useMediaQuery';
import Sidebar from './Sidebar';
import Header from './Header';
import styles from './AppLayout.module.css';

export default function AppLayout() {
  const { pathname } = useLocation();
  const isMobile = useMediaQuery('(max-width: 767px)');
  const [menuOpen, setMenuOpen] = useState(false);
  const menuButtonRef = useRef(null);
  const metadata = getPageMetadata(pathname);

  useEffect(() => { setMenuOpen(false); }, [pathname, isMobile]);
  useEffect(() => { document.title = `${metadata.title} · Smart Shipment Tracking`; }, [metadata.title]);

  return (
    <div className={styles.shell}>
      <a href="#main-content" className={styles.skipLink} tabIndex={isMobile && menuOpen ? -1 : undefined}>Към съдържанието</a>
      <Sidebar isMobile={isMobile} open={menuOpen} onClose={() => setMenuOpen(false)} />
      {isMobile && menuOpen && <button type="button" className={styles.backdrop} tabIndex={-1} aria-hidden="true" onClick={() => setMenuOpen(false)} />}
      <div className={styles.contentColumn} inert={isMobile && menuOpen ? true : undefined}>
        <Header metadata={metadata} isMobile={isMobile} menuOpen={menuOpen} menuButtonRef={menuButtonRef} onToggleMenu={() => setMenuOpen((current) => !current)} />
        <main id="main-content" className={styles.workspace} tabIndex={-1}><Outlet /></main>
        <footer className={styles.footer}><span>Smart Shipment Tracking</span><span>Управление на международни превози</span></footer>
      </div>
    </div>
  );
}
