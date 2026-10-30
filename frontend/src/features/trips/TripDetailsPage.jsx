import { useParams } from 'react-router-dom';
import RoutePlaceholder from '../../components/ui/RoutePlaceholder';

export default function TripDetailsPage() {
  const { id } = useParams();
  return <RoutePlaceholder title={`Курс #${id}`} description="Данните за курса, екипажа и пратките са в подготовка." />;
}
