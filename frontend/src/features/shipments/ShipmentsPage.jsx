import { Link } from 'react-router-dom';
import RoutePlaceholder from '../../components/ui/RoutePlaceholder';

export default function ShipmentsPage() {
  return (
    <RoutePlaceholder title="Пратки" description="Справките и търсенето на пратки ще бъдат добавени в следващите стъпки.">
      <Link to="/shipments/new">Създай пратка →</Link>
    </RoutePlaceholder>
  );
}
