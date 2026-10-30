import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Button from '../../components/ui/Button';
import DataTable from '../../components/ui/DataTable';
import { getTrips } from '../../services/tripApi';
import { formatDateTime, userTimeZone } from '../../utils/dateFormat';
import TripForm from './TripForm';
import DeactivateTripDialog from './DeactivateTripDialog';
import styles from './TripsPage.module.css';

export default function TripsPage() {
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [editor, setEditor] = useState(null);
  const [deactivating, setDeactivating] = useState(null);
  const [notice, setNotice] = useState('');
  const addButton = useRef(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    setLoading(true);
    setError('');
    getTrips({ signal: controller.signal })
      .then((data) => { if (active) setTrips(data); })
      .catch((failure) => { if (active && failure.name !== 'AbortError') setError(failure.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [revision]);

  const reload = () => {
    setEditor(null);
    setDeactivating(null);
    setRevision((value) => value + 1);
  };
  const columns = [
    { key: 'id', header: 'Курс', render: (trip) => <Link className={styles.link} to={'/trips/' + trip.id}>Курс #{trip.id}</Link> },
    { key: 'route', header: 'Маршрут', render: (trip) => <strong>{trip.origin} → {trip.destination}</strong> },
    { key: 'departureAt', header: 'Тръгване', render: (trip) => formatDateTime(trip.departureAt) },
    { key: 'expectedArrivalAt', header: 'Очаквано пристигане', render: (trip) => formatDateTime(trip.expectedArrivalAt) },
    { key: 'vehicleId', header: 'Превозно средство', render: (trip) => trip.vehicleId == null ? 'Не е зададено' : 'ID: ' + trip.vehicleId },
    { key: 'actions', header: 'Действия', render: (trip) => <div className={styles.rowActions}>
      <Button variant="secondary" aria-label={'Редактирай курс #' + trip.id}
        onClick={() => { setNotice(''); setEditor({ id: trip.id }); }}>Редактирай</Button>
      <Button variant="danger" aria-label={'Деактивирай курс #' + trip.id}
        onClick={() => { setNotice(''); setDeactivating(trip); }}>Деактивирай</Button>
    </div> },
  ];
  return (
    <section>
      <div className={styles.heading}>
        <div><span className={styles.eyebrow}>УПРАВЛЕНИЕ</span><h1>Курсове</h1>
          <p>Активни курсове и транспортни маршрути.</p><p>Часова зона: {userTimeZone}</p></div>
        <div className={styles.rowActions}>
          <Button variant="secondary" onClick={reload} disabled={loading}>Обнови</Button>
          <Button ref={addButton} disabled={loading || !!error} onClick={() => { setNotice(''); setEditor({ id: null }); }}>Нов курс</Button>
        </div>
      </div>
      {notice && <p role="status" className={styles.notice}>{notice}</p>}
      <DataTable caption="Активни курсове" columns={columns} rows={trips} loading={loading} error={error}
        onRetry={reload} emptyMessage="Няма активни курсове. Добавете първия курс." />
      {editor && <TripForm key={editor.id ?? 'new'} tripId={editor.id} onClose={() => setEditor(null)} onReloadList={reload}
        onSaved={(trip, editing) => {
          setTrips((current) => editing ? current.map((row) => row.id === trip.id ? trip : row) : [...current, trip]);
          setNotice(editing ? 'Промените по курса са запазени.' : 'Курсът е създаден.');
          setEditor(null);
        }} />}
      {deactivating && <DeactivateTripDialog trip={deactivating} onClose={() => setDeactivating(null)} onReload={reload}
        onDeactivated={(id) => {
          setTrips((current) => current.filter((trip) => trip.id !== id));
          setDeactivating(null);
          setNotice('Курсът е деактивиран.');
          addButton.current?.focus();
        }} />}
    </section>
  );
}
