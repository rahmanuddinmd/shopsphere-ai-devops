import { useState } from 'react';
import { Link } from 'react-router-dom';
import { rupees } from '../api/format.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import CartItem from '../components/CartItem.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';

export default function Cart() {
  const { user } = useAuth();
  const { cart, update, remove } = useCart();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  async function run(fn) {
    setBusy(true);
    setError(null);
    try {
      await fn();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  if (!user) {
    return (
      <main className="container page stack" style={{ maxWidth: 560 }}>
        <h1 className="page-title">Your cart</h1>
        <p className="muted">Sign in to see your cart and check out.</p>
        <Link to="/login" state={{ from: '/cart' }} className="btn btn-primary" style={{ alignSelf: 'flex-start' }}>Sign in</Link>
      </main>
    );
  }

  return (
    <main className="container page">
      <h1 className="page-title" style={{ marginBottom: 24 }}>Your cart</h1>
      <ErrorMessage error={error} />
      {cart.items.length === 0 ? (
        <div className="panel stack" style={{ alignItems: 'flex-start' }}>
          <p>Your cart is empty.</p>
          <Link to="/products" className="btn btn-primary">Browse products</Link>
        </div>
      ) : (
        <div className="with-sidebar">
          <section aria-label="Cart items" className="main-col panel" style={{ padding: '4px 20px 8px' }}>
            {cart.items.map((item) => (
              <CartItem key={item.id} item={item} busy={busy}
                onQuantity={(id, q) => run(() => update(id, q))}
                onRemove={(id) => run(() => remove(id))} />
            ))}
            <Link to="/products" className="row" style={{ minHeight: 44, fontWeight: 600 }}>← Continue shopping</Link>
          </section>
          <aside aria-label="Order summary" className="panel panel-surface sidebar stack">
            <h2 style={{ fontSize: 20 }}>Order summary</h2>
            <div className="summary-row"><span className="muted">Subtotal ({cart.itemCount} items)</span><span>{rupees(cart.total)}</span></div>
            <div className="summary-row"><span className="muted">Delivery</span><span>Free</span></div>
            <div className="summary-row"><span className="muted">GST</span><span>Included</span></div>
            <hr style={{ border: 0, borderTop: '1px solid var(--line)', width: '100%' }} />
            <div className="summary-row summary-total"><span>Total</span><span className="price">{rupees(cart.total)}</span></div>
            <Link to="/checkout" className="btn btn-primary btn-block">Proceed to checkout</Link>
          </aside>
        </div>
      )}
    </main>
  );
}
