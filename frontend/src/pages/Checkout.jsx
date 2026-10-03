import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client.js';
import { rupees } from '../api/format.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';

export default function Checkout() {
  const { user } = useAuth();
  const { cart, clearLocal } = useCart();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    fullName: user?.fullName || '', phone: user?.phone || '', address: '', city: '', state: '', pin: '',
    paymentMethod: 'CASH_ON_DELIVERY',
  });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const fieldErrors = error?.fieldErrors || {};

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const order = await api.placeOrder(form);
      clearLocal();
      navigate('/orders', { state: { placed: order.orderNumber } });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  if (cart.items.length === 0) {
    return (
      <main className="container page stack">
        <h1 className="page-title">Checkout</h1>
        <p className="muted">Your cart is empty.</p>
        <Link to="/products" className="btn btn-primary" style={{ alignSelf: 'flex-start' }}>Browse products</Link>
      </main>
    );
  }

  const input = (id, label, props = {}) => (
    <div className={`field ${props.span ? 'span-all' : ''}`}>
      <label htmlFor={id}>{label}</label>
      <input id={id} className="input" required value={form[id]} aria-invalid={!!fieldErrors[id]}
        onChange={(e) => setForm({ ...form, [id]: e.target.value })} {...props.attrs} />
      {fieldErrors[id] && <span className="small" style={{ color: 'var(--red)' }}>{fieldErrors[id]}</span>}
    </div>
  );

  return (
    <main className="container page">
      <h1 className="page-title" style={{ marginBottom: 24 }}>Checkout</h1>
      <form className="with-sidebar" onSubmit={submit} noValidate>
        <div className="main-col stack" style={{ gap: 24 }}>
          <ErrorMessage error={Object.keys(fieldErrors).length ? 'Please fix the highlighted fields.' : error} />
          <section className="panel stack" aria-labelledby="addr-h">
            <h2 id="addr-h" style={{ fontSize: 20 }}>Delivery address</h2>
            <div className="form-grid">
              {input('fullName', 'Full name', { attrs: { autoComplete: 'name' } })}
              {input('phone', 'Mobile number', { attrs: { type: 'tel', autoComplete: 'tel' } })}
              {input('address', 'Address', { span: true, attrs: { autoComplete: 'street-address', placeholder: 'House no., street, area' } })}
              {input('city', 'City', { attrs: { autoComplete: 'address-level2' } })}
              {input('state', 'State', { attrs: { autoComplete: 'address-level1' } })}
              {input('pin', 'PIN code', { attrs: { inputMode: 'numeric', maxLength: 6, autoComplete: 'postal-code' } })}
            </div>
          </section>
          <fieldset className="panel stack">
            <legend style={{ fontSize: 20, fontFamily: 'var(--font-head)', padding: 0 }}>Payment method</legend>
            <label className="radio-card">
              <input type="radio" name="pay" checked={form.paymentMethod === 'CASH_ON_DELIVERY'}
                onChange={() => setForm({ ...form, paymentMethod: 'CASH_ON_DELIVERY' })} />
              <span><b>Cash on delivery</b><br /><span className="muted small">Pay when your order arrives.</span></span>
            </label>
            <label className="radio-card">
              <input type="radio" name="pay" checked={form.paymentMethod === 'DEMO_CARD'}
                onChange={() => setForm({ ...form, paymentMethod: 'DEMO_CARD' })} />
              <span><b>Demo card payment</b><br /><span className="muted small">Simulated in this training store — no real charge.</span></span>
            </label>
          </fieldset>
        </div>
        <aside className="panel panel-surface sidebar stack" aria-label="Order summary">
          <h2 style={{ fontSize: 20 }}>Order summary</h2>
          {cart.items.map((i) => (
            <div key={i.id} className="summary-row small"><span>{i.productName} × {i.quantity}</span><span>{rupees(i.lineTotal)}</span></div>
          ))}
          <div className="summary-row"><span className="muted">Delivery</span><span>Free</span></div>
          <hr style={{ border: 0, borderTop: '1px solid var(--line)', width: '100%' }} />
          <div className="summary-row summary-total"><span>Total</span><span className="price">{rupees(cart.total)}</span></div>
          <button type="submit" className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Placing order…' : 'Place order'}</button>
        </aside>
      </form>
    </main>
  );
}
