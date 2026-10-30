import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import Button from './Button';
import FormField from './FormField';
import Modal from './Modal';
import DataTable from './DataTable';

test('Button defaults to a non-submit action and blocks clicks while loading', () => {
  const onClick = jest.fn();
  const { rerender } = render(<Button onClick={onClick}>Запази</Button>);
  expect(screen.getByRole('button')).toHaveAttribute('type', 'button');
  userEvent.click(screen.getByRole('button'));
  expect(onClick).toHaveBeenCalledTimes(1);
  rerender(<Button onClick={onClick} loading loadingText="Запазване…">Запази</Button>);
  expect(screen.getByRole('button')).toBeDisabled();
  expect(screen.getByRole('button')).toHaveAttribute('aria-busy', 'true');
  userEvent.click(screen.getByRole('button'));
  expect(onClick).toHaveBeenCalledTimes(1);
});

test('FormField associates unique labels, hints, errors and native validation', () => {
  render(<><FormField label="Име" hint="Пълно име" error="Въведете име" required />
    <FormField label="Адрес" /></>);
  const name = screen.getByRole('textbox', { name: 'Име' });
  expect(name).toBeRequired();
  expect(name).toHaveAttribute('aria-invalid', 'true');
  expect(name).toHaveAccessibleDescription('Пълно име Въведете име');
  expect(screen.getByRole('alert')).toHaveTextContent('Въведете име');
  expect(name.id).not.toBe(screen.getByLabelText('Адрес').id);
  userEvent.type(name, 'Мария');
  expect(name).toHaveValue('Мария');
});

test('FormField supports select, textarea, disabled state and external descriptions', () => {
  render(<><p id="extra">Избор на клиент</p>
    <FormField as="select" label="Клиент" aria-describedby="extra" defaultValue="1" disabled>
      <option value="1">Мария</option>
    </FormField>
    <FormField as="textarea" label="Бележки" defaultValue="Чупливо" />
  </>);
  expect(screen.getByRole('combobox', { name: 'Клиент' })).toBeDisabled();
  expect(screen.getByRole('combobox')).toHaveAccessibleDescription('Избор на клиент');
  expect(screen.getByLabelText('Бележки')).toHaveValue('Чупливо');
});

const columns = [
  { key: 'name', header: 'Клиент' },
  { key: 'weight', header: 'Тегло', align: 'right', render: (row) => row.weight + ' кг' },
  { key: 'email', header: 'Имейл' },
];

test('DataTable renders semantic headers, custom cells, missing values and row actions', () => {
  const edit = jest.fn();
  render(<DataTable caption="Клиенти" columns={[...columns,
    { key: 'actions', header: 'Действия', render: (row) => <Button onClick={() => edit(row.id)}>Редактирай</Button> }
  ]} rows={[{ id: 4, name: 'Мария', weight: 0 }]} />);
  const table = screen.getByRole('table', { name: 'Клиенти' });
  expect(within(table).getAllByRole('columnheader')).toHaveLength(4);
  expect(within(table).getByText('0 кг')).toBeInTheDocument();
  expect(within(table).getByText('—')).toBeInTheDocument();
  userEvent.click(within(table).getByRole('button', { name: 'Редактирай' }));
  expect(edit).toHaveBeenCalledWith(4);
});

test('DataTable distinguishes loading, failure with retry, and an empty result', () => {
  const retry = jest.fn();
  const { rerender } = render(<DataTable columns={columns} loading />);
  expect(screen.getByRole('status')).toHaveTextContent('Зареждане…');
  rerender(<DataTable columns={columns} error="Няма връзка" onRetry={retry} />);
  expect(screen.getByRole('alert')).toHaveTextContent('Няма връзка');
  userEvent.click(screen.getByRole('button', { name: 'Опитай отново' }));
  expect(retry).toHaveBeenCalledTimes(1);
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
  rerender(<DataTable columns={columns} rows={[]} emptyMessage="Няма клиенти." />);
  expect(screen.getByRole('cell')).toHaveAttribute('colspan', '3');
  expect(screen.getByText('Няма клиенти.')).toBeInTheDocument();
});

// jsdom lacks native dialog opening and top-layer focus management.
beforeEach(() => {
  HTMLDialogElement.prototype.showModal = jest.fn(function () {
    this.setAttribute('open', '');
    this.querySelector('button')?.focus();
  });
  HTMLDialogElement.prototype.close = jest.fn(function () { this.removeAttribute('open'); });
});

function ModalExample({ closeOnBackdrop = true }) {
  const [open, setOpen] = useState(false);
  return <><Button onClick={() => setOpen(true)}>Отвори</Button>
    <Modal open={open} onClose={() => setOpen(false)} title="Редакция"
      description="Данни за клиента" closeOnBackdrop={closeOnBackdrop}
      footer={<Button onClick={() => setOpen(false)}>Готово</Button>}>
      <FormField label="Име" data-modal-initial-focus />
    </Modal></>;
}

test('Modal opens with a name, focuses content, traps Tab and restores focus/scroll after Escape', () => {
  render(<ModalExample />);
  const opener = screen.getByRole('button', { name: 'Отвори' });
  userEvent.click(opener);
  const dialog = screen.getByRole('dialog', { name: 'Редакция' });
  expect(dialog).toHaveAccessibleDescription('Данни за клиента');
  expect(screen.getByLabelText('Име')).toHaveFocus();
  expect(document.body.style.overflow).toBe('hidden');
  const first = screen.getByRole('button', { name: 'Затвори прозореца' });
  const last = screen.getByRole('button', { name: 'Готово' });
  last.focus();
  fireEvent.keyDown(last, { key: 'Tab' });
  expect(first).toHaveFocus();
  fireEvent.keyDown(first, { key: 'Tab', shiftKey: true });
  expect(last).toHaveFocus();
  fireEvent(dialog, new Event('cancel', { bubbles: false, cancelable: true }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(opener).toHaveFocus();
  expect(document.body.style.overflow).toBe('');
});

test('Modal dismisses via close button and backdrop, but not content clicks', () => {
  render(<ModalExample />);
  const opener = screen.getByRole('button', { name: 'Отвори' });
  userEvent.click(opener);
  userEvent.click(screen.getByLabelText('Име'));
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  userEvent.click(screen.getByRole('button', { name: 'Затвори прозореца' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  userEvent.click(opener);
  fireEvent.click(screen.getByRole('dialog'), { clientX: -1, clientY: -1 });
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

test('Modal can keep backdrop clicks open and restores scroll when unmounted', () => {
  const { unmount } = render(<ModalExample closeOnBackdrop={false} />);
  userEvent.click(screen.getByRole('button', { name: 'Отвори' }));
  fireEvent.click(screen.getByRole('dialog'), { clientX: -1, clientY: -1 });
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  unmount();
  expect(document.body.style.overflow).toBe('');
});
