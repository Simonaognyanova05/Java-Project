import { apiRequest } from './apiClient';

export const getCustomers = () => apiRequest('/customers');
