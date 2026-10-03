export async function api(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const writes = !['GET', 'HEAD', 'OPTIONS'].includes(method);
  const headers = new Headers(options.headers);
  if (writes) {
    const csrf = await api('/auth/csrf');
    headers.set(csrf.headerName, csrf.token);
  }
  let response;
  try {
    response = await fetch(`/api${path}`, { ...options, method, headers, credentials: 'same-origin', cache: 'no-store' });
  } catch (cause) {
    if (cause.name === 'AbortError') throw cause;
    throw Object.assign(new Error(writes
      ? 'Connection interrupted. Check your history before retrying.'
      : 'The service is unavailable. It may be waking up; try again shortly.'), { status: 0 });
  }
  const json = response.headers.get('content-type')?.includes('application/json');
  let data = null;
  try {
    if (response.status !== 204) {
      if (json) data = await response.json();
      else if (response.ok) throw new Error('Missing JSON response');
    }
  } catch (cause) {
    if (cause.name === 'AbortError') throw cause;
    if (response.ok) throw Object.assign(new Error(writes
      ? 'The response was interrupted. Check your history before retrying.'
      : 'The service response could not be read. Try again shortly.'), { status: 0 });
    // An unreadable error body must not hide its known HTTP status.
  }
  if (!response.ok) {
    let status = response.status;
    if (writes && status === 403) {
      try {
        const me = await fetch('/api/auth/me', { credentials: 'same-origin', cache: 'no-store' });
        if (me.status === 401) status = 401;
      } catch { /* Keep the known rejection when the diagnostic request cannot reach the service. */ }
    }
    const message = status === 401 ? 'Please sign in to continue.'
      : data?.message || 'The service is unavailable. It may be waking up; try again shortly.';
    throw Object.assign(new Error(message), { status, fieldErrors: data?.fieldErrors || {} });
  }
  return data;
}

export async function login(email, password) {
  await api('/auth/login', { method: 'POST', body: new URLSearchParams({ email, password }) });
  return api('/auth/me');
}
export async function logout() { await api('/auth/logout', { method: 'POST' }); }
