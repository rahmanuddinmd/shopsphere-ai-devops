import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '' });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const fieldErrors = error?.fieldErrors || {};

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await register(form.fullName, form.email, form.password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  const field = (id, label, type, autoComplete, hint) => (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} className="input" type={type} autoComplete={autoComplete} required value={form[id]}
        aria-invalid={!!fieldErrors[id]} aria-describedby={`${id}-hint`}
        onChange={(e) => setForm({ ...form, [id]: e.target.value })} />
      <span id={`${id}-hint`} className="small" style={{ color: fieldErrors[id] ? 'var(--red)' : 'var(--muted)' }}>
        {fieldErrors[id] || hint}
      </span>
    </div>
  );

  return (
    <main className="container page" style={{ maxWidth: 460 }}>
      <form className="panel stack" onSubmit={submit} noValidate>
        <h1 className="page-title" style={{ fontSize: 32 }}>Create account</h1>
        <ErrorMessage error={Object.keys(fieldErrors).length ? null : error} />
        {field('fullName', 'Full name', 'text', 'name', '')}
        {field('email', 'Email', 'email', 'email', '')}
        {field('password', 'Password', 'password', 'new-password', 'At least 8 characters')}
        <button type="submit" className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</button>
        <p className="muted small">Already have an account? <Link to="/login">Sign in</Link></p>
      </form>
    </main>
  );
}
