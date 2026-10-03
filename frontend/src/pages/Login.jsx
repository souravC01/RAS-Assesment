import { useState } from 'react';
import { login } from '../api.js';

export default function Login({ onSignedIn, inline = false }) {
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  async function submit(event) {
    event.preventDefault();
    const fields = new FormData(event.currentTarget);
    setPending(true); setError('');
    try { onSignedIn(await login(fields.get('email').trim(), fields.get('password'))); }
    catch (failure) { setError(failure.message); }
    finally { setPending(false); }
  }
  return <section className={inline ? 'panel' : 'login panel'} aria-labelledby="login-heading">
    <p className="eyebrow">RAS • Site safety</p>
    <h1 id="login-heading">{inline ? 'Sign in to continue' : 'Welcome back'}</h1>
    <p>Record your daily safety checks and supporting photos.</p>
    <form onSubmit={submit}>
      <label>Email<input name="email" type="email" autoComplete="username" required /></label>
      <label>Password<input name="password" type="password" autoComplete="current-password" required /></label>
      {error && <p className="error" role="alert">{error}</p>}
      <button disabled={pending}>{pending ? 'Signing in…' : 'Sign in'}</button>
      {pending && <p role="status">The service may need a moment to wake up.</p>}
    </form>
  </section>;
}
