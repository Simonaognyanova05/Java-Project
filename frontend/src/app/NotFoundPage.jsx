import { Link } from 'react-router-dom';
import RoutePlaceholder from '../components/ui/RoutePlaceholder';

export default function NotFoundPage() {
  return (
    <RoutePlaceholder title="Страницата не е намерена" description="Проверете адреса или се върнете към началния екран.">
      <Link to="/dashboard">Към обзора →</Link>
    </RoutePlaceholder>
  );
}
