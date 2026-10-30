# Shared UI components

These components use Option 2 tokens and have no API dependencies.
Import each component directly from components/ui/ComponentName.
Visible default labels are Bulgarian; documentation is English.

## Button

Variants: primary (default), secondary, danger, ghost.
The default type is button. Explicitly use type="submit" inside forms.
loading disables interaction, exposes aria-busy, and shows loadingText.
disabled, fullWidth, className, native button attributes, and ref are supported.
Icon-only buttons must have an aria-label.

```jsx
<Button type="submit" loading={saving} loadingText="Запазване…">
  Запази
</Button>
```

## FormField

Renders a labeled input by default; as="select" and as="textarea" are supported.
Pass options as children for a select. Native props (name, value, onChange,
required, disabled, min, max, etc.) go to the control, as does the forwarded ref.
id is optional and defaults to React useId. hint and error are linked via
aria-describedby; errors set aria-invalid and render an alert.
An external aria-describedby is preserved alongside generated descriptions.
leading/trailing are decorative, non-interactive adornments. Include essential
units or meaning in the label or hint too. className styles the wrapper;
controlClassName styles the input. This is a text/select/textarea field,
not a checkbox, radio group, or file-upload component.

```jsx
<FormField label="Тегло (кг)" name="weightKg" type="number"
  min="0.01" step="0.01" value={weight} onChange={onWeightChange}
  error={errors.weightKg} hint="Въведете теглото в килограми." required />
```

## Modal

Controlled with open and onClose. title is required; description, children and
footer accept content. The native dialog is portaled to document.body and
opened with showModal, giving it top-layer rendering and an inert background.
Modern browsers with HTMLDialogElement.showModal are required.
Escape, the close button and backdrop request onClose; the parent sets open=false.
closeOnBackdrop={false} disables backdrop dismissal.
Focus stays inside the dialog and returns to the opener after closing/unmount.
Body scrolling is locked while open. Add data-modal-initial-focus to the desired
initial focus target; otherwise the native dialog focuses its first control.
Use one application modal at a time. Actions and submission state are owned by
the feature; the modal does not save, delete or close itself after API requests.

```jsx
<Modal open={editing} onClose={() => setEditing(false)} title="Редакция"
  footer={<Button onClick={save} loading={saving}>Запази</Button>}>
  <FormField label="Име" value={name} onChange={onNameChange}
    data-modal-initial-focus />
</Modal>
```

## DataTable

Pass a non-empty columns array and rows. Columns have a unique key, header,
optional align="right", and optional render(row) for formatted data/actions.
Without render, the value is row[column.key]; only null/undefined display an em
dash, preserving zero and false values. Render booleans explicitly as UI labels.
rowKey defaults to "id" and can be a property name or function returning a stable,
unique key. Never use array position for records that can change.
caption names the semantic table and its keyboard-focusable horizontal scroller.
loading, error, onRetry and emptyMessage distinguish request states; loading and
error hide rows rather than presenting old data as current.
This component does not fetch, sort or paginate: feature pages own server requests,
filters and pagination and pass the current rows.

```jsx
<DataTable caption="Клиенти" rows={customers} loading={loading}
  error={error} onRetry={reload}
  columns={[
    { key: 'name', header: 'Име',
      render: (row) => row.firstName + ' ' + row.lastName },
    { key: 'email', header: 'Имейл' },
    { key: 'actions', header: 'Действия',
      render: (row) => <Button variant="secondary"
        onClick={() => edit(row.id)}>Редактирай</Button> },
  ]} />
```

Button and FormField are already used by the shipment creation page. Modal and
DataTable are ready for upcoming CRUD screens; no mock CRUD or production demo
route is introduced. SharedUi.test.jsx covers behavior, accessible associations,
table states/actions and modal lifecycle. The existing form regression test
continues to verify the backend payload through mocked services.
