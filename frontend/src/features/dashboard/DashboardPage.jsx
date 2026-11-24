import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getOverdueShipments } from '../../services/shipmentApi';
import { apiRequest } from '../../services/apiClient';
import StatusBadge from '../../components/StatusBadge';
import './DashboardPage.css';

const dateTime = (value) => value ? new Intl.DateTimeFormat('bg-BG', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Europe/Sofia' }).format(new Date(value)) : 'Няма данни';

export default function DashboardPage() {
  const [trips, setTrips] = useState([]);
  const [overdue, setOverdue] = useState([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const load = useCallback(async (nextPage = page) => {
    setLoading(true); setError('');
    const results = await Promise.allSettled([apiRequest('/trips'), getOverdueShipments(nextPage, 10)]);
    if (results[0].status === 'fulfilled') setTrips(results[0].value.filter((trip) => !trip.deleted));
    if (results[1].status === 'fulfilled') setOverdue(results[1].value);
    const failed = results.find((result) => result.status === 'rejected');
    if (failed) setError(`Част от справката не се зареди: ${failed.reason.message}`);
    setLoading(false);
  }, [page]);
  useEffect(() => { load(); }, [load]);
  const changePage = (next) => setPage(next);
  const trip = trips[0];
  return <main className="dashboard">
    <header className="dashboardHeader"><div><span className="dashboardEyebrow">ЦЕНТЪР ЗА ДИСПЕЧЕРИ</span><h1>Табло</h1><p>Оперативен преглед от наличните справки.</p></div><Link className="dashboardAction" to="/shipments/new">+ Нова пратка</Link></header>
    {error && <div className="dashboardNotice" role="alert">{error}<button onClick={() => load()}>Опитай отново</button></div>}
    <section className="dashboardMetrics" aria-label="Налични данни">
      <article><span>Активни курсове</span><strong>{loading ? '…' : trips.length}</strong><small>От GET /api/trips</small></article>
      <article><span>Закъснели · тази страница</span><strong>{loading ? '…' : overdue.length}</strong><small>Справка с пагинация</small></article>
      <article><span>Общ брой пратки</span><strong>Няма данни</strong><small>Няма обща статистика в API</small></article>
    </section>
    <section className="dashboardRoute"><div className="dashboardSectionTitle"><div><span>МАРШРУТ</span><h2>{trip ? `Курс #${trip.id}` : 'Няма активен курс'}</h2></div>{trip && <span className="routeChip">Схематичен маршрут</span>}</div>
      {trip ? <div className="routeDiagram"><div className="routeStop"><i/><span>НАЧАЛО</span><strong>{trip.origin}</strong><small>Отпътуване · {dateTime(trip.departureAt)}</small></div><div className="routeTrack"><b>Курс #{trip.id}</b></div><div className="routeStop routeStopEnd"><i/><span>ДЕСТИНАЦИЯ</span><strong>{trip.destination}</strong><small>Пристигане · {dateTime(trip.expectedArrivalAt)}</small></div></div> : <p className="dashboardEmpty">Няма реални данни за маршрут.</p>}
    </section>
    <section className="dashboardTable"><div className="dashboardSectionTitle"><div><span>СПРАВКА</span><h2>Закъснели пратки</h2></div><Link to="/shipments/new">Създай пратка →</Link></div>
      {loading ? <p className="dashboardEmpty">Зареждане…</p> : overdue.length === 0 ? <p className="dashboardEmpty">Няма закъснели пратки на тази страница.</p> : <div className="dashboardScroll"><table><thead><tr><th>Tracking №</th><th>Статус</th><th>Очаквана доставка</th><th>Курс</th><th></th></tr></thead><tbody>{overdue.map((item) => <tr key={item.id}><td><Link to={`/shipments/${item.id}`}>{item.trackingNumber || `#${item.id}`}</Link></td><td><StatusBadge status={item.status} overdue/></td><td>{dateTime(item.expectedDeliveryAt)}</td><td>{item.tripId ? `#${item.tripId}` : 'Без курс'}</td><td><Link to={`/shipments/${item.id}`}>Отвори</Link></td></tr>)}</tbody></table></div>}
      <div className="dashboardPagination"><button type="button" disabled={page===0||loading} onClick={() => changePage(page-1)}>← Назад</button><span>Страница {page+1}</span><button type="button" disabled={loading||overdue.length<10} onClick={() => changePage(page+1)}>Напред →</button></div>
    </section>
  </main>;
}
