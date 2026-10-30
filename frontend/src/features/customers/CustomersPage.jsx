import { useEffect, useRef, useState } from 'react';
import Button from '../../components/ui/Button';
import DataTable from '../../components/ui/DataTable';
import Modal from '../../components/ui/Modal';
import { deleteCustomer, getCustomers } from '../../services/customerApi';
import CustomerForm from './CustomerForm';
import styles from './CustomersPage.module.css';

export default function CustomersPage() {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [editor, setEditor] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [deleteError, setDeleteError] = useState('');
  const [deleteMissing, setDeleteMissing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState('');
  const removing = useRef(false);
  const addButton = useRef(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    setLoading(true);
    setError('');
    getCustomers({ signal: controller.signal })
      .then((data) => { if (active) setCustomers(data); })
      .catch((failure) => { if (active && failure.name !== 'AbortError') setError(failure.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [revision]);

  const reload = () => {
    setEditor(null);
    setDeleting(null);
    setRevision((value) => value + 1);
  };

  const remove = async () => {
    if (removing.current) return;
    removing.current = true;
    setBusy(true);
    setDeleteError('');
    try {
      await deleteCustomer(deleting.id);
      setCustomers((current) => current.filter((customer) => customer.id !== deleting.id));
      setDeleting(null);
      setNotice('Клиентът е изтрит.');
      // The row action that opened this dialog no longer exists.
      addButton.current?.focus();
    } catch (failure) {
      setDeleteMissing(failure.status === 404);
      setDeleteError(failure.status === 404
        ? 'Клиентът вече не съществува. Обновете списъка.'
        : failure.status === 409
          ? 'Клиентът не може да бъде изтрит поради конфликт. Проверете свързаните данни.'
          : failure.message);
    } finally {
      removing.current = false;
      setBusy(false);
    }
  };

  const columns = [
    { key: 'name', header: 'Клиент', render: (row) => <strong>{row.firstName} {row.lastName}</strong> },
    { key: 'email', header: 'Имейл' },
    { key: 'phone', header: 'Телефон', render: (row) => row.phone || '—' },
    { key: 'actions', header: 'Действия', render: (row) => (
      <div className={styles.rowActions}>
        <Button variant="secondary" aria-label={'Редактирай ' + row.firstName + ' ' + row.lastName}
          onClick={() => { setNotice(''); setEditor({ id: row.id }); }}>Редактирай</Button>
        <Button variant="danger" aria-label={'Изтрий ' + row.firstName + ' ' + row.lastName}
          onClick={() => { setNotice(''); setDeleteError(''); setDeleteMissing(false); setDeleting(row); }}>Изтрий</Button>
      </div>
    ) },
  ];

  return (
    <section>
      <div className={styles.heading}>
        <div><span className={styles.eyebrow}>УПРАВЛЕНИЕ</span><h1>Клиенти</h1>
          <p>Данни за контакт и управление на клиентите.</p></div>
        <div className={styles.rowActions}>
          <Button variant="secondary" onClick={reload} disabled={loading}>Обнови</Button>
          <Button ref={addButton} disabled={loading || !!error} onClick={() => { setNotice(''); setEditor({ id: null }); }}>Нов клиент</Button>
        </div>
      </div>
      {notice && <p role="status" className={styles.notice}>{notice}</p>}
      <DataTable caption="Списък с клиенти" columns={columns} rows={customers} loading={loading}
        error={error} onRetry={reload} emptyMessage="Все още няма клиенти. Добавете първия клиент." />
      {editor && <CustomerForm key={editor.id ?? 'new'} customerId={editor.id}
        onClose={() => setEditor(null)} onReload={reload}
        onSaved={(customer, editing) => {
          setCustomers((current) => editing
            ? current.map((row) => row.id === customer.id ? customer : row)
            : [...current, customer]);
          setNotice(editing ? 'Промените са запазени.' : 'Клиентът е създаден.');
          setEditor(null);
        }} />}
      {deleting && <Modal open title="Изтриване на клиент"
        description={'Да изтрием ли ' + deleting.firstName + ' ' + deleting.lastName + '?'}
        onClose={() => { if (!removing.current) setDeleting(null); }} closeOnBackdrop={!busy}
        footer={<>
          {deleteMissing && <Button variant="secondary" onClick={reload}>Обнови списъка</Button>}
          <Button variant="secondary" data-modal-initial-focus onClick={() => setDeleting(null)} disabled={busy}>Отказ</Button>
          <Button variant="danger" onClick={remove} loading={busy} loadingText="Изтриване…" disabled={deleteMissing}>Изтрий клиента</Button>
        </>}>
        <p>Изтриването е окончателно. Клиент със свързани пратки може да не бъде изтрит от сървъра.</p>
        {deleteError && <p role="alert" className={styles.error}>{deleteError}</p>}
      </Modal>}
    </section>
  );
}
