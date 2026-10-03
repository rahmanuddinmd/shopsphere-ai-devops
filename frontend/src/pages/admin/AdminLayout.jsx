import { Link, NavLink, Outlet } from 'react-router-dom';
import { SphereIcon } from '../../components/Icons.jsx';

export default function AdminLayout() {
  const cls = ({ isActive }) => (isActive ? 'active' : undefined);
  return (
    <div className="admin-shell">
      <aside className="admin-nav" aria-label="Admin navigation">
        <Link to="/" className="brand" style={{ color: '#fff', padding: '0 8px 20px', background: 'none' }}>
          <span className="brand-mark" style={{ width: 34, height: 34 }}><SphereIcon size={20} /></span>
          <span className="brand-name" style={{ fontSize: 18 }}>ShopSphere Admin</span>
        </Link>
        <NavLink to="/admin" end className={cls}>Dashboard</NavLink>
        <NavLink to="/admin/products" className={cls}>Products</NavLink>
        <NavLink to="/admin/orders" className={cls}>Orders</NavLink>
        <NavLink to="/admin/devops-assistant" className={cls}>AI DevOps Assistant</NavLink>
        <hr style={{ border: 0, borderTop: '1px solid #2c3038', width: '100%' }} />
        <Link to="/">← Back to store</Link>
      </aside>
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}
