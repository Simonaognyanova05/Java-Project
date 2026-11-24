import { SHIPMENT_STATUSES } from '../constants/shipmentStatuses';
import './StatusBadge.css';

export default function StatusBadge({ status, overdue = false }) {
  const item = SHIPMENT_STATUSES[status];
  return <span className={`statusBadge statusBadge--${item?.tone || 'neutral'}${overdue ? ' statusBadge--overdue' : ''}`}>
    <i aria-hidden="true" />{overdue ? 'Закъсняла · ' : ''}{item?.label || status || 'Неизвестен'}
  </span>;
}
