import { useEffect, useState } from 'react';
import { api, logout } from './api.js';
import Login from './pages/Login.jsx';

export default function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    let active = true;
    api('/auth/me').then(actor => { if (active) setUser(actor); })
      .catch(failure => { if (active && failure.status !== 401) setError(failure.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);
  async function signOut() {
    try { await logout(); setUser(null); setError(''); }
    catch (failure) { if (failure.status === 401) setUser(null); else setError(failure.message); }
  }
  return <main>
    {error && <p className="error" role="alert">{error}</p>}
    {loading ? <p role="status">Connecting to site safety…</p> : !user
      ? <Login onSignedIn={actor => { setUser(actor); setError(''); }} />
      : <section className="panel"><p className="eyebrow">RAS • Site safety</p><h1>Hello, {user.name}</h1>
        <p>Signed in as {user.role === 'ADMIN' ? 'Admin' : 'Framer'}.</p><button onClick={signOut}>Sign out</button></section>}
  </main>;
}
