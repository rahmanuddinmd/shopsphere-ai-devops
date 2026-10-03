import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import SearchBar from './SearchBar.jsx';
import { CartIcon, SphereIcon } from './Icons.jsx';

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const { cart } = useCart();
  const navigate = useNavigate();
  const navClass = ({ isActive }) => `nav-link${isActive ? ' active' : ''}`;

  return (
    <>
      <div className="announce">
        <div className="container">
          <span>Free delivery on every order</span>
          <span aria-hidden="true">·</span>
          <span>Prices include GST</span>
          <span aria-hidden="true">·</span>
          <span>7-day easy returns</span>
        </div>
      </div>
      <header className="navbar">
        <div className="container">
          <Link to="/" className="brand" aria-label="ShopSphere AI home">
            <span className="brand-mark"><SphereIcon /></span>
            <span className="brand-name">ShopSphere <span>AI</span></span>
          </Link>
          <nav aria-label="Main" className="nav-links">
            <NavLink to="/" end className={navClass}>Home</NavLink>
            <NavLink to="/products" className={navClass}>Products</NavLink>
            <NavLink to="/assistant" className={navClass}>AI Assistant</NavLink>
            {user && <NavLink to="/orders" className={navClass}>Orders</NavLink>}
            {isAdmin && <NavLink to="/admin" className={navClass}>Admin</NavLink>}
          </nav>
          <SearchBar onSearch={(q) => navigate(`/products?q=${encodeURIComponent(q)}`)} />
          <Link to="/cart" className="icon-btn" aria-label={`Cart, ${cart.itemCount} items`}>
            <CartIcon />
            {cart.itemCount > 0 && <span className="cart-count">{cart.itemCount}</span>}
          </Link>
          {user ? (
            <div className="row" style={{ gap: 8 }}>
              <Link to="/profile" className="btn btn-secondary btn-sm">{user.fullName.split(' ')[0]}</Link>
              <button type="button" className="btn btn-dark btn-sm" onClick={() => { logout(); navigate('/'); }}>
                Sign out
              </button>
            </div>
          ) : (
            <Link to="/login" className="btn btn-dark btn-sm">Sign in</Link>
          )}
        </div>
      </header>
    </>
  );
}
