import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom';
import CreateShipmentPage from './features/shipments/CreateShipmentPage';
import './styles/tokens.css';
import './styles/global.css';

function NotFoundPage() {
  return (
    <main style={{ maxWidth: 560, margin: '18vh auto', padding: 24, textAlign: 'center' }}>
      <p style={{ color: 'var(--color-primary)', letterSpacing: 2, fontSize: 12 }}>404 · НЕ Е НАМЕРЕНА</p>
      <h1>Тази страница не е налична</h1>
      <p style={{ color: 'var(--color-text-muted)' }}>Върнете се към създаването на пратка.</p>
      <Link to="/shipments/new" style={{ color: 'var(--color-primary)' }}>Създаване на пратка →</Link>
    </main>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/shipments/new" replace />} />
        <Route path="/shipments/new" element={<CreateShipmentPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
