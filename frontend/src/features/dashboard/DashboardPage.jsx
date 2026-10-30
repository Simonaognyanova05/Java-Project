import { Link } from 'react-router-dom';
import RoutePlaceholder from '../../components/ui/RoutePlaceholder';

export default function DashboardPage() {
  return (
    <RoutePlaceholder title="Обзор" description="Тук ще бъде оперативният преглед на транспорта. Разделът е в подготовка.">
      <Link to="/shipments/new">Създай пратка →</Link>
    </RoutePlaceholder>
  );
}
