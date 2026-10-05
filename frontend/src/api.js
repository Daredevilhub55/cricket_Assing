export const API = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export async function request(path, options = {}) {
  const response = await fetch(`${API}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });

  if (!response.ok) {
    let message = 'Something went wrong';
    try {
      const body = await response.json();
      message = body.message || body.detail || message;
    } catch {
      // Keep the fallback message when the server returns a non-JSON error.
    }
    throw new Error(message);
  }

  return response.status === 204 ? null : response.json();
}
