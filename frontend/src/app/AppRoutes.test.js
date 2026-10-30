import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router-dom';
import AppRoutes from './AppRoutes';

jest.mock('../features/shipments/CreateShipmentPage', () => {
  return function CreateShipmentRoute() {
    return <h1>Създаване на пратка</h1>;
  };
});

function CurrentPath() {
  return <output aria-label="Current path">{useLocation().pathname}</output>;
}

jest.mock('../features/customers/CustomersPage', () => {
  return function CustomersRoute() { return <h1>Клиенти</h1>; };
});

function renderRoute(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AppRoutes />
      <CurrentPath />
    </MemoryRouter>
  );
}

test('redirects the root to the dashboard', () => {
  renderRoute('/');
  expect(screen.getByRole('heading', { name: 'Обзор' })).toBeInTheDocument();
  expect(screen.getByLabelText('Current path')).toHaveTextContent('/dashboard');
});

test('navigates between feature routes without reloading the page', () => {
  renderRoute('/dashboard');
  userEvent.click(screen.getByRole('link', { name: 'Курсове' }));
  expect(screen.getByRole('heading', { name: 'Курсове' })).toBeInTheDocument();
  expect(screen.getByLabelText('Current path')).toHaveTextContent('/trips');
  expect(screen.getByRole('link', { name: 'Курсове' })).toHaveAttribute('aria-current', 'page');
});

test.each([
  ['/shipments/new', 'Създаване на пратка'],
  ['/shipments/42', 'Пратка #42'],
  ['/trips/17', 'Курс #17'],
  ['/customers', 'Клиенти'],
  ['/employees', 'Екипаж'],
  ['/vehicles', 'Автопарк'],
])('resolves the direct route %s', (path, heading) => {
  renderRoute(path);
  expect(screen.getByRole('heading', { name: heading })).toBeInTheDocument();
});

test('provides a working recovery link for an unknown route', () => {
  renderRoute('/missing/page');
  expect(screen.getByRole('heading', { name: 'Страницата не е намерена' })).toBeInTheDocument();
  userEvent.click(screen.getByRole('link', { name: 'Към обзора →' }));
  expect(screen.getByRole('heading', { name: 'Обзор' })).toBeInTheDocument();
});
