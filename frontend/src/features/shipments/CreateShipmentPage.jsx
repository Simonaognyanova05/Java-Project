import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { getCustomers } from '../../services/customerApi';
import { createShipment } from '../../services/shipmentApi';
import styles from './CreateShipmentPage.module.css';

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
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
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
    <div className={styles.shell}>
      <aside className={`${styles.sidebar} ${mobileMenuOpen ? styles.sidebarOpen : ''}`}>
        <Link to="/shipments/new" className={styles.brand} onClick={() => setMobileMenuOpen(false)}>
          <span className={styles.brandMark} aria-hidden="true">S</span>
          <span>Smart<span className={styles.brandAccent}>Shipment</span><small>LOGISTICS PLATFORM</small></span>
        </Link>
        <div className={styles.navLabel}>РАБОТНО ПРОСТРАНСТВО</div>
        <nav aria-label="Основна навигация" className={styles.nav}>
          <span className={styles.navItem} aria-disabled="true" title="Екранът ще бъде добавен в следваща стъпка">
            <span className={styles.navIcon} aria-hidden="true">▦</span> Табло
          </span>
          <a href="/shipments/new" className={`${styles.navItem} ${styles.navActive}`} aria-current="page">
            <span className={styles.navIcon} aria-hidden="true">◇</span> Пратки
            <span className={styles.navCount}>НОВА</span>
          </a>
          <span className={styles.navItem} aria-disabled="true" title="Екранът ще бъде добавен в следваща стъпка">
            <span className={styles.navIcon} aria-hidden="true">↗</span> Курсове
          </span>
          <span className={styles.navItem} aria-disabled="true" title="Екранът ще бъде добавен в следваща стъпка">
            <span className={styles.navIcon} aria-hidden="true">◎</span> Клиенти
          </span>
        </nav>
        <div className={styles.sidebarBottom}>
          <div className={styles.onlineDot} /> Система онлайн
          <small>SMART SHIPMENT TRACKING · v1.0</small>
        </div>
      </aside>

      <div className={styles.mainColumn}>
        <header className={styles.topbar}>
          <button className={styles.menuButton} type="button" aria-label="Отвори навигацията" onClick={() => setMobileMenuOpen((open) => !open)}>☰</button>
          <div className={styles.breadcrumb}>Пратки <span>/</span> <strong>Нова пратка</strong></div>
          <div className={styles.topbarRight}>
            <span className={styles.environment}><span /> ОПЕРАТИВНА СИСТЕМА</span>
            <div className={styles.avatar} aria-label="Потребител">ОА</div>
          </div>
        </header>

        <main className={styles.workspace}>
          <div className={styles.pageHeading}>
            <div>
              <div className={styles.eyebrow}>ПРАТКИ <span>·</span> СЪЗДАВАНЕ</div>
              <h1>Създаване на пратка</h1>
              <p>Въведете данните за получателя и доставката, за да регистрирате нова пратка.</p>
            </div>
            <Link to="/shipments/new" className={styles.backLink}><span aria-hidden="true">←</span> Към пратки</Link>
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
                  <div className={styles.field}>
                    <label htmlFor="customerId">Клиент <span>*</span></label>
                    <select id="customerId" name="customerId" value={form.customerId} onChange={updateField} required disabled={customersLoading || !!customersError || customers.length === 0}>
                      <option value="">{customersLoading ? 'Зареждане на клиенти…' : customers.length ? 'Изберете клиент' : 'Няма налични клиенти'}</option>
                      {customers.map((customer) => <option key={customer.id} value={customer.id}>{customerLabel(customer)}{customer.email ? ` · ${customer.email}` : ''}</option>)}
                    </select>
                    <small>Пратката ще бъде свързана с избрания клиент.</small>
                  </div>
                  <div className={styles.field}>
                    <label htmlFor="recipientName">Име на получателя <span>*</span></label>
                    <input id="recipientName" name="recipientName" value={form.recipientName} onChange={updateField} placeholder="напр. Мария Петрова" maxLength="160" autoComplete="name" required />
                  </div>
                </div>

                <div className={styles.formSection}>
                  <div className={styles.sectionTitle}><span>02</span><h3>Доставка</h3><i /></div>
                  <div className={styles.field}>
                    <label htmlFor="deliveryAddress">Адрес за доставка <span>*</span></label>
                    <div className={styles.inputWithIcon}><span aria-hidden="true">⌖</span><input id="deliveryAddress" name="deliveryAddress" value={form.deliveryAddress} onChange={updateField} placeholder="Улица, номер, град, пощенски код" maxLength="255" autoComplete="street-address" required /></div>
                  </div>
                  <div className={styles.twoColumns}>
                    <div className={styles.field}>
                      <label htmlFor="weightKg">Тегло <span>*</span></label>
                      <div className={styles.inputWithUnit}><input id="weightKg" name="weightKg" type="number" min="0.01" max="9999999999.99" step="0.01" value={form.weightKg} onChange={updateField} placeholder="0.00" required /><span>кг</span></div>
                      <small>Въведете теглото в килограми.</small>
                    </div>
                    <div className={styles.field}>
                      <label htmlFor="expectedDeliveryAt">Очаквана доставка <span>*</span></label>
                      <input id="expectedDeliveryAt" name="expectedDeliveryAt" type="datetime-local" min={minDeliveryDate} value={form.expectedDeliveryAt} onChange={updateField} required />
                      <small>Посочете бъдеща дата и час.</small>
                    </div>
                  </div>
                </div>

                {formError && <div className={styles.formError} role="alert"><span aria-hidden="true">!</span>{formError}</div>}

                <div className={styles.formFooter}>
                  <p><span className={styles.lockIcon} aria-hidden="true">▣</span> Tracking номерът се генерира автоматично.</p>
                  <button className={styles.submitButton} type="submit" disabled={saving || customersLoading || !!customersError || customers.length === 0}>
                    {saving ? <><span className={styles.spinner} /> Създаване…</> : <>Създай пратка <span aria-hidden="true">→</span></>}
                  </button>
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
          <footer className={styles.pageFooter}><span>© 2025 Smart Shipment Tracking</span><span>Защитена връзка <i /></span></footer>
        </main>
      </div>
    </div>
  );
}

export default CreateShipmentPage;
