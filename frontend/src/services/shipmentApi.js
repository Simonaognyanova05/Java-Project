import { apiRequest } from './apiClient';

export const createShipment = (shipment) => apiRequest('/shipments', {
  method: 'POST',
  body: JSON.stringify(shipment),
});
