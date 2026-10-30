import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CustomersPage from './CustomersPage';

const maria = { id: 1, firstName: 'Мария', lastName: 'Петрова', email: 'maria@example.com', phone: null };
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
function reply(data, status = 200) { global.fetch.mockResolvedValueOnce(response(data, status)); }
function table() { return screen.getByRole('table', { name: 'Списък с клиенти' }); }
async function showCustomers() {
  reply([maria]);
  render(<CustomersPage />);
  await screen.findByRole('table');
}
function fillCreate() {
  userEvent.type(screen.getByRole('textbox', { name: 'Име' }), ' Иван ');
  userEvent.type(screen.getByRole('textbox', { name: 'Фамилия' }), ' Иванов ');
  userEvent.type(screen.getByRole('textbox', { name: 'Имейл' }), 'ivan@example.com');
}
function submitCustomer() { userEvent.click(screen.getByRole('button', { name: 'Създай клиент' })); }

test('loads actual customer rows, optional phone and uniquely named row actions', async () => {
  await showCustomers();
  expect(global.fetch.mock.calls[0][0]).toBe('/api/customers');
  expect(within(table()).getByText('Мария Петрова')).toBeInTheDocument();
  expect(within(table()).getByText('maria@example.com')).toBeInTheDocument();
  expect(within(table()).getByText('—')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Редактирай Мария Петрова' })).toBeEnabled();
});

test('distinguishes initial loading, request failure, retry, and an empty list', async () => {
  reply({ detail: 'Сървърът не е достъпен' }, 503);
  render(<CustomersPage />);
  expect(screen.getByRole('status')).toHaveTextContent('Зареждане');
  expect(await screen.findByRole('alert')).toHaveTextContent('Сървърът не е достъпен');
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
  reply([]);
  userEvent.click(screen.getByRole('button', { name: 'Опитай отново' }));
  expect(await screen.findByText(/Все още няма клиенти/)).toBeInTheDocument();
});

test('creates once, trims fields, preserves optional phone and displays the server response', async () => {
  await showCustomers();
  userEvent.click(screen.getByRole('button', { name: 'Нов клиент' }));
  fillCreate();
  let resolveCreate;
  global.fetch.mockImplementationOnce(() => new Promise((resolve) => { resolveCreate = resolve; }));
  submitCustomer();
  expect(screen.getByRole('button', { name: 'Запазване…' })).toBeDisabled();
  userEvent.click(screen.getByRole('button', { name: 'Запазване…' }));
  expect(global.fetch).toHaveBeenCalledTimes(2);
  const [url, options] = global.fetch.mock.calls[1];
  expect(url).toBe('/api/customers');
  expect(options.method).toBe('POST');
  expect(JSON.parse(options.body)).toEqual({
    firstName: 'Иван', lastName: 'Иванов', email: 'ivan@example.com', phone: '',
  });
  const dialog = screen.getByRole('dialog');
  fireEvent(dialog, new Event('cancel', { cancelable: true }));
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  await act(async () => resolveCreate(response({
    id: 7, firstName: 'Иван', lastName: 'Иванов', email: 'ivan@example.com', phone: '',
  }, 201)));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(within(table()).getByText('Иван Иванов')).toBeInTheDocument();
  expect(screen.getByRole('status')).toHaveTextContent('Клиентът е създаден.');
});

test('loads detail before editing and keeps input on email conflict before a successful PUT', async () => {
  await showCustomers();
  reply({ ...maria, phone: '123' });
  userEvent.click(screen.getByRole('button', { name: 'Редактирай Мария Петрова' }));
  await waitFor(() => expect(screen.getByLabelText('Телефон')).toHaveValue('123'));
  expect(global.fetch.mock.calls[1][0]).toBe('/api/customers/1');
  userEvent.clear(screen.getByRole('textbox', { name: 'Име' }));
  userEvent.type(screen.getByRole('textbox', { name: 'Име' }), 'Мая');
  reply({ detail: 'Duplicate email' }, 409);
  userEvent.click(screen.getByRole('button', { name: 'Запази промените' }));
  await screen.findByText('Този имейл вече се използва.');
  expect(screen.getByRole('textbox', { name: 'Име' })).toHaveValue('Мая');
  expect(screen.getByRole('textbox', { name: 'Имейл' })).toHaveAttribute('aria-invalid', 'true');
  expect(global.fetch.mock.calls[2][1].method).toBe('PUT');
  expect(JSON.parse(global.fetch.mock.calls[2][1].body)).toEqual({
    firstName: 'Мая', lastName: 'Петрова', email: 'maria@example.com', phone: '123',
  });
  reply({ ...maria, firstName: 'Мая', phone: '123' });
  userEvent.click(screen.getByRole('button', { name: 'Запази промените' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  expect(within(table()).getByText('Мая Петрова')).toBeInTheDocument();
});

test('requires delete confirmation, keeps rows on failure and removes only after server success', async () => {
  await showCustomers();
  userEvent.click(screen.getByRole('button', { name: 'Изтрий Мария Петрова' }));
  expect(screen.getByRole('button', { name: 'Отказ' })).toHaveFocus();
  userEvent.click(screen.getByRole('button', { name: 'Отказ' }));
  expect(global.fetch).toHaveBeenCalledTimes(1);
  userEvent.click(screen.getByRole('button', { name: 'Изтрий Мария Петрова' }));
  reply({ detail: 'Свързани пратки' }, 409);
  userEvent.click(screen.getByRole('button', { name: 'Изтрий клиента' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Клиентът не може да бъде изтрит');
  expect(within(table()).getByText('Мария Петрова')).toBeInTheDocument();
  reply(null, 204);
  userEvent.click(screen.getByRole('button', { name: 'Изтрий клиента' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  expect(global.fetch.mock.calls[2][0]).toBe('/api/customers/1');
  expect(global.fetch.mock.calls[2][1].method).toBe('DELETE');
  expect(screen.getByRole('status')).toHaveTextContent('Клиентът е изтрит.');
  expect(screen.getByRole('button', { name: 'Нов клиент' })).toHaveFocus();
  expect(within(table()).queryByText('Мария Петрова')).not.toBeInTheDocument();
});

test('handles an edit detail 404 with list refresh instead of a blank editing form', async () => {
  await showCustomers();
  reply({ detail: 'Not found' }, 404);
  userEvent.click(screen.getByRole('button', { name: 'Редактирай Мария Петрова' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Клиентът вече не съществува.');
  expect(screen.queryByRole('textbox', { name: 'Име' })).not.toBeInTheDocument();
  reply([]);
  userEvent.click(screen.getByRole('button', { name: 'Обнови списъка' }));
  await screen.findByText(/Все още няма клиенти/);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

test('validates whitespace and displays backend field validation without losing the form', async () => {
  await showCustomers();
  userEvent.click(screen.getByRole('button', { name: 'Нов клиент' }));
  fillCreate();
  userEvent.clear(screen.getByRole('textbox', { name: 'Име' }));
  userEvent.type(screen.getByRole('textbox', { name: 'Име' }), '   ');
  submitCustomer();
  expect(screen.getByRole('alert')).toHaveTextContent('Въведете име.');
  expect(global.fetch).toHaveBeenCalledTimes(1);
  userEvent.clear(screen.getByRole('textbox', { name: 'Име' }));
  userEvent.type(screen.getByRole('textbox', { name: 'Име' }), 'Иван');
  reply({ message: 'Невалидни данни', errors: { phone: 'Невалиден телефон' } }, 400);
  submitCustomer();
  expect(await screen.findByText('Невалиден телефон')).toBeInTheDocument();
  expect(screen.getByRole('textbox', { name: 'Фамилия' })).toHaveValue(' Иванов ');
});

test('cancels list loading on unmount', () => {
  global.fetch.mockImplementation(() => new Promise(() => {}));
  const { unmount } = render(<CustomersPage />);
  const signal = global.fetch.mock.calls[0][1].signal;
  expect(signal.aborted).toBe(false);
  unmount();
  expect(signal.aborted).toBe(true);
});
