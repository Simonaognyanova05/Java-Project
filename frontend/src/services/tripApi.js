import { apiRequest } from './apiClient';

const tripPath = (id) => '/trips/' + encodeURIComponent(id);
export const getTrips = (options = {}) => apiRequest('/trips', options);
export const getTrip = (id, options = {}) => apiRequest(tripPath(id), options);
export const createTrip = (trip, options = {}) => apiRequest('/trips', {
  ...options, method: 'POST', body: JSON.stringify(trip),
});
export const updateTrip = (id, trip, options = {}) => apiRequest(tripPath(id), {
  ...options, method: 'PUT', body: JSON.stringify(trip),
});
export const deactivateTrip = (id, options = {}) => apiRequest(tripPath(id), {
  ...options, method: 'DELETE',
});
