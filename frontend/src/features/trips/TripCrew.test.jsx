import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import TripDetailsPage from './TripDetailsPage';

const trip = {
  id: 17, origin: 'София', destination: 'Виена', departureAt: '2030-01-01T12:00:00Z',
  expectedArrivalAt: '2030-01-02T12:00:00Z', vehicleId: 3, version: 4, employeeIds: [9],
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
const idInput = () => screen.getByRole('spinbutton', { name: 'Служител (ID)' });
const crewTable = () => screen.getByRole('table', { name: 'Назначени служители' });
async function showCrew(initial = trip) {
  reply(initial);
  const view = render(<MemoryRouter initialEntries={['/trips/17']}><Routes>
    <Route path="/trips/:id" element={<TripDetailsPage />} />
    <Route path="/trips" element={<h1>Курсове</h1>} />
  </Routes></MemoryRouter>);
  await screen.findByRole('table', { name: 'Назначени служители' });
  return view;
}

test('shows returned crew IDs and adds an employee using PUT and the authoritative response', async () => {
  await showCrew();
  expect(within(crewTable()).getByText('9')).toBeInTheDocument();
  userEvent.type(idInput(), '12');
  reply({ ...trip, version: 5, employeeIds: [9, 12] });
  userEvent.click(screen.getByRole('button', { name: 'Добави към екипажа' }));
  expect(await screen.findByRole('status')).toHaveTextContent('Служителят е добавен');
  const [url, options] = global.fetch.mock.calls[1];
  expect(url).toBe('/api/trips/17/employees/12');
  expect(options.method).toBe('PUT');
  expect(options.body).toBeUndefined();
  expect(within(crewTable()).getByText('12')).toBeInTheDocument();
  expect(screen.getByText('9, 12')).toBeInTheDocument();
  expect(idInput()).toHaveValue(null);
});

test('prevents duplicate assignments locally without sending a mutation', async () => {
  await showCrew();
  userEvent.type(idInput(), '9');
  userEvent.click(screen.getByRole('button', { name: 'Добави към екипажа' }));
  expect(screen.getByRole('alert')).toHaveTextContent('Служителят вече е назначен');
  expect(global.fetch).toHaveBeenCalledTimes(1);
});

test('preserves employee ID on a missing active employee and never invents a crew member', async () => {
  await showCrew();
  userEvent.type(idInput(), '12');
  reply({ detail: 'Active employee not found' }, 404);
  userEvent.click(screen.getByRole('button', { name: 'Добави към екипажа' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('активният служител не е намерен');
  expect(idInput()).toHaveValue(12);
  expect(within(crewTable()).queryByText('12')).not.toBeInTheDocument();
});

test('blocks duplicate clicks and other trip mutations while assignment is pending', async () => {
  await showCrew();
  let resolveAdd;
  global.fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveAdd = resolve; }));
  userEvent.type(idInput(), '12');
  userEvent.click(screen.getByRole('button', { name: 'Добави към екипажа' }));
  const pending = screen.getByRole('button', { name: 'Добавяне…' });
  expect(pending).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Редактирай курса' })).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Деактивирай' })).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Обнови' })).toBeDisabled();
  userEvent.click(pending);
  expect(global.fetch).toHaveBeenCalledTimes(2);
  await act(async () => resolveAdd(response({ ...trip, version: 5, employeeIds: [9, 12] })));
  expect(screen.getByRole('button', { name: 'Редактирай курса' })).toBeEnabled();
});

test('requires removal confirmation, then reads fresh crew/version after a 204 response', async () => {
  await showCrew();
  userEvent.click(screen.getByRole('button', { name: 'Премахни служител #9' }));
  expect(screen.getByRole('button', { name: 'Отказ' })).toHaveFocus();
  userEvent.click(screen.getByRole('button', { name: 'Отказ' }));
  expect(global.fetch).toHaveBeenCalledTimes(1);
  userEvent.click(screen.getByRole('button', { name: 'Премахни служител #9' }));
  reply(null, 204);
  reply({ ...trip, version: 5, employeeIds: [] });
  userEvent.click(screen.getByRole('button', { name: 'Премахни от курса' }));
  expect(await screen.findByText('Няма назначени служители.')).toBeInTheDocument();
  expect(global.fetch.mock.calls[1][0]).toBe('/api/trips/17/employees/9');
  expect(global.fetch.mock.calls[1][1].method).toBe('DELETE');
  expect(global.fetch.mock.calls[2][0]).toBe('/api/trips/17');
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(idInput()).toHaveFocus();
  // Editing still reads the latest record and sends its new version.
  reply({ ...trip, version: 5, employeeIds: [] });
  userEvent.click(screen.getByRole('button', { name: 'Редактирай курса' }));
  await waitFor(() => expect(screen.getByRole('textbox', { name: 'Начална точка' })).toHaveValue('София'));
  reply({ ...trip, version: 6, employeeIds: [] });
  userEvent.click(screen.getByRole('button', { name: 'Запази промените' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  expect(JSON.parse(global.fetch.mock.calls[4][1].body).expectedVersion).toBe(5);
});

test('keeps crew unchanged when DELETE fails and requires refresh after a conflict', async () => {
  await showCrew();
  userEvent.click(screen.getByRole('button', { name: 'Премахни служител #9' }));
  reply({ detail: 'Conflict' }, 409);
  userEvent.click(screen.getByRole('button', { name: 'Премахни от курса' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Конфликт');
  expect(within(crewTable()).getByText('9')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Добави към екипажа' })).toBeDisabled();
  reply({ ...trip, version: 6, employeeIds: [9] });
  userEvent.click(screen.getByRole('button', { name: 'Обнови екипажа' }));
  await waitFor(() => expect(screen.getByRole('button', { name: 'Добави към екипажа' })).toBeEnabled());
  expect(global.fetch).toHaveBeenCalledTimes(3);
});

test('distinguishes successful deletion from failed readback and does not resend DELETE on retry', async () => {
  await showCrew();
  userEvent.click(screen.getByRole('button', { name: 'Премахни служител #9' }));
  reply(null, 204);
  reply({ detail: 'Unavailable' }, 503);
  userEvent.click(screen.getByRole('button', { name: 'Премахни от курса' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Премахването е успешно');
  expect(screen.getByRole('button', { name: 'Премахни служител #9' })).toBeDisabled();
  reply({ ...trip, version: 5, employeeIds: [] });
  userEvent.click(screen.getByRole('button', { name: 'Обнови екипажа' }));
  expect(await screen.findByText('Няма назначени служители.')).toBeInTheDocument();
  expect(global.fetch.mock.calls.map(([, options]) => options.method).filter((method) => method === 'DELETE')).toHaveLength(1);
  expect(screen.getByRole('button', { name: 'Добави към екипажа' })).toBeEnabled();
});

test('ignores a late assignment result after navigating to another course/page', async () => {
  await showCrew();
  let resolveAdd;
  global.fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveAdd = resolve; }));
  userEvent.type(idInput(), '12');
  userEvent.click(screen.getByRole('button', { name: 'Добави към екипажа' }));
  userEvent.click(screen.getByRole('link', { name: '← Към курсовете' }));
  await act(async () => resolveAdd(response({ ...trip, employeeIds: [9, 12] })));
  expect(screen.getByRole('heading', { name: 'Курсове' })).toBeInTheDocument();
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
});
