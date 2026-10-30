import { useParams } from 'react-router-dom';
import RoutePlaceholder from '../../components/ui/RoutePlaceholder';

export default function ShipmentDetailsPage() {
  const { id } = useParams();
  return <RoutePlaceholder title={`Пратка #${id}`} description="Детайлите и разпределянето към курс са в подготовка." />;
}
