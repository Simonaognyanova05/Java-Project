import { useEffect, useRef, useState } from 'react';
import Button from '../../components/ui/Button';
import FormField from '../../components/ui/FormField';
import Modal from '../../components/ui/Modal';
import { createCustomer, getCustomer, updateCustomer } from '../../services/customerApi';
import styles from './CustomersPage.module.css';

const emptyForm = { firstName: '', lastName: '', email: '', phone: '' };

export default function CustomerForm({ customerId, onClose, onSaved, onReload }) {
  const editing = customerId != null;
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(editing);
  const [loadError, setLoadError] = useState('');
  const [attempt, setAttempt] = useState(0);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [missing, setMissing] = useState(false);
  const submitting = useRef(false);
  const firstInput = useRef(null);

  useEffect(() => {
    if (!editing) return undefined;
    const controller = new AbortController();
    let active = true;
    setLoading(true);
    setLoadError('');
    setMissing(false);
    getCustomer(customerId, { signal: controller.signal })
      .then((customer) => {
        if (active) setForm({
          firstName: customer.firstName ?? '', lastName: customer.lastName ?? '',
          email: customer.email ?? '', phone: customer.phone ?? '',
        });
      })
      .catch((failure) => {
        if (active && failure.name !== 'AbortError') {
          setLoadError(failure.status === 404 ? 'Клиентът вече не съществува.' : failure.message);
          setMissing(failure.status === 404);
        }
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [customerId, editing, attempt]);

  useEffect(() => {
    if (!loading && !loadError) firstInput.current?.focus();
  }, [loading, loadError]);

  const change = ({ target: { name, value } }) => {
    setForm((current) => ({ ...current, [name]: value }));
    setFieldErrors((current) => ({ ...current, [name]: undefined }));
    setError('');
  };

  const submit = async (event) => {
    event.preventDefault();
    if (submitting.current) return;
    const payload = Object.fromEntries(Object.entries(form).map(([key, value]) => [key, value.trim()]));
    const invalid = {};
    if (!payload.firstName) invalid.firstName = 'Въведете име.';
    if (!payload.lastName) invalid.lastName = 'Въведете фамилия.';
    if (!payload.email) invalid.email = 'Въведете имейл.';
    setFieldErrors(invalid);
    setError('');
    if (Object.keys(invalid).length) return;
    submitting.current = true;
    setSaving(true);
    try {
      const customer = editing ? await updateCustomer(customerId, payload) : await createCustomer(payload);
      onSaved(customer, editing);
    } catch (failure) {
      if (failure.status === 409) {
        setError('Вече съществува клиент с този имейл. Проверете адреса.');
        setFieldErrors({ email: 'Този имейл вече се използва.' });
      } else {
        setError(failure.status === 404 ? 'Клиентът вече не съществува. Обновете списъка.' : failure.message);
        setMissing(failure.status === 404);
        if (failure.fieldErrors && !Array.isArray(failure.fieldErrors)) {
          setFieldErrors(Object.fromEntries(Object.entries(failure.fieldErrors)
            .filter(([key, value]) => key in emptyForm && typeof value === 'string')));
        }
      }
    } finally {
      submitting.current = false;
      setSaving(false);
    }
  };

  return (
    <Modal open title={editing ? 'Редакция на клиент' : 'Нов клиент'}
      description="Полетата със * са задължителни."
      onClose={() => { if (!submitting.current) onClose(); }} closeOnBackdrop={!saving}>
      {loading ? <p role="status">Зареждане на клиента…</p> : loadError ? (
        <div role="alert" className={styles.error}>
          <p>{loadError}</p>
          <Button variant="secondary" onClick={() => setAttempt((value) => value + 1)}>Опитай отново</Button>
          {missing && <Button variant="ghost" onClick={onReload}>Обнови списъка</Button>}
        </div>
      ) : (
        <form onSubmit={submit} aria-label="Данни за клиента">
          <FormField ref={firstInput} data-modal-initial-focus label="Име" name="firstName"
            value={form.firstName} onChange={change} maxLength={80} autoComplete="given-name"
            error={fieldErrors.firstName} disabled={saving} required />
          <FormField label="Фамилия" name="lastName" value={form.lastName} onChange={change}
            maxLength={80} autoComplete="family-name" error={fieldErrors.lastName} disabled={saving} required />
          <FormField label="Имейл" name="email" type="email" value={form.email} onChange={change}
            maxLength={254} autoComplete="email" error={fieldErrors.email} disabled={saving} required />
          <FormField label="Телефон" name="phone" type="tel" value={form.phone} onChange={change}
            maxLength={30} autoComplete="tel" error={fieldErrors.phone} hint="Незадължително поле." disabled={saving} />
          {error && <p role="alert" className={styles.error}>{error}</p>}
          <div className={styles.formActions}>
            {missing && <Button variant="secondary" onClick={onReload} disabled={saving}>Обнови списъка</Button>}
            <Button variant="secondary" onClick={onClose} disabled={saving}>Отказ</Button>
            <Button type="submit" loading={saving} loadingText="Запазване…" disabled={missing}>
              {editing ? 'Запази промените' : 'Създай клиент'}
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
