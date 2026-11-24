export const SHIPMENT_STATUSES = {
  CREATED: { label: 'Създадена', tone: 'neutral' },
  ACCEPTED: { label: 'Приета', tone: 'blue' },
  LOADED: { label: 'Натоварена', tone: 'purple' },
  IN_TRANSIT: { label: 'В транспорт', tone: 'teal' },
  DELIVERED: { label: 'Доставена', tone: 'green' },
};

export const shipmentStatusLabel = (status) => SHIPMENT_STATUSES[status]?.label || status || 'Неизвестен';

export const SHIPMENT_STATUS_SEQUENCE = ['CREATED', 'ACCEPTED', 'LOADED', 'IN_TRANSIT', 'DELIVERED'];
