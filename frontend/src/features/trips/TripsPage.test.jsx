import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import TripsPage from './TripsPage';
import TripDetailsPage from './TripDetailsPage';
import { toLocalDateTimeInput } from '../../utils/dateFormat';

const trip = {
  id: 17, origin: 'София', destination: 'Виена',
  departureAt: '2030-01-01T12:15:30.123Z', expectedArrivalAt: '2030-01-02T18:45:20.456Z',
  vehicleId: 3, version: 0, deleted: false, employeeIds: [4, 9],
};
const originalFetch = global.fetch;
beforeEach(() => {
  global.fetch = jest.fn();
  HTMLDialogElement.prototype.showModal = jest.fn(function () {
    this.setAttribute('open', '');
    this.querySelector('button')?.focus();
  });
  HTMLDialogElement.prototype.close = jest.fn(function () { this.removeAttribute('open'); });
});
afterAll(() => { global.fetch = originalFetch; });
const response = (data, status = 200) => ({
  ok: status >= 200 && status < 300, status,
  text: async () => data == null ? '' : JSON.stringify(data),
});
const reply = (data, status = 200) => global.fetch.mockResolvedValueOnce(response(data, status));
function renderPage(path = '/trips') {
  return render(<MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/trips" element={<TripsPage />} />
    <Route path="/trips/:id" element={<TripDetailsPage />} />
  </Routes></MemoryRouter>);
}
async function showTrips(rows = [trip]) {
  reply(rows);
  const view = renderPage();
  await screen.findByRole('table');
  return view;
}
const field = (name) => screen.getByRole('textbox', { name });
const dateField = (name) => screen.getByLabelText(new RegExp('^' + name));
function fillForm() {
  userEvent.type(field('Начална точка'), ' София ');
  userEvent.type(field('Крайна точка'), ' Виена ');
  fireEvent.change(dateField('Тръгване'), { target: { value: '2030-01-01T12:00:00.123' } });
  fireEvent.change(dateField('Очаквано пристигане'), { target: { value: '2030-01-02T18:00:00.456' } });
  userEvent.type(screen.getByRole('spinbutton', { name: 'Превозно средство (ID)' }), '3');
}

test('loads active trips, real links and the unassigned vehicle state', async () => {
  await showTrips([{ ...trip, vehicleId: null }]);
  expect(global.fetch.mock.calls[0][0]).toBe('/api/trips');
  expect(screen.getByRole('link', { name: 'Курс #17' })).toHaveAttribute('href', '/trips/17');
  expect(screen.getByText('Не е зададено')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Редактирай курс #17' })).toBeEnabled();
});

test('distinguishes loading, failure/retry and empty results', async () => {
  reply({ detail: 'Временна грешка' }, 503);
  renderPage();
  expect(screen.getByRole('status')).toHaveTextContent('Зареждане');
  expect(await screen.findByRole('alert')).toHaveTextContent('Временна грешка');
  reply([]);
  userEvent.click(screen.getByRole('button', { name: 'Опитай отново' }));
  expect(await screen.findByText('Няма активни курсове. Добавете първия курс.')).toBeInTheDocument();
});

test('creates one trip with trimmed routes, UTC dates, vehicle ID and no client-assigned version', async () => {
  await showTrips([]);
  userEvent.click(screen.getByRole('button', { name: 'Нов курс' }));
  fillForm();
  let resolveCreate;
  global.fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveCreate = resolve; }));
  userEvent.click(screen.getByRole('button', { name: 'Създай курс' }));
  expect(screen.getByRole('button', { name: 'Запазване…' })).toBeDisabled();
  userEvent.click(screen.getByRole('button', { name: 'Запазване…' }));
  expect(global.fetch).toHaveBeenCalledTimes(2);
  const [url, options] = global.fetch.mock.calls[1];
  expect(url).toBe('/api/trips');
  expect(options.method).toBe('POST');
  expect(JSON.parse(options.body)).toEqual({
    origin: 'София', destination: 'Виена', vehicleId: 3,
    departureAt: new Date('2030-01-01T12:00:00.123').toISOString(),
    expectedArrivalAt: new Date('2030-01-02T18:00:00.456').toISOString(),
  });
  fireEvent(screen.getByRole('dialog'), new Event('cancel', { cancelable: true }));
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  await act(async () => resolveCreate(response(trip, 201)));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(screen.getByRole('status')).toHaveTextContent('Курсът е създаден.');
  expect(screen.getByRole('link', { name: 'Курс #17' })).toBeInTheDocument();
});

test('validates whitespace and requires arrival after departure', async () => {
  await showTrips([]);
  userEvent.click(screen.getByRole('button', { name: 'Нов курс' }));
  fillForm();
  userEvent.clear(field('Начална точка'));
  userEvent.type(field('Начална точка'), '   ');
  fireEvent.change(dateField('Очаквано пристигане'), { target: { value: '2030-01-01T12:00:00.123' } });
  userEvent.click(screen.getByRole('button', { name: 'Създай курс' }));
  expect(screen.getByText('Въведете начална точка.')).toBeInTheDocument();
  expect(screen.getByText('Пристигането трябва да е след тръгването.')).toBeInTheDocument();
  expect(global.fetch).toHaveBeenCalledTimes(1);
});

