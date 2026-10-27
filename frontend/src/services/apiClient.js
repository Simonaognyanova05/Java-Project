const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '/api';

export async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    let message = `Заявката не успя (${response.status}).`;
    try {
      const payload = await response.json();
      message = payload.message || payload.detail || payload.title || message;
    } catch {
      // Keep the HTTP status message when the API has no JSON error body.
    }
    throw new Error(message);
  }

  if (response.status === 204) return null;
  return response.json();
}
