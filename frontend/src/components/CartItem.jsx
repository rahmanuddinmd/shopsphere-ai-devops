import { Link } from 'react-router-dom';
import { rupees } from '../api/format.js';
import { TrashIcon } from './Icons.jsx';

export default function CartItem({ item, onQuantity, onRemove, busy }) {
  return (
    <div className="cart-row">
      <img src={item.imageUrl} alt="" />
      <div className="info">
        <Link to={`/products/${item.productId}`} style={{ fontWeight: 700, color: 'var(--ink)', textDecoration: 'none', fontSize: 16 }}>
          {item.productName}
        </Link>
        <span className="muted small">{rupees(item.unitPrice)} each</span>
        {item.stockAvailable <= 5 && <span className="small" style={{ color: 'var(--red)', fontWeight: 600 }}>Only {item.stockAvailable} left</span>}
      </div>
      <div className="qty">
        <button type="button" aria-label={`Decrease quantity of ${item.productName}`} disabled={busy || item.quantity <= 1}
          onClick={() => onQuantity(item.id, item.quantity - 1)}>−</button>
        <span aria-live="polite">{item.quantity}</span>
        <button type="button" aria-label={`Increase quantity of ${item.productName}`}
          disabled={busy || item.quantity >= Math.min(10, item.stockAvailable)}
          onClick={() => onQuantity(item.id, item.quantity + 1)}>+</button>
      </div>
      <span className="price" style={{ fontSize: 20, minWidth: 110, textAlign: 'right' }}>{rupees(item.lineTotal)}</span>
      <button type="button" className="icon-btn" aria-label={`Remove ${item.productName}`} disabled={busy} onClick={() => onRemove(item.id)}>
        <TrashIcon />
      </button>
    </div>
  );
}
