import { apiRequest } from './apiClient';

export const createShipment = (shipment, options = {}) => apiRequest('/shipments', {
  ...options,
  method: 'POST',
  body: JSON.stringify(shipment),
});
