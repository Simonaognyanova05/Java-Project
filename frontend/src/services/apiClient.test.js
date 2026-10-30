import { ApiError, apiRequest } from './apiClient';
import { getCustomers } from './customerApi';
import { createShipment } from './shipmentApi';

const originalFetch = global.fetch;
beforeEach(() => { global.fetch = jest.fn(); });
afterAll(() => { global.fetch = originalFetch; });

function reply(status, body = '') {
  global.fetch.mockResolvedValue({
    ok: status >= 200 && status < 300, status, text: jest.fn().mockResolvedValue(body),
  });
}

test('GET joins service paths and sends Accept without a JSON body header', async () => {
  reply(200, '[{"id":1}]');
  await expect(getCustomers()).resolves.toEqual([{ id: 1 }]);
  const [url, options] = global.fetch.mock.calls[0];
  expect(url).toBe('/api/customers');
  expect(options.headers.get('Accept')).toBe('application/json');
  expect(options.headers.has('Content-Type')).toBe(false);
});

test('shipment service serializes JSON and forwards cancellation and custom headers', async () => {
  reply(201, '{"id":12}');
  const controller = new AbortController();
  await expect(createShipment({ weightKg: 2 }, {
    signal: controller.signal, headers: new Headers({ 'X-Request-ID': 'test' }),
  })).resolves.toEqual({ id: 12 });
  const [url, options] = global.fetch.mock.calls[0];
  expect(url).toBe('/api/shipments');
  expect(options.method).toBe('POST');
  expect(options.body).toBe('{"weightKg":2}');
  expect(options.signal).toBe(controller.signal);
  expect(options.headers.get('Content-Type')).toBe('application/json');
  expect(options.headers.get('X-Request-ID')).toBe('test');
});

test.each([204, 205, 200])('handles an empty %s response', async (status) => {
  reply(status);
  await expect(apiRequest('customers')).resolves.toBeNull();
});

test.each([
  [400, { message: 'Невалидни данни', errors: { email: 'Невалиден имейл' } }, 'Невалидни данни'],
  [404, { title: 'Не е намерено' }, 'Не е намерено'],
  [409, { detail: 'Версията е променена' }, 'Версията е променена'],
])('preserves HTTP %s, backend details and field errors', async (status, data, message) => {
  reply(status, JSON.stringify(data));
  await expect(apiRequest('/customers')).rejects.toMatchObject({
    name: 'ApiError', status, data, message,
    fieldErrors: data.errors ?? null,
  });
  expect(global.fetch).toHaveBeenCalledTimes(1);
});

test('HTML proxy errors retain HTTP status without exposing markup', async () => {
  reply(500, '<html>Proxy failed</html>');
  await expect(apiRequest('/customers')).rejects.toMatchObject({
    status: 500, message: 'Заявката не успя (500).', data: null,
  });
});

test('rejects malformed successful JSON instead of presenting it as data', async () => {
  reply(200, '<html>Wrong origin</html>');
  await expect(apiRequest('/customers')).rejects.toMatchObject({
    status: 200, message: 'Сървърът върна неочакван отговор.',
  });
});

test('network errors become ApiError and never trigger automatic retries', async () => {
  global.fetch.mockRejectedValue(new TypeError('Failed to fetch'));
  await expect(apiRequest('/customers')).rejects.toBeInstanceOf(ApiError);
  expect(global.fetch).toHaveBeenCalledTimes(1);
  await expect(apiRequest('/customers')).rejects.toMatchObject({ status: 0, data: null });
});

test('aborts remain distinguishable from network errors', async () => {
  const error = new DOMException('Aborted', 'AbortError');
  global.fetch.mockRejectedValue(error);
  await expect(apiRequest('/customers')).rejects.toBe(error);
});

test('leaves multipart boundaries to fetch and preserves explicit headers', async () => {
  reply(204);
  const body = new FormData();
  body.append('name', 'test');
  await apiRequest('/upload', { method: 'POST', body });
  expect(global.fetch.mock.calls[0][1].headers.has('Content-Type')).toBe(false);
  reply(204);
  await apiRequest('/customers', { headers: [['Accept', 'application/problem+json']] });
  expect(global.fetch.mock.calls[1][1].headers.get('Accept')).toBe('application/problem+json');
});

test('normalizes a configured base URL with trailing slashes', async () => {
  const previous = process.env.REACT_APP_API_BASE_URL;
  try {
    process.env.REACT_APP_API_BASE_URL = ' https://api.example.test/api/// ';
    let configuredRequest;
    jest.isolateModules(() => { configuredRequest = require('./apiClient').apiRequest; });
    reply(204);
    await configuredRequest('/customers');
    expect(global.fetch.mock.calls[0][0]).toBe('https://api.example.test/api/customers');
  } finally {
    if (previous === undefined) delete process.env.REACT_APP_API_BASE_URL;
    else process.env.REACT_APP_API_BASE_URL = previous;
  }
});
