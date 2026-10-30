import { Navigate, Route, Routes } from 'react-router-dom';
import DashboardPage from '../features/dashboard/DashboardPage';
import ShipmentsPage from '../features/shipments/ShipmentsPage';
import CreateShipmentPage from '../features/shipments/CreateShipmentPage';
import ShipmentDetailsPage from '../features/shipments/ShipmentDetailsPage';
import TripsPage from '../features/trips/TripsPage';
import TripDetailsPage from '../features/trips/TripDetailsPage';
import CustomersPage from '../features/customers/CustomersPage';
import EmployeesPage from '../features/employees/EmployeesPage';
import VehiclesPage from '../features/vehicles/VehiclesPage';
import NotFoundPage from './NotFoundPage';

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="/shipments" element={<ShipmentsPage />} />
      <Route path="/shipments/new" element={<CreateShipmentPage />} />
      <Route path="/shipments/:id" element={<ShipmentDetailsPage />} />
      <Route path="/trips" element={<TripsPage />} />
      <Route path="/trips/:id" element={<TripDetailsPage />} />
      <Route path="/customers" element={<CustomersPage />} />
      <Route path="/employees" element={<EmployeesPage />} />
      <Route path="/vehicles" element={<VehiclesPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
