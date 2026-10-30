import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import AppRoutes from '../../app/AppRoutes';

jest.mock('../../features/shipments/CreateShipmentPage', () => {
  return function CreateShipmentRoute() { return <h1>Създаване на пратка</h1>; };
});

const originalMatchMedia = window.matchMedia;
afterEach(() => { window.matchMedia = originalMatchMedia; });

jest.mock('../../features/customers/CustomersPage', () => {
  return function CustomersRoute() { return <h1>Клиенти</h1>; };
});

function renderRoute(path) {
  return render(<MemoryRouter initialEntries={[path]}><AppRoutes /></MemoryRouter>);
}

function mobileViewport() {
  let changeListener;
  const media = {
    matches: true,
    addEventListener: jest.fn((event, listener) => { changeListener = listener; }),
    removeEventListener: jest.fn(),
  };
  window.matchMedia = jest.fn(() => media);
  return () => { act(() => { media.matches = false; changeListener(); }); };
}

test('uses one shared shell and keeps the shipment section active on detail routes', () => {
  renderRoute('/shipments/42');
  expect(screen.getAllByRole('banner')).toHaveLength(1);
  expect(screen.getAllByRole('main')).toHaveLength(1);
  const navigation = screen.getByRole('navigation', { name: 'Основна навигация' });
  expect(within(navigation).getByRole('link', { name: 'Пратки' })).toHaveAttribute('aria-current', 'page');
  const breadcrumbs = screen.getByRole('navigation', { name: 'Път до текущата страница' });
  expect(within(breadcrumbs).getByRole('link', { name: 'Пратки' })).toHaveAttribute('href', '/shipments');
  expect(breadcrumbs).toHaveTextContent('Пратка #42');
  expect(document.title).toBe('Пратка #42 · Smart Shipment Tracking');
});

test('mobile navigation traps focus, closes with Escape, and restores focus and scrolling', () => {
  mobileViewport();
  renderRoute('/dashboard');
  const menuButton = screen.getByRole('button', { name: 'Отвори навигацията' });
  expect(screen.queryByRole('navigation', { name: 'Основна навигация' })).not.toBeInTheDocument();
  userEvent.click(menuButton);
  const dialog = screen.getByRole('dialog', { name: 'Основно меню' });
  expect(menuButton).toHaveAttribute('aria-expanded', 'true');
  expect(within(dialog).getByRole('button', { name: 'Затвори навигацията' })).toHaveFocus();
  expect(document.body.style.overflow).toBe('hidden');
  const links = within(dialog).getAllByRole('link');
  links[links.length - 1].focus();
  userEvent.tab();
  expect(links[0]).toHaveFocus();
  userEvent.keyboard('{Escape}');
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(menuButton).toHaveAttribute('aria-expanded', 'false');
  expect(menuButton).toHaveFocus();
  expect(document.body.style.overflow).toBe('');
});

test('selecting a route from the mobile drawer navigates and closes the drawer', () => {
  mobileViewport();
  renderRoute('/dashboard');
  userEvent.click(screen.getByRole('button', { name: 'Отвори навигацията' }));
  userEvent.click(within(screen.getByRole('dialog')).getByRole('link', { name: 'Клиенти' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Клиенти' })).toBeInTheDocument();
  expect(document.body.style.overflow).toBe('');
});

test('resizing to desktop removes the modal and leaves the persistent navigation usable', () => {
  const resizeToDesktop = mobileViewport();
  renderRoute('/trips');
  userEvent.click(screen.getByRole('button', { name: 'Отвори навигацията' }));
  resizeToDesktop();
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Отвори навигацията' })).not.toBeInTheDocument();
  expect(screen.getByRole('navigation', { name: 'Основна навигация' })).toBeInTheDocument();
  expect(document.body.style.overflow).toBe('');
});
