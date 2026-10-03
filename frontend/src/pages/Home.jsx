import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client.js';
import ProductGrid from '../components/ProductGrid.jsx';
import Loading from '../components/Loading.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';
import { CheckIcon, SparkIcon } from '../components/Icons.jsx';

const CATEGORIES = [
  { name: 'Laptops', slug: 'laptops', img: '/images/laptop-nimbus.jpg', note: 'From ₹45,990' },
  { name: 'Smartphones', slug: 'smartphones', img: '/images/phone-aurora.jpg', note: 'From ₹21,499' },
  { name: 'Monitors', slug: 'monitors', img: '/images/monitor-orbit.jpg', note: '4K and QHD' },
  { name: 'Accessories', slug: 'accessories', img: '/images/keyboard-pulse.jpg', note: 'Keyboards & audio' },
];

export default function Home() {
  const [products, setProducts] = useState(null);
  const [error, setError] = useState(null);

  const load = () => {
    setError(null);
    api.products({ size: 8 }).then((r) => setProducts(r.content)).catch(setError);
  };
  useEffect(load, []);

  return (
    <main>
      <section className="hero">
        <div className="container">
          <div className="hero-copy">
            <span className="pill pill-primary" style={{ alignSelf: 'flex-start', fontSize: 13, padding: '6px 12px' }}>
              <SparkIcon size={16} /> New: shop with the AI assistant
            </span>
            <h1>The gear developers actually use.</h1>
            <p className="hero-lead">
              Laptops, smartphones, monitors and accessories — tested for coding, priced honestly, and explained in plain words by our AI assistant.
            </p>
            <div className="row" style={{ gap: 12 }}>
              <Link to="/products" className="btn btn-primary">Shop all products</Link>
              <Link to="/assistant" className="btn btn-secondary"><SparkIcon /> Ask the AI assistant</Link>
            </div>
            <div className="checks">
              <span><CheckIcon /> Free delivery</span>
              <span><CheckIcon /> GST invoice</span>
              <span><CheckIcon /> 7-day returns</span>
            </div>
          </div>
          <div className="hero-media">
            <img src="/images/hero-desk.jpg" alt="A desk with a monitor, laptop and smartphone" />
            <Link to="/products?category=laptops" className="hero-float">
              <img src="/images/laptop-nimbus.jpg" alt="" />
              <span className="stack-sm" style={{ gap: 0, lineHeight: 1.25 }}>
                <b style={{ fontSize: 14 }}>Nimbus Pro 14</b>
                <span className="price">₹68,990</span>
              </span>
            </Link>
          </div>
        </div>
      </section>

      <section className="container section stack">
        <h2 className="section-title">Shop by category</h2>
        <div className="grid-auto" style={{ gap: 16 }}>
          {CATEGORIES.map((c) => (
            <Link key={c.slug} to={`/products?category=${c.slug}`} className="card category-card">
              <img src={c.img} alt="" />
              <span className="row">
                <b>{c.name}</b>
                <span className="muted small">{c.note}</span>
              </span>
            </Link>
          ))}
        </div>
      </section>

      <section className="container section stack">
        <div className="row between">
          <h2 className="section-title">Trending this week</h2>
          <Link to="/products" style={{ fontWeight: 600 }}>View all products →</Link>
        </div>
        <ErrorMessage error={error} onRetry={load} />
        {!products && !error ? <Loading /> : products && <ProductGrid products={products} />}
      </section>

      <section className="container section">
        <div className="promo">
          <div className="copy">
            <span className="small" style={{ fontWeight: 700, letterSpacing: '.08em', textTransform: 'uppercase', color: '#c9d0f5' }}>Smartphone week</span>
            <h2 style={{ fontSize: 38, lineHeight: 1.1 }}>Nova 12 at ₹21,499</h2>
            <p style={{ color: '#dce1fa', maxWidth: 420 }}>OLED display, 8 GB RAM and all-day battery. Save ₹3,500 this week.</p>
            <Link to="/products?category=smartphones" className="btn">Shop smartphones</Link>
          </div>
          <img src="/images/phone-nova.jpg" alt="Nova 12 smartphone, front and back" />
        </div>
      </section>

      <section className="container section">
        <div className="card row" style={{ padding: 32, gap: 32, alignItems: 'center' }}>
          <div className="stack" style={{ flex: '1 1 360px' }}>
            <span className="pill pill-primary" style={{ alignSelf: 'flex-start' }}><SparkIcon size={16} /> ShopSphere AI Assistant</span>
            <h2 className="section-title" style={{ fontSize: 34 }}>Not sure what to buy? Just ask.</h2>
            <p className="muted" style={{ fontSize: 16 }}>
              “Show laptops under ₹70,000” or “Which phone has the best battery?” — answers come straight from our live catalog, with real prices and stock.
            </p>
            <Link to="/assistant" className="btn btn-primary" style={{ alignSelf: 'flex-start' }}>Start a conversation</Link>
          </div>
          <div className="panel panel-surface stack" style={{ flex: '1 1 380px' }}>
            <div className="msg-user">Show laptops under ₹70,000 for programming.</div>
            <div className="msg-ai" style={{ background: '#fff', border: '1px solid var(--line)' }}>
              <span className="small">Two in-stock laptops fit, both with 16 GB RAM:</span>
              <span className="row" style={{ gap: 12 }}><img src="/images/laptop-nimbus.jpg" alt="" style={{ width: 64, height: 48, objectFit: 'cover', borderRadius: 8 }} /><b style={{ flex: 1 }}>Nimbus Pro 14</b><span className="price">₹68,990</span></span>
              <span className="row" style={{ gap: 12 }}><img src="/images/laptop-vertex.jpg" alt="" style={{ width: 64, height: 48, objectFit: 'cover', borderRadius: 8 }} /><b style={{ flex: 1 }}>Vertex Book 15</b><span className="price">₹62,490</span></span>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
