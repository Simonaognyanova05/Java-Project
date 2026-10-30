import { useRef, useState } from 'react';
import Button from '../../components/ui/Button';
import Modal from '../../components/ui/Modal';
import { deactivateTrip } from '../../services/tripApi';
import styles from './TripsPage.module.css';

export default function DeactivateTripDialog({ trip, onClose, onDeactivated, onReload }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [missing, setMissing] = useState(false);
  const pending = useRef(false);

  const deactivate = async () => {
    if (pending.current) return;
    pending.current = true;
    setBusy(true);
    setError('');
    try {
      await deactivateTrip(trip.id);
      onDeactivated(trip.id);
    } catch (failure) {
      setMissing(failure.status === 404);
      setError(failure.status === 409
        ? 'Курсът има недоставени пратки и не може да бъде деактивиран.'
        : failure.status === 404 ? 'Курсът вече не е активен. Обновете списъка.' : failure.message);
    } finally { pending.current = false; setBusy(false); }
  };

  return (
    <Modal open title="Деактивиране на курс"
      description={'Курс #' + trip.id + ': ' + trip.origin + ' → ' + trip.destination}
      onClose={() => { if (!pending.current) onClose(); }} closeOnBackdrop={!busy}
      footer={<>
        {missing && <Button variant="secondary" onClick={onReload}>Обнови списъка</Button>}
        <Button variant="secondary" data-modal-initial-focus onClick={onClose} disabled={busy}>Отказ</Button>
        <Button variant="danger" onClick={deactivate} loading={busy} loadingText="Деактивиране…" disabled={missing}>Деактивирай курса</Button>
      </>}>
      <p>Курсът ще бъде премахнат от активния списък. Данните се запазват в системата.</p>
      <p>Курс с недоставени пратки не може да бъде деактивиран.</p>
      {error && <p role="alert" className={styles.error}>{error}</p>}
    </Modal>
  );
}
