import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getCustomer } from '../../services/customerApi';
import { API_BASE_URL, apiRequest } from '../../services/apiClient';
import { assignShipmentToTrip, getShipment, getShipmentHistory } from '../../services/shipmentApi';
import { SHIPMENT_STATUS_SEQUENCE, shipmentStatusLabel } from '../../constants/shipmentStatuses';
import StatusBadge from '../../components/StatusBadge';
import pageStyles from './CreateShipmentPage.module.css';
import styles from './ShipmentDetailsPage.module.css';

const dateTime = (value) => {
  if (!value) return 'Няма зададена дата';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return 'Невалидна дата';
  return new Intl.DateTimeFormat('bg-BG', {
    dateStyle: 'long',
    timeStyle: 'short',
    timeZone: 'Europe/Sofia',
  }).format(parsed);
};

function ShipmentDetailsPage() {
  const { id } = useParams();
  const [shipment, setShipment] = useState(null);
  const [customer, setCustomer] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [historyError, setHistoryError] = useState('');
  const [qrError, setQrError] = useState(false);
  const [trips, setTrips] = useState([]);
  const [tripId, setTripId] = useState('');
  const [assigning, setAssigning] = useState(false);
  const [assignmentMessage, setAssignmentMessage] = useState('');
  const validId = /^\d+$/.test(id || '') && Number(id) > 0;

  useEffect(() => {
    let active = true;
    setShipment(null);
    setCustomer(null);
    setHistory([]);
    setHistoryError('');
    setQrError(false);
    setError('');
    if (!validId) {
      setError('Невалиден идентификатор на пратка.');
      setLoading(false);
      return () => { active = false; };
    }

    setLoading(true);
    setError('');
    getShipment(id)
      .then(async (result) => {
        if (!active) return;
        setShipment(result);

        apiRequest('/trips').then((items) => { if (active) setTrips(items.filter((item) => !item.deleted)); }).catch(() => {});

        const [historyResult, customerResult] = await Promise.allSettled([
          getShipmentHistory(id),
          getCustomer(result.customerId),
        ]);
        if (!active) return;
        if (historyResult.status === 'fulfilled') setHistory(historyResult.value);
        else setHistoryError(historyResult.reason.message || 'Историята не може да бъде заредена.');
        if (customerResult.status === 'fulfilled') setCustomer(customerResult.value);
      })
      .catch((requestError) => { if (active) setError(requestError.message || 'Пратката не може да бъде заредена.'); })
      .finally(() => { if (active) setLoading(false); });

    return () => { active = false; };
  }, [id, validId]);

  const assignTrip = async (event) => {
    event.preventDefault();
    if (!tripId || !shipment) return;
    setAssigning(true); setAssignmentMessage('');
    try {
      const updated = await assignShipmentToTrip(shipment.id, tripId, shipment.version);
      setShipment((current) => ({ ...current, tripId: updated.tripId, version: updated.version, status: updated.status }));
      setAssignmentMessage('Пратката е разпределена към курса.');
    } catch (requestError) {
      setAssignmentMessage(requestError.status === 409
        ? 'Конфликт: пратката е променена след зареждането. Презаредете детайлите и прегледайте актуалната версия.'
        : requestError.message || 'Разпределянето не успя.');
    } finally { setAssigning(false); }
  };

  const qrUrl = `${API_BASE_URL.replace(/\/$/, '')}/shipments/${id}/qr-code`;
  const back = <Link to="/shipments" className={pageStyles.backLink}><span aria-hidden="true">←</span> Към пратки</Link>;

  return (
    <div className={pageStyles.page}>
      <div className={pageStyles.pageHeading}>
        <div><div className={pageStyles.eyebrow}>ПРАТКИ <span>·</span> ПРЕГЛЕД</div><h1>Пратка #{id}</h1><p>Информация за състоянието и проследяването на пратката.</p></div>
        {back}
      </div>

          {loading && <section className={styles.stateCard} role="status"><span className={styles.loader} />Зареждане на пратката…</section>}
          {!loading && error && <section className={`${styles.stateCard} ${styles.stateError}`} role="alert"><span className={styles.stateMark}>!</span><div><strong>Пратката не е налична</strong><p>{error}</p>{back}</div></section>}

          {!loading && shipment && (
            <>
              <div className={styles.summary}>
                <div className={styles.summaryMain}><span className={styles.summaryIcon} aria-hidden="true">◇</span><div><span className={styles.summaryLabel}>TRACKING НОМЕР</span><strong>{shipment.trackingNumber || `#${shipment.id}`}</strong></div></div>
                <StatusBadge status={shipment.status} />
              </div>

              <section className={styles.historyCard} aria-label="Последователност на статусите">
                <h2>Последователност на статусите</h2>
                <ol className="shipmentProgress">{SHIPMENT_STATUS_SEQUENCE.map((status, index) => { const currentIndex = SHIPMENT_STATUS_SEQUENCE.indexOf(shipment.status); return <li key={status} className={index < currentIndex ? 'isComplete' : index === currentIndex ? 'isCurrent' : ''}><span>{index < currentIndex ? '✓' : index + 1}</span><b>{shipmentStatusLabel(status)}</b></li>; })}</ol>
              </section>

              <div className={pageStyles.contentGrid}>
                <div className={styles.primaryColumn}>
                  <section className={pageStyles.formCard} aria-labelledby="shipment-data-title">
                    <div className={pageStyles.cardHeader}><div className={pageStyles.stepIcon}>01</div><div><h2 id="shipment-data-title">Информация за пратката</h2><p>Идентификатор #{shipment.id} <span>·</span> версия {shipment.version}</p></div></div>
                    <div className={styles.detailGrid}>
                      <div className={styles.detailItem}><span>ПОЛУЧАТЕЛ</span><strong>{shipment.recipientName || '—'}</strong></div>
                      <div className={styles.detailItem}><span>КЛИЕНТ</span><strong>{customer ? `${customer.firstName} ${customer.lastName}` : `Клиент #${shipment.customerId}`}</strong>{customer?.email && <small>{customer.email}</small>}</div>
                      <div className={`${styles.detailItem} ${styles.wideItem}`}><span>АДРЕС ЗА ДОСТАВКА</span><strong>{shipment.deliveryAddress || '—'}</strong></div>
                      <div className={styles.detailItem}><span>ТЕГЛО</span><strong>{shipment.weightKg != null ? `${Number(shipment.weightKg).toLocaleString('bg-BG', { maximumFractionDigits: 2 })} кг` : '—'}</strong></div>
                      <div className={styles.detailItem}><span>ОЧАКВАНА ДОСТАВКА</span><strong>{dateTime(shipment.expectedDeliveryAt)}</strong></div>
                      <div className={styles.detailItem}><span>КУРС</span><strong>{shipment.tripId ? `Курс #${shipment.tripId}` : 'Не е разпределена'}</strong></div>
                      <div className={styles.detailItem}><span>СТАТУС</span><strong>{shipmentStatusLabel(shipment.status)}</strong></div>
                    </div>
                  </section>

                  <section className={styles.historyCard} aria-labelledby="history-title">
                    <div className={styles.historyHeading}><div><span className={styles.historyStep}>02</span><h2 id="history-title">История на проследяването</h2></div><span className={styles.eventCount}>{history.length} {history.length === 1 ? 'СЪБИТИЕ' : 'СЪБИТИЯ'}</span></div>
                    {historyError ? <p className={styles.historyMessage} role="status">{historyError}</p> : history.length === 0 ? <p className={styles.historyMessage}>Все още няма събития в историята.</p> : (
                      <ol className={styles.timeline}>
                        {history.map((event, index) => (
                          <li className={styles.timelineItem} key={event.id || `${event.occurredAt}-${index}`}>
                            <span className={`${styles.timelineDot} ${index === history.length - 1 ? styles.latestDot : ''}`} />
                            {index < history.length - 1 && <span className={styles.timelineStem} />}
                            <div className={styles.eventContent}><div><strong>{shipmentStatusLabel(event.newStatus)}</strong>{event.previousStatus && <span> от {shipmentStatusLabel(event.previousStatus)}</span>}</div><p>{dateTime(event.occurredAt)}{event.employeeId ? ` · Служител #${event.employeeId}` : ''}</p></div>
                          </li>
                        ))}
                      </ol>
                    )}
                  </section>
                  <section className={styles.historyCard} aria-labelledby="assign-trip-title">
                    <div className={styles.historyHeading}><div><span className={styles.historyStep}>03</span><h2 id="assign-trip-title">Разпределяне към курс</h2></div></div>
                    {shipment.tripId ? <p className={styles.historyMessage}>Пратката е към курс #{shipment.tripId}.</p> : trips.length === 0 ? <p className={styles.historyMessage}>Няма налични курсове за разпределяне.</p> : <form className="assignmentForm" onSubmit={assignTrip}><label htmlFor="tripId">Курс</label><select id="tripId" value={tripId} onChange={(event) => setTripId(event.target.value)} required><option value="">Изберете курс</option>{trips.map((item) => <option key={item.id} value={item.id}>#{item.id} · {item.origin} → {item.destination}</option>)}</select><button type="submit" disabled={assigning || !tripId}>{assigning ? 'Разпределяне…' : 'Разпредели'}</button></form>}
                    {assignmentMessage && <p role={assignmentMessage.startsWith('Конфликт') ? 'alert' : 'status'} className={styles.historyMessage}>{assignmentMessage} {assignmentMessage.startsWith('Конфликт') && <button type="button" onClick={() => window.location.reload()}>Презареди</button>}</p>}
                  </section>
                </div>

                <aside className={styles.sideColumn}>
                  <section className={styles.qrCard} aria-labelledby="qr-title">
                    <div className={styles.qrHeader}><div><span>TRACKING</span><h2 id="qr-title">QR код</h2></div><span className={styles.qrSpark} aria-hidden="true">⌗</span></div>
                    {!qrError ? <img className={styles.qrImage} src={qrUrl} alt={`QR код за пратка ${shipment.trackingNumber}`} onError={() => setQrError(true)} /> : <div className={styles.qrUnavailable}>QR кодът не може да бъде зареден.</div>}
                    <p>Сканирай кода за бърз достъп до tracking номера.</p>
                    <div className={styles.qrNumber}>{shipment.trackingNumber}</div>
                  </section>
                  <section className={styles.tipCard}><span className={styles.tipIcon} aria-hidden="true">i</span><div><strong>Само за преглед</strong><p>Промяна на статус и разпределяне към курс се извършват от оперативния екип.</p></div></section>
                </aside>
              </div>
            </>
          )}
    </div>
  );
}

export default ShipmentDetailsPage;
