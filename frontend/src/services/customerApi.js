import { apiRequest } from './apiClient';

export const getCustomers = () => apiRequest('/customers');
export const getCustomer = (id) => apiRequest(`/customers/${id}`);
