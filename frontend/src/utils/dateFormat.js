export const userTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
const dateTimeFormatter = new Intl.DateTimeFormat('bg-BG', {
  dateStyle: 'medium', timeStyle: 'short', timeZone: userTimeZone,
});

export function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date);
}

// datetime-local has no offset; preserve seconds and milliseconds when editing.
export function toLocalDateTimeInput(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 23);
}
