import { apiRequest } from './apiClient';

export const createShipment = (shipment) => apiRequest('/shipments', {
  method: 'POST',
  body: JSON.stringify(shipment),
});

export const getShipment = (id) => apiRequest(`/shipments/${id}`);
export const getShipmentHistory = (id) => apiRequest(`/shipments/${id}/history`);
export const assignShipmentToTrip = (id, tripId, expectedVersion) => apiRequest(`/shipments/${id}/trip`, {
  method: 'PUT',
  body: JSON.stringify({ tripId: Number(tripId), expectedVersion }),
});
export const getOverdueShipments = (page = 0, size = 20) =>
  apiRequest(`/shipments/overdue?page=${page}&size=${size}`);
