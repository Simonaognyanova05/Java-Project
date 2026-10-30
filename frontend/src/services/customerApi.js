import { apiRequest } from './apiClient';

const customerPath = (id) => '/customers/' + encodeURIComponent(id);
export const getCustomers = (options = {}) => apiRequest('/customers', options);
export const getCustomer = (id, options = {}) => apiRequest(customerPath(id), options);
export const createCustomer = (customer, options = {}) => apiRequest('/customers', {
  ...options, method: 'POST', body: JSON.stringify(customer),
});
export const updateCustomer = (id, customer, options = {}) => apiRequest(customerPath(id), {
  ...options, method: 'PUT', body: JSON.stringify(customer),
});
export const deleteCustomer = (id, options = {}) => apiRequest(customerPath(id), {
  ...options, method: 'DELETE',
});
