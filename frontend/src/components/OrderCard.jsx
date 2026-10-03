import { formatDate, rupees, statusPill } from '../api/format.js';

export default function OrderCard({ order }) {
  return (
    <article className="panel stack">
      <div className="row between">
        <div className="stack-sm" style={{ gap: 2 }}>
          <span className="mono">{order.orderNumber}</span>
          <span className="muted small">Placed {formatDate(order.createdAt)} · {order.paymentMethod === 'CASH_ON_DELIVERY' ? 'Cash on delivery' : 'Demo card'}</span>
        </div>
        <span className={`pill ${statusPill(order.status)}`}>{order.status}</span>
      </div>
      <div className="stack-sm">
        {order.items.map((i) => (
          <div key={`${order.id}-${i.productName}`} className="row" style={{ gap: 12 }}>
            {i.imageUrl && <img src={i.imageUrl} alt="" style={{ width: 64, height: 48, objectFit: 'cover', borderRadius: 8 }} />}
            <span style={{ flex: 1 }}>{i.productName} × {i.quantity}</span>
            <span className="price">{rupees(i.lineTotal)}</span>
          </div>
        ))}
      </div>
      <div className="summary-row summary-total">
        <span>Total</span>
        <span className="price">{rupees(order.totalAmount)}</span>
      </div>
      <span className="muted small">Ship to {order.shippingName}, {order.shippingCity} {order.shippingPin}</span>
    </article>
  );
}
