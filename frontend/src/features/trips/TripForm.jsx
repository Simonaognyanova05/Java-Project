import { useEffect, useRef, useState } from 'react';
import Button from '../../components/ui/Button';
import FormField from '../../components/ui/FormField';
import Modal from '../../components/ui/Modal';
import { createTrip, getTrip, updateTrip } from '../../services/tripApi';
import { toLocalDateTimeInput, userTimeZone } from '../../utils/dateFormat';
import styles from './TripsPage.module.css';

const blank = { origin: '', destination: '', departureAt: '', expectedArrivalAt: '', vehicleId: '' };

export default function TripForm({ tripId, onClose, onSaved, onReloadList }) {
  const editing = tripId != null;
  const [form, setForm] = useState(blank);
  const [version, setVersion] = useState(null);
  const [loading, setLoading] = useState(editing);
  const [loadError, setLoadError] = useState('');
  const [attempt, setAttempt] = useState(0);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [errors, setErrors] = useState({});
  const [conflict, setConflict] = useState(false);
  const submitting = useRef(false);
  const firstField = useRef(null);

  useEffect(() => {
    if (!editing) return undefined;
    const controller = new AbortController();
    let active = true;
    setLoading(true);
    setLoadError('');
    getTrip(tripId, { signal: controller.signal })
      .then((trip) => {
        if (!active) return;
        setForm({
          origin: trip.origin ?? '', destination: trip.destination ?? '',
          departureAt: toLocalDateTimeInput(trip.departureAt),
          expectedArrivalAt: toLocalDateTimeInput(trip.expectedArrivalAt),
          vehicleId: trip.vehicleId == null ? '' : String(trip.vehicleId),
        });
        setVersion(trip.version);
        setError('');
        setErrors({});
        setConflict(false);
      })
      .catch((failure) => {
        if (active && failure.name !== 'AbortError') {
          setLoadError(failure.status === 404 ? 'Курсът не е намерен или е деактивиран.' : failure.message);
        }
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [tripId, editing, attempt]);

  useEffect(() => { if (!loading && !loadError) firstField.current?.focus(); }, [loading, loadError]);

  const change = ({ target: { name, value } }) => {
    setForm((current) => ({ ...current, [name]: value }));
    setErrors((current) => ({ ...current, [name]: undefined }));
    if (!conflict) setError('');
  };

  const submit = async (event) => {
    event.preventDefault();
    if (submitting.current || conflict) return;
    const invalid = {};
    const origin = form.origin.trim();
    const destination = form.destination.trim();
    const departure = new Date(form.departureAt);
    const arrival = new Date(form.expectedArrivalAt);
    const vehicleId = Number(form.vehicleId);
    if (!origin) invalid.origin = 'Въведете начална точка.';
    if (!destination) invalid.destination = 'Въведете крайна точка.';
    if (!form.departureAt || Number.isNaN(departure.getTime())) invalid.departureAt = 'Посочете дата и час.';
    if (!form.expectedArrivalAt || Number.isNaN(arrival.getTime())) invalid.expectedArrivalAt = 'Посочете дата и час.';
    else if (arrival <= departure) invalid.expectedArrivalAt = 'Пристигането трябва да е след тръгването.';
    if (!Number.isSafeInteger(vehicleId) || vehicleId <= 0) invalid.vehicleId = 'Въведете валидно положително ID.';
    setErrors(invalid);
    setError('');
    if (Object.keys(invalid).length) return;
    if (editing && (!Number.isSafeInteger(version) || version < 0)) {
      setConflict(true);
      setError('Липсва актуална версия на курса. Заредете данните отново.');
      return;
    }
    const payload = {
      origin, destination, departureAt: departure.toISOString(),
      expectedArrivalAt: arrival.toISOString(), vehicleId,
      ...(editing ? { expectedVersion: version } : {}),
    };
    submitting.current = true;
    setSaving(true);
    try {
      const trip = editing ? await updateTrip(tripId, payload) : await createTrip(payload);
      onSaved(trip, editing);
    } catch (failure) {
      if (failure.status === 409) {
        setConflict(true);
        setError('Конфликт: курсът е променен или превозното средство няма достатъчен капацитет. Заредете актуалните данни и проверете избраното превозно средство.');
      } else {
        setError(failure.status === 404
          ? 'Курсът или активното превозно средство не е намерено. Проверете ID и актуалните данни.'
          : failure.message);
        if (failure.fieldErrors && !Array.isArray(failure.fieldErrors)) {
          setErrors(Object.fromEntries(Object.entries(failure.fieldErrors)
            .filter(([key, value]) => key in blank && typeof value === 'string')));
        }
      }
    } finally {
      submitting.current = false;
      setSaving(false);
    }
  };

  return (
    <Modal open title={editing ? 'Редакция на курс' : 'Нов курс'}
      description={'Полетата със * са задължителни. Часова зона: ' + userTimeZone}
      onClose={() => { if (!submitting.current) onClose(); }} closeOnBackdrop={!saving}>
      {loading ? <p role="status">Зареждане на курса…</p> : loadError ? (
        <div role="alert" className={styles.error}><p>{loadError}</p>
          <Button variant="secondary" onClick={() => setAttempt((value) => value + 1)}>Опитай отново</Button>
          <Button variant="ghost" onClick={onReloadList}>Обнови списъка</Button>
        </div>
      ) : (
        <form onSubmit={submit} aria-label="Данни за курса">
          <FormField ref={firstField} data-modal-initial-focus label="Начална точка" name="origin"
            value={form.origin} onChange={change} maxLength={255} error={errors.origin} disabled={saving} required />
          <FormField label="Крайна точка" name="destination" value={form.destination} onChange={change}
            maxLength={255} error={errors.destination} disabled={saving} required />
          <FormField label="Тръгване" name="departureAt" type="datetime-local" step="0.001"
            value={form.departureAt} onChange={change} error={errors.departureAt} disabled={saving} required />
          <FormField label="Очаквано пристигане" name="expectedArrivalAt" type="datetime-local" step="0.001"
            value={form.expectedArrivalAt} onChange={change} error={errors.expectedArrivalAt} disabled={saving} required />
          <FormField label="Превозно средство (ID)" name="vehicleId" type="number" min="1" step="1"
            value={form.vehicleId} onChange={change} error={errors.vehicleId} disabled={saving} required
            hint="Въведете ID на съществуващо активно превозно средство." />
          {error && <p role="alert" className={styles.error}>{error}</p>}
          {conflict && <div className={styles.recovery}>
            {editing ? <><p>Презареждането ще замени въведените стойности с актуалния запис.</p>
              <Button variant="secondary" onClick={() => setAttempt((value) => value + 1)}>Зареди актуалните данни</Button></>
              : <Button variant="secondary" onClick={() => { setConflict(false); setError(''); }}>Провери данните отново</Button>}
          </div>}
          <div className={styles.formActions}>
            <Button variant="secondary" onClick={onClose} disabled={saving}>Отказ</Button>
            <Button type="submit" loading={saving} loadingText="Запазване…" disabled={conflict}>
              {editing ? 'Запази промените' : 'Създай курс'}
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
