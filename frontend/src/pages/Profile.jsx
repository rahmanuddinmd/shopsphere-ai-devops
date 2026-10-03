import { useState } from 'react';
import { api } from '../api/client.js';
import { formatDate } from '../api/format.js';
import { useAuth } from '../context/AuthContext.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';

export default function Profile() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({ fullName: user.fullName, phone: user.phone || '' });
  const [status, setStatus] = useState(null);
  const [error, setError] = useState(null);

  async function submit(e) {
    e.preventDefault();
    setError(null);
    setStatus(null);
    try {
      setUser(await api.updateProfile(form));
      setStatus('Profile saved.');
    } catch (err) {
      setError(err);
    }
  }

  return (
    <main className="container page" style={{ maxWidth: 640 }}>
      <form className="panel stack" onSubmit={submit}>
        <h1 className="page-title" style={{ fontSize: 32 }}>My profile</h1>
        <p className="muted small">{user.email} · member since {formatDate(user.createdAt)} · {user.roles.join(', ')}</p>
        <ErrorMessage error={error} />
        {status && <div className="alert alert-success" role="status">{status}</div>}
        <div className="field">
          <label htmlFor="fullName">Full name</label>
          <input id="fullName" className="input" value={form.fullName} required onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </div>
        <div className="field">
          <label htmlFor="phone">Mobile number</label>
          <input id="phone" className="input" type="tel" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        </div>
        <button type="submit" className="btn btn-primary" style={{ alignSelf: 'flex-start' }}>Save changes</button>
      </form>
    </main>
  );
}
