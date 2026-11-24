import { apiRequest } from './apiClient';

export const createShipment = (shipment) => apiRequest('/shipments', {
  method: 'POST',
  body: JSON.stringify(shipment),
});

export const getShipment = (id) => apiRequest(`/shipments/${id}`);
export const getShipmentHistory = (id) => apiRequest(`/shipments/${id}/history`);
