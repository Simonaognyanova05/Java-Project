const API_BASE_URL = (process.env.REACT_APP_API_BASE_URL?.trim() || '/api').replace(/\/+$/, '');

export class ApiError extends Error {
  constructor(message, { status = 0, data = null, cause } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status; // 0 means no HTTP response (network/read failure).
    this.data = data;
    this.fieldErrors = data?.errors ?? data?.fieldErrors ?? null;
    if (cause) this.cause = cause;
  }
}

export async function apiRequest(path, options = {}) {
  const headers = new Headers(options.headers);
  if (!headers.has('Accept')) headers.set('Accept', 'application/json');
  // Services serialize JSON themselves; let the browser set FormData boundaries.
  if (typeof options.body === 'string' && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  let response;
  let text;
  try {
    response = await fetch(API_BASE_URL + '/' + path.replace(/^\/+/, ''), { ...options, headers });
    if (response.status !== 204 && response.status !== 205) text = await response.text();
  } catch (cause) {
    if (cause.name === 'AbortError' || options.signal?.aborted) throw cause;
    throw new ApiError('Няма връзка със сървъра. Опитайте отново.', { cause });
  }

  let data = null;
  if (text?.trim()) {
    try { data = JSON.parse(text); } catch {
      // Never display an HTML proxy/server error page as an application message.
      if (response.ok) {
        throw new ApiError('Сървърът върна неочакван отговор.', { status: response.status });
      }
    }
  }
  if (!response.ok) {
    const message = [data?.message, data?.detail, data?.title]
      .find((value) => typeof value === 'string' && value.trim())
      || `Заявката не успя (${response.status}).`;
    throw new ApiError(message, { status: response.status, data });
  }
  return data;
}
