import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { rupees } from '../api/format.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import Loading from '../components/Loading.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';
import ProductGrid from '../components/ProductGrid.jsx';
import { CartIcon, SparkIcon } from '../components/Icons.jsx';

const VIEWS = ['center', '20% 60%', '80% 60%', '50% 85%'];

export default function ProductDetails() {
  const { id } = useParams();
  const { user } = useAuth();
  const { add } = useCart();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [related, setRelated] = useState([]);
  const [error, setError] = useState(null);
  const [qty, setQty] = useState(1);
  const [view, setView] = useState(0);
  const [notice, setNotice] = useState(null);

  useEffect(() => {
    setProduct(null);
    setError(null);
    setQty(1);
    setView(0);
    api.product(id)
      .then((p) => {
        setProduct(p);
        return api.products({ category: p.category.slug, size: 5 });
      })
      .then((r) => setRelated(r.content.filter((x) => String(x.id) !== String(id)).slice(0, 4)))
      .catch(setError);
  }, [id]);

  async function addToCart(goToCart) {
    if (!user) {
      navigate('/login', { state: { from: `/products/${id}` } });
      return;
    }
    try {
      await add(product.id, qty);
      if (goToCart) navigate('/cart');
      else setNotice({ type: 'success', text: `${product.name} added to your cart.` });
    } catch (e) {
      setNotice({ type: 'error', text: e.message });
    }
  }

  if (error) return <main className="container page"><ErrorMessage error={error} /></main>;
  if (!product) return <Loading />;

  const save = product.mrp ? product.mrp - product.price : 0;
  const specs = [
    ['Brand', product.brand],
    ['Processor', product.processor],
    ['Memory', product.ramGb && `${product.ramGb} GB`],
    ['Storage', product.storageGb && `${product.storageGb} GB`],
    ['Display', product.display],
    ['SKU', product.sku],
  ].filter(([, v]) => v);

  return (
    <main className="container page">
      <nav aria-label="Breadcrumb" className="small muted row" style={{ gap: 8 }}>
        <Link to="/">Home</Link><span aria-hidden="true">/</span>
        <Link to={`/products?category=${product.category.slug}`}>{product.category.name}</Link><span aria-hidden="true">/</span>
        <span>{product.name}</span>
      </nav>
      <div className="row" style={{ gap: 40, marginTop: 20, alignItems: 'flex-start' }}>
        <div className="stack" style={{ flex: '1 1 440px', gap: 12 }}>
          <img className="gallery-main" src={product.imageUrl} alt={product.name} style={{ objectPosition: VIEWS[view] }} />
          <div className="thumbs">
            {VIEWS.map((pos, i) => (
              <button key={pos} type="button" className="thumb" aria-pressed={view === i} aria-label={`Show view ${i + 1}`} onClick={() => setView(i)}>
                <img src={product.imageUrl} alt="" style={{ objectPosition: pos }} />
              </button>
            ))}
          </div>
        </div>
        <div className="stack" style={{ flex: '1 1 400px' }}>
          <div className="row" style={{ gap: 8 }}>
            {product.badge && <span className="pill pill-saffron">{product.badge}</span>}
            <span className={`pill ${product.inStock ? 'pill-green' : 'pill-red'}`}>
              {product.inStock ? (product.stockQuantity <= 5 ? `Only ${product.stockQuantity} left` : 'In stock') : 'Out of stock'}
            </span>
          </div>
          <h1 className="page-title" style={{ fontSize: 42, lineHeight: 1.1 }}>{product.name}</h1>
          <p className="muted" style={{ fontSize: 17 }}>{product.description}</p>
          <div className="row" style={{ alignItems: 'baseline', gap: 12 }}>
            <span className="price" style={{ fontSize: 38 }}>{rupees(product.price)}</span>
            {product.mrp && <span className="strike" style={{ fontSize: 18 }}>{rupees(product.mrp)}</span>}
            {save > 0 && <span className="pill pill-green">Save {rupees(save)}</span>}
          </div>
          <span className="muted small">Inclusive of GST · Free delivery</span>
          {notice && <div className={`alert ${notice.type === 'error' ? 'alert-error' : 'alert-success'}`} role="status">{notice.text}</div>}
          <div className="row" style={{ gap: 12 }}>
            <div className="qty">
              <button type="button" aria-label="Decrease quantity" onClick={() => setQty((q) => Math.max(1, q - 1))}>−</button>
              <span aria-live="polite">{qty}</span>
              <button type="button" aria-label="Increase quantity" onClick={() => setQty((q) => Math.min(10, product.stockQuantity, q + 1))}>+</button>
            </div>
            <button type="button" className="btn btn-primary" style={{ flex: '1 1 180px' }} disabled={!product.inStock} onClick={() => addToCart(false)}>
              <CartIcon size={20} /> Add to cart
            </button>
            <button type="button" className="btn btn-secondary" style={{ flex: '1 1 140px' }} disabled={!product.inStock} onClick={() => addToCart(true)}>
              Buy now
            </button>
          </div>
          <Link to={`/assistant?q=${encodeURIComponent(`Is ${product.name} good for programming?`)}`} className="panel row"
            style={{ border: '1px dashed var(--primary)', gap: 12, textDecoration: 'none', color: 'var(--ink)', flexWrap: 'nowrap' }}>
            <span className="brand-mark" style={{ background: 'var(--primary-soft)', color: 'var(--primary)', width: 40, height: 40 }}><SparkIcon size={20} /></span>
            <span><b>Ask AI about this product</b><br /><span className="muted small">“Is {product.name} good for programming?”</span></span>
          </Link>
          {specs.length > 0 && (
            <div className="card" style={{ overflow: 'hidden' }}>
              <div style={{ padding: '14px 16px', background: 'var(--surface)', fontWeight: 700, borderBottom: '1px solid var(--line)' }}>Specifications</div>
              <table className="spec-table">
                <tbody>{specs.map(([k, v]) => <tr key={k}><th scope="row">{k}</th><td>{v}</td></tr>)}</tbody>
              </table>
            </div>
          )}
        </div>
      </div>
      {related.length > 0 && (
        <section className="section stack">
          <h2 className="section-title">You may also like</h2>
          <ProductGrid products={related} />
        </section>
      )}
    </main>
  );
}
