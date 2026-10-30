import { apiRequest } from './apiClient';

export const getCustomers = (options = {}) => apiRequest('/customers', options);
