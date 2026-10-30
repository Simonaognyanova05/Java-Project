import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { getCustomers } from '../../services/customerApi';
import { createShipment } from '../../services/shipmentApi';
import styles from './CreateShipmentPage.module.css';
import Button from '../../components/ui/Button';
import FormField from '../../components/ui/FormField';

const initialForm = {
  customerId: '',
  recipientName: '',
  deliveryAddress: '',
  weightKg: '',
  expectedDeliveryAt: '',
};

const formatLocalDateTime = (date) => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
};

function CreateShipmentPage() {
  const [customers, setCustomers] = useState([]);
  const [customersLoading, setCustomersLoading] = useState(true);
  const [customersError, setCustomersError] = useState('');
  const [form, setForm] = useState(initialForm);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [createdShipment, setCreatedShipment] = useState(null);
  const minDeliveryDate = useMemo(() => formatLocalDateTime(new Date()), []);

  useEffect(() => {
    let active = true;
    getCustomers()
      .then((data) => { if (active) setCustomers(data); })
      .catch((error) => { if (active) setCustomersError(error.message); })
      .finally(() => { if (active) setCustomersLoading(false); });
    return () => { active = false; };
  }, []);

  const updateField = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setFormError('');
    setCreatedShipment(null);
  };

  const submit = async (event) => {
    event.preventDefault();
    setFormError('');
    setCreatedShipment(null);
    if (!form.customerId) {
      setFormError('Изберете клиент, преди да създадете пратката.');
      return;
    }
    if (new Date(form.expectedDeliveryAt).getTime() <= Date.now()) {
      setFormError('Очакваната доставка трябва да е в бъдещ момент.');
      return;
    }

    setSaving(true);
    try {
      const created = await createShipment({
        customerId: Number(form.customerId),
        recipientName: form.recipientName.trim(),
        deliveryAddress: form.deliveryAddress.trim(),
        weightKg: Number(form.weightKg),
        expectedDeliveryAt: new Date(form.expectedDeliveryAt).toISOString(),
      });
      setCreatedShipment(created);
      setForm(initialForm);
    } catch (error) {
      setFormError(error.message || 'Пратката не беше създадена. Опитайте отново.');
    } finally {
      setSaving(false);
    }
  };

  const customerLabel = (customer) => `${customer.firstName} ${customer.lastName}`;

  return (
    <div className={styles.page}>
      <div className={styles.pageHeading}>
        <div>
          <div className={styles.eyebrow}>ПРАТКИ <span>·</span> СЪЗДАВАНЕ</div>
          <h1>Създаване на пратка</h1>
          <p>Въведете данните за получателя и доставката, за да регистрирате нова пратка.</p>
        </div>
        <Link to="/shipments" className={styles.backLink}><span aria-hidden="true">←</span> Към пратки</Link>
      </div>

      <div className={styles.contentGrid}>
        <section className={styles.formCard} aria-labelledby="form-title">
          <div className={styles.cardHeader}>
            <div className={styles.stepIcon}>01</div>
            <div><h2 id="form-title">Данни за пратката</h2><p>Задължителните полета са обозначени със <span>*</span></p></div>
          </div>

          {customersError && (
            <div className={styles.inlineError} role="alert">
              <span aria-hidden="true">!</span>
              <div><strong>Клиентите не могат да се заредят</strong><p>{customersError}</p><button type="button" onClick={() => window.location.reload()}>Опитай отново</button></div>
            </div>
          )}

          {createdShipment && (
            <div className={styles.success} role="status">
              <span className={styles.successIcon} aria-hidden="true">✓</span>
              <div><strong>Пратката е създадена успешно</strong><p>Tracking номер: <b>{createdShipment.trackingNumber}</b></p></div>
              <button type="button" aria-label="Затвори съобщението" onClick={() => setCreatedShipment(null)}>×</button>
            </div>
          )}

          <form onSubmit={submit}>
            <div className={styles.formSection}>
              <div className={styles.sectionTitle}><span>01</span><h3>Получател</h3><i /></div>
              <FormField as="select" id="customerId" name="customerId" label="Клиент"
                value={form.customerId} onChange={updateField} required
                disabled={customersLoading || !!customersError || customers.length === 0}
                hint="Пратката ще бъде свързана с избрания клиент.">
                <option value="">{customersLoading ? 'Зареждане на клиенти…' : customers.length ? 'Изберете клиент' : 'Няма налични клиенти'}</option>
                {customers.map((customer) => <option key={customer.id} value={customer.id}>{customerLabel(customer)}{customer.email ? ` · ${customer.email}` : ''}</option>)}
              </FormField>
              <FormField id="recipientName" name="recipientName" label="Име на получателя"
                value={form.recipientName} onChange={updateField} placeholder="напр. Мария Петрова"
                maxLength="160" autoComplete="name" required />
            </div>

            <div className={styles.formSection}>
              <div className={styles.sectionTitle}><span>02</span><h3>Доставка</h3><i /></div>
              <FormField id="deliveryAddress" name="deliveryAddress" label="Адрес за доставка"
                value={form.deliveryAddress} onChange={updateField} leading="⌖"
                placeholder="Улица, номер, град, пощенски код" maxLength="255" autoComplete="street-address" required />
              <div className={styles.twoColumns}>
                <FormField id="weightKg" name="weightKg" label="Тегло" type="number"
                  min="0.01" max="9999999999.99" step="0.01" value={form.weightKg} onChange={updateField}
                  placeholder="0.00" trailing="кг" hint="Въведете теглото в килограми." required />
                <FormField id="expectedDeliveryAt" name="expectedDeliveryAt" label="Очаквана доставка"
                  type="datetime-local" min={minDeliveryDate} value={form.expectedDeliveryAt}
                  onChange={updateField} hint="Посочете бъдеща дата и час." required />
              </div>
            </div>

            {formError && <div className={styles.formError} role="alert"><span aria-hidden="true">!</span>{formError}</div>}

            <div className={styles.formFooter}>
              <p><span className={styles.lockIcon} aria-hidden="true">▣</span> Tracking номерът се генерира автоматично.</p>
              <Button type="submit" loading={saving} loadingText="Създаване…"
                disabled={customersLoading || !!customersError || customers.length === 0}>
                Създай пратка <span aria-hidden="true">→</span>
              </Button>
            </div>
          </form>
        </section>

        <aside className={styles.infoColumn}>
          <section className={styles.infoCard}>
            <div className={styles.infoIcon} aria-hidden="true">✳</div>
            <h2>Какво следва?</h2>
            <p>След създаване пратката получава уникален tracking номер и статус <strong>„Създадена“</strong>.</p>
            <div className={styles.infoDivider} />
            <div className={styles.processStep}><span>1</span><div><b>Регистрация</b><small>Пратката се записва в системата</small></div></div>
            <div className={styles.processLine} />
            <div className={styles.processStep}><span>2</span><div><b>Tracking номер</b><small>Генерира се уникален код</small></div></div>
            <div className={styles.processLine} />
            <div className={styles.processStep}><span>3</span><div><b>Готова за обработка</b><small>Може да бъде разпределена към курс</small></div></div>
          </section>
          <section className={styles.helpCard}>
            <div className={styles.helpIcon} aria-hidden="true">?</div>
            <div><strong>Нужда от помощ?</strong><p>Провери данните преди създаване. Очакваната дата за доставка трябва да е в бъдещето.</p></div>
          </section>
        </aside>
      </div>
    </div>
  );
}

export default CreateShipmentPage;
