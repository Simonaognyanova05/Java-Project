import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import AppRoutes from '../../app/AppRoutes';
import { getCustomers } from '../../services/customerApi';
import { createShipment } from '../../services/shipmentApi';

jest.mock('../../services/customerApi');
jest.mock('../../services/shipmentApi');

test('the existing shipment form submits inside a single shared layout', async () => {
  getCustomers.mockResolvedValue([{ id: 1, firstName: 'Иван', lastName: 'Петров', email: 'ivan@example.com' }]);
  createShipment.mockResolvedValue({ id: 12, trackingNumber: 'test-tracking', status: 'CREATED' });
  render(<MemoryRouter initialEntries={['/shipments/new']}><AppRoutes /></MemoryRouter>);
  await screen.findByRole('option', { name: /Иван Петров/ });
  expect(screen.getAllByRole('banner')).toHaveLength(1);
  expect(screen.getAllByRole('main')).toHaveLength(1);
  expect(screen.getByRole('link', { name: 'Към пратки' })).toHaveAttribute('href', '/shipments');
  userEvent.selectOptions(screen.getByLabelText(/Клиент/), '1');
  userEvent.type(screen.getByLabelText(/Име на получателя/), 'Мария Петрова');
  userEvent.type(screen.getByLabelText(/Адрес за доставка/), 'Vienna 1');
  fireEvent.change(screen.getByLabelText(/Тегло/), { target: { value: '10.50' } });
  fireEvent.change(screen.getByLabelText(/Очаквана доставка/), { target: { value: '2030-01-01T12:00' } });
  userEvent.click(screen.getByRole('button', { name: /Създай пратка/ }));
  await waitFor(() => expect(createShipment).toHaveBeenCalledTimes(1));
  expect(createShipment).toHaveBeenCalledWith({
    customerId: 1, recipientName: 'Мария Петрова', deliveryAddress: 'Vienna 1',
    weightKg: 10.5, expectedDeliveryAt: new Date('2030-01-01T12:00').toISOString(),
  });
  expect(await screen.findByRole('status')).toHaveTextContent('test-tracking');
});
