import { navigationItems } from './navigation';

function decodeId(value) {
  try { return decodeURIComponent(value); } catch { return value; }
}

export function getPageMetadata(pathname) {
  if (pathname === '/shipments/new') return { title: 'Нова пратка', section: 'Пратки', parent: '/shipments' };
  const shipment = pathname.match(/^\/shipments\/([^/]+)\/?$/);
  if (shipment) return { title: `Пратка #${decodeId(shipment[1])}`, section: 'Пратки', parent: '/shipments' };
  const trip = pathname.match(/^\/trips\/([^/]+)\/?$/);
  if (trip) return { title: `Курс #${decodeId(trip[1])}`, section: 'Курсове', parent: '/trips' };
  const item = navigationItems.find(({ path }) => pathname === path || pathname === `${path}/`);
  return { title: item?.label || 'Страницата не е намерена', section: 'Работно пространство', parent: '/dashboard' };
}
