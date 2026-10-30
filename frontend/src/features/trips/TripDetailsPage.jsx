import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import Button from '../../components/ui/Button';
import { getTrip } from '../../services/tripApi';
import { formatDateTime, userTimeZone } from '../../utils/dateFormat';
import TripForm from './TripForm';
import TripCrew from './TripCrew';
import DeactivateTripDialog from './DeactivateTripDialog';
import styles from './TripsPage.module.css';

export default function TripDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [trip, setTrip] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [editing, setEditing] = useState(false);
  const [deactivating, setDeactivating] = useState(false);
  const [notice, setNotice] = useState('');
  const [crewBusy, setCrewBusy] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    setLoading(true);
    setError('');
    setTrip(null);
    setEditing(false);
    setDeactivating(false);
    setNotice('');
    setCrewBusy(false);
    getTrip(id, { signal: controller.signal })
      .then((data) => { if (active) setTrip(data); })
      .catch((failure) => {
        if (active && failure.name !== 'AbortError') setError(
          failure.status === 404 ? 'Курсът не е намерен или е деактивиран.' : failure.message);
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [id, revision]);

  const reload = () => { setEditing(false); setDeactivating(false); setRevision((value) => value + 1); };
  return (
    <section>
      <div className={styles.heading}><div><span className={styles.eyebrow}>КУРСОВЕ</span><h1>Курс #{id}</h1></div>
        <Link className={styles.link} to="/trips">← Към курсовете</Link></div>
      {loading ? <p role="status">Зареждане на курса…</p> : error ? (
        <div role="alert" className={styles.error}><p>{error}</p>
          <Button variant="secondary" onClick={reload}>Опитай отново</Button>
        </div>
      ) : trip && <>
        {notice && <p role="status" className={styles.notice}>{notice}</p>}
        <div className={styles.detailPanel}>
          <h2>{trip.origin} → {trip.destination}</h2>
          <p className={styles.muted}>Часова зона: {userTimeZone}</p>
          <dl className={styles.details}>
            <div><dt>Тръгване</dt><dd>{formatDateTime(trip.departureAt)}</dd></div>
            <div><dt>Очаквано пристигане</dt><dd>{formatDateTime(trip.expectedArrivalAt)}</dd></div>
            <div><dt>Превозно средство</dt><dd>{trip.vehicleId == null ? 'Не е зададено' : 'ID: ' + trip.vehicleId}</dd></div>
            <div><dt>Екипаж (ID)</dt><dd>{trip.employeeIds?.length ? trip.employeeIds.join(', ') : 'Няма назначен екипаж'}</dd></div>
          </dl>
          <div className={styles.rowActions}>
            <Button variant="secondary" disabled={crewBusy} onClick={() => setEditing(true)}>Редактирай курса</Button>
            <Button variant="danger" disabled={crewBusy} onClick={() => setDeactivating(true)}>Деактивирай</Button>
            <Button variant="ghost" disabled={crewBusy} onClick={reload}>Обнови</Button>
          </div>
        </div>
        <TripCrew key={trip.id} trip={trip} onUpdated={setTrip} onBusyChange={setCrewBusy} />
        {editing && <TripForm tripId={trip.id} onClose={() => setEditing(false)} onReloadList={() => navigate('/trips')}
          onSaved={(updated) => { setTrip(updated); setEditing(false); setNotice('Промените по курса са запазени.'); }} />}
        {deactivating && <DeactivateTripDialog trip={trip} onClose={() => setDeactivating(false)}
          onReload={() => navigate('/trips')} onDeactivated={() => navigate('/trips', { replace: true })} />}
      </>}
    </section>
  );
}
