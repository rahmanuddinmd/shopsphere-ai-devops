import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { rupees, specLine } from '../api/format.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { CartIcon } from './Icons.jsx';

export default function ProductCard({ product }) {
  const { user } = useAuth();
  const { add } = useCart();
  const navigate = useNavigate();
  const [state, setState] = useState('idle');

  async function addToCart() {
    if (!user) {
      navigate('/login', { state: { from: `/products/${product.id}` } });
      return;
    }
    setState('busy');
    try {
      await add(product.id, 1);
      setState('added');
      setTimeout(() => setState('idle'), 1500);
    } catch {
      setState('error');
    }
  }

  return (
    <article className="card product-card">
      <Link to={`/products/${product.id}`} className="media">
        <img src={product.imageUrl || '/images/hero-desk.jpg'} alt={product.name} loading="lazy" />
      </Link>
      <div className="body">
        <div className="row between" style={{ gap: 8, minHeight: 24 }}>
          <span className="eyebrow">{product.category?.name}</span>
          {product.badge && <span className="pill pill-saffron">{product.badge}</span>}
        </div>
        <h3><Link to={`/products/${product.id}`}>{product.name}</Link></h3>
        <p className="muted" style={{ fontSize: 14 }}>{specLine(product)}</p>
        {!product.inStock && <span className="pill pill-red" style={{ alignSelf: 'flex-start' }}>Out of stock</span>}
        <div className="foot">
          <span className="stack-sm" style={{ gap: 0, lineHeight: 1.15 }}>
            <span className="price" style={{ fontSize: 21 }}>{rupees(product.price)}</span>
            {product.mrp && <span className="strike small">{rupees(product.mrp)}</span>}
          </span>
          <button type="button" className="btn btn-primary btn-sm" onClick={addToCart}
            disabled={!product.inStock || state === 'busy'} aria-label={`Add ${product.name} to cart`}>
            <CartIcon size={18} /> {state === 'added' ? 'Added' : state === 'error' ? 'Retry' : 'Add'}
          </button>
        </div>
      </div>
    </article>
  );
}
