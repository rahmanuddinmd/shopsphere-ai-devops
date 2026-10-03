import { Link } from 'react-router-dom';

// Build metadata is injected at image build time (Dockerfile build args) -> visible proof of which release is live.
const VERSION = import.meta.env.VITE_APP_VERSION || '1.0.0';
const BUILD = import.meta.env.VITE_BUILD_SHA || 'local';

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container">
        <div className="stack-sm" style={{ maxWidth: 360 }}>
          <span className="brand-name" style={{ fontSize: 18 }}>ShopSphere <span>AI</span></span>
          <span className="muted small">A training store for developer hardware. Demo project — no real payments.</span>
        </div>
        <nav aria-label="Footer" className="row" style={{ gap: '8px 28px', fontSize: 14 }}>
          <Link to="/products">All products</Link>
          <Link to="/assistant">AI Assistant</Link>
          <Link to="/cart">Cart</Link>
          <Link to="/orders">Orders</Link>
        </nav>
        <div className="build">
          <div>v{VERSION} · build {BUILD}</div>
          <div>ShopSphere AI DevOps platform</div>
        </div>
      </div>
    </footer>
  );
}