test('reads current version and exact timestamps, preserves a conflicting draft, and reloads only on request', async () => {
  await showTrips();
  reply(trip);
  userEvent.click(screen.getByRole('button', { name: 'Редактирай курс #17' }));
  await waitFor(() => expect(field('Начална точка')).toHaveValue('София'));
  expect(global.fetch.mock.calls[1][0]).toBe('/api/trips/17');
  expect(dateField('Тръгване')).toHaveValue(toLocalDateTimeInput(trip.departureAt));
  userEvent.clear(field('Крайна точка'));
  userEvent.type(field('Крайна точка'), 'Берлин');
  reply({ detail: 'Trip was modified' }, 409);
  userEvent.click(screen.getByRole('button', { name: 'Запази промените' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Конфликт');
  expect(field('Крайна точка')).toHaveValue('Берлин');
  const firstPut = JSON.parse(global.fetch.mock.calls[2][1].body);
  expect(firstPut).toEqual({
    origin: trip.origin, destination: 'Берлин', departureAt: trip.departureAt,
    expectedArrivalAt: trip.expectedArrivalAt, vehicleId: 3, expectedVersion: 0,
  });
  expect(global.fetch.mock.calls[2][1].method).toBe('PUT');
  expect(screen.getByRole('button', { name: 'Запази промените' })).toBeDisabled();
  expect(global.fetch).toHaveBeenCalledTimes(3);
  reply({ ...trip, version: 5, destination: 'Прага' });
  userEvent.click(screen.getByRole('button', { name: 'Зареди актуалните данни' }));
  await waitFor(() => expect(field('Крайна точка')).toHaveValue('Прага'));
  reply({ ...trip, version: 6, destination: 'Прага' });
  userEvent.click(screen.getByRole('button', { name: 'Запази промените' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  expect(JSON.parse(global.fetch.mock.calls[4][1].body).expectedVersion).toBe(5);
  expect(screen.getByText('София → Прага')).toBeInTheDocument();
});

test('does not fabricate version zero for a record without a version', async () => {
  await showTrips();
  reply({ ...trip, version: null });
  userEvent.click(screen.getByRole('button', { name: 'Редактирай курс #17' }));
  await waitFor(() => expect(field('Начална точка')).toHaveValue('София'));
  userEvent.click(screen.getByRole('button', { name: 'Запази промените' }));
  expect(screen.getByRole('alert')).toHaveTextContent('Липсва актуална версия');
  expect(global.fetch).toHaveBeenCalledTimes(2);
});

test('retains create input on a missing active vehicle and backend field validation', async () => {
  await showTrips([]);
  userEvent.click(screen.getByRole('button', { name: 'Нов курс' }));
  fillForm();
  reply({ detail: 'Active vehicle not found' }, 404);
  userEvent.click(screen.getByRole('button', { name: 'Създай курс' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Проверете ID');
  expect(field('Крайна точка')).toHaveValue(' Виена ');
  reply({ message: 'Невалидни данни', errors: { origin: 'Невалидна начална точка' } }, 400);
  userEvent.click(screen.getByRole('button', { name: 'Създай курс' }));
  expect(await screen.findByText('Невалидна начална точка')).toBeInTheDocument();
});

test('requires deactivation confirmation and keeps a trip with undelivered shipments', async () => {
  await showTrips();
  userEvent.click(screen.getByRole('button', { name: 'Деактивирай курс #17' }));
  expect(screen.getByRole('button', { name: 'Отказ' })).toHaveFocus();
  userEvent.click(screen.getByRole('button', { name: 'Отказ' }));
  expect(global.fetch).toHaveBeenCalledTimes(1);
  userEvent.click(screen.getByRole('button', { name: 'Деактивирай курс #17' }));
  reply({ detail: 'Trip has undelivered shipments' }, 409);
  userEvent.click(screen.getByRole('button', { name: 'Деактивирай курса' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('недоставени пратки');
  expect(screen.getByRole('link', { name: 'Курс #17' })).toBeInTheDocument();
  reply(null, 204);
  userEvent.click(screen.getByRole('button', { name: 'Деактивирай курса' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  expect(global.fetch.mock.calls[2][0]).toBe('/api/trips/17');
  expect(global.fetch.mock.calls[2][1].method).toBe('DELETE');
  expect(screen.getByRole('status')).toHaveTextContent('Курсът е деактивиран.');
  expect(screen.getByRole('button', { name: 'Нов курс' })).toHaveFocus();
  expect(screen.queryByRole('link', { name: 'Курс #17' })).not.toBeInTheDocument();
});

test('handles a missing edit record and allows list refresh', async () => {
  await showTrips();
  reply({ detail: 'Not found' }, 404);
  userEvent.click(screen.getByRole('button', { name: 'Редактирай курс #17' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Курсът не е намерен');
  reply([]);
  userEvent.click(screen.getByRole('button', { name: 'Обнови списъка' }));
  await screen.findByText('Няма активни курсове. Добавете първия курс.');
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

test('opens real trip details via the list link and displays crew IDs', async () => {
  await showTrips();
  reply(trip);
  userEvent.click(screen.getByRole('link', { name: 'Курс #17' }));
  expect(await screen.findByText('4, 9')).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Курс #17' })).toBeInTheDocument();
  expect(screen.getByText('ID: 3')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Редактирай курса' })).toBeEnabled();
  expect(global.fetch.mock.calls[1][0]).toBe('/api/trips/17');
});

test('detail errors retain navigation and retry', async () => {
  reply({ detail: 'Missing' }, 404);
  renderPage('/trips/17');
  expect(await screen.findByRole('alert')).toHaveTextContent('Курсът не е намерен или е деактивиран.');
  expect(screen.getByRole('link', { name: '← Към курсовете' })).toHaveAttribute('href', '/trips');
  reply(trip);
  userEvent.click(screen.getByRole('button', { name: 'Опитай отново' }));
  expect(await screen.findByText('4, 9')).toBeInTheDocument();
});

test('cancels trip list reads on unmount', () => {
  global.fetch.mockImplementation(() => new Promise(() => {}));
  const { unmount } = renderPage();
  const signal = global.fetch.mock.calls[0][1].signal;
  unmount();
  expect(signal.aborted).toBe(true);
});
