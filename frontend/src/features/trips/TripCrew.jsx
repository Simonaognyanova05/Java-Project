import { useEffect, useRef, useState } from 'react';
import Button from '../../components/ui/Button';
import DataTable from '../../components/ui/DataTable';
import FormField from '../../components/ui/FormField';
import Modal from '../../components/ui/Modal';
import { addTripEmployee, getTrip, removeTripEmployee } from '../../services/tripApi';
import styles from './TripsPage.module.css';

export default function TripCrew({ trip, onUpdated, onBusyChange }) {
  const [employeeId, setEmployeeId] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [operation, setOperation] = useState('');
  const [removing, setRemoving] = useState(null);
  const [stale, setStale] = useState(false);
  const pending = useRef(false);
  const mounted = useRef(true);
  const readController = useRef(null);
  const inputRef = useRef(null);
  const restoreInputFocus = useRef(false);
  const refreshButton = useRef(null);
  const employeeIds = trip.employeeIds ?? [];

  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; readController.current?.abort(); };
  }, []);

  useEffect(() => {
    if (!operation && restoreInputFocus.current) {
      restoreInputFocus.current = false;
      (stale ? refreshButton.current : inputRef.current)?.focus();
    }
  }, [operation, stale]);

  const start = (name) => {
    if (pending.current) return false;
    pending.current = true;
    setOperation(name);
    setError('');
    setNotice('');
    onBusyChange(true);
    return true;
  };
  const finish = () => {
    pending.current = false;
    if (mounted.current) { setOperation(''); onBusyChange(false); }
  };
  const readCurrentTrip = async () => {
    readController.current?.abort();
    readController.current = new AbortController();
    const updated = await getTrip(trip.id, { signal: readController.current.signal });
    if (mounted.current) { onUpdated(updated); setStale(false); }
  };

  const refresh = async () => {
    if (!start('refresh')) return;
    try { await readCurrentTrip(); }
    catch (failure) {
      if (mounted.current && failure.name !== 'AbortError') {
        setStale(true);
        setError(failure.status === 404 ? 'Курсът не е намерен или е деактивиран. Върнете се към курсовете.' : failure.message);
      }
    } finally { finish(); }
  };

  const add = async (event) => {
    event.preventDefault();
    if (pending.current || stale) return;
    const id = Number(employeeId);
    if (!Number.isSafeInteger(id) || id <= 0) { setFieldError('Въведете валидно положително ID.'); return; }
    if (employeeIds.some((existing) => Number(existing) === id)) {
      setFieldError('Служителят вече е назначен към курса.');
      return;
    }
    if (!start('add')) return;
    setFieldError('');
    try {
      const updated = await addTripEmployee(trip.id, id);
      if (mounted.current) {
        onUpdated(updated);
        setEmployeeId('');
        setNotice('Служителят е добавен към екипажа.');
      }
    } catch (failure) {
      if (mounted.current) {
        setError(failure.status === 404
          ? 'Курсът или активният служител не е намерен. Проверете ID или обновете данните.'
          : failure.status === 409 ? 'Данните са променени. Обновете екипажа преди нов опит.' : failure.message);
        if (failure.status === 409) setStale(true);
      }
    } finally { finish(); }
  };

  const remove = async () => {
    if (stale || !start('remove')) return;
    try {
      await removeTripEmployee(trip.id, removing);
      if (!mounted.current) return;
      setRemoving(null);
      setNotice('Служителят е премахнат от екипажа.');
      restoreInputFocus.current = true;
      try { await readCurrentTrip(); }
      catch (failure) {
        if (mounted.current && failure.name !== 'AbortError') {
          setStale(true);
          setError('Премахването е успешно, но актуалният екипаж не може да се зареди. Обновете данните.');
        }
      }
    } catch (failure) {
      if (mounted.current) {
        setError(failure.status === 404
          ? 'Курсът или служителят не е намерен. Обновете екипажа.'
          : failure.status === 409 ? 'Конфликт при промяната. Обновете екипажа.' : failure.message);
        setRemoving(null);
        restoreInputFocus.current = true;
        if (failure.status === 404 || failure.status === 409) setStale(true);
      }
    } finally { finish(); }
  };

  const busy = !!operation;
  return (
    <section className={styles.crewPanel} aria-labelledby="trip-crew-title">
      <div className={styles.heading}>
        <div><h2 id="trip-crew-title">Екипаж на курса</h2><p>Назначени служители: {employeeIds.length}</p></div>
        <Button ref={refreshButton} variant="secondary" onClick={refresh} loading={operation === 'refresh'} disabled={busy}>Обнови екипажа</Button>
      </div>
      <form onSubmit={add} className={styles.crewForm} aria-label="Добавяне на служител">
        <FormField ref={inputRef} label="Служител (ID)" name="employeeId" type="number" min="1" step="1"
          value={employeeId} onChange={(event) => { setEmployeeId(event.target.value); setFieldError(''); }}
          error={fieldError} hint="Въведете ID на съществуващ активен служител."
          required disabled={busy || stale} />
        <Button type="submit" loading={operation === 'add'} loadingText="Добавяне…" disabled={busy || stale}>Добави към екипажа</Button>
      </form>
      {notice && <p role="status" className={styles.notice}>{notice}</p>}
      {error && <p role="alert" className={styles.error}>{error}</p>}
      {stale && <p className={styles.recovery}>Показаните данни може да не са актуални. Натиснете „Обнови екипажа“, за да продължите.</p>}
      <DataTable caption="Назначени служители" rows={employeeIds.map((id) => ({ id }))}
        emptyMessage="Няма назначени служители."
        columns={[
          { key: 'id', header: 'ID на служител' },
          { key: 'actions', header: 'Действия', render: (employee) =>
            <Button variant="danger" aria-label={'Премахни служител #' + employee.id}
              disabled={busy || stale} onClick={() => { setError(''); setRemoving(employee.id); }}>Премахни</Button> },
        ]} />
      {removing != null && <Modal open title="Премахване от екипажа"
        description={'Да премахнем ли служител #' + removing + ' от курс #' + trip.id + '?'}
        onClose={() => { if (!pending.current) setRemoving(null); }} closeOnBackdrop={!busy}
        footer={<>
          <Button variant="secondary" data-modal-initial-focus disabled={busy} onClick={() => setRemoving(null)}>Отказ</Button>
          <Button variant="danger" onClick={remove} loading={operation === 'remove'} loadingText="Премахване…" disabled={busy}>Премахни от курса</Button>
        </>}>
        <p>Служителят ще бъде отстранен само от този курс. Профилът му се запазва.</p>
      </Modal>}
    </section>
  );
}
