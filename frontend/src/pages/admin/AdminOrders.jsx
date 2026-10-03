import { useEffect, useState } from 'react';
import { api } from '../../api/client.js';
import { formatDate, ORDER_STATUSES, rupees, statusPill } from '../../api/format.js';
import ErrorMessage from '../../components/ErrorMessage.jsx';
import Loading from '../../components/Loading.jsx';

export default function AdminOrders() {
  const [page, setPage] = useState(null);
  const [error, setError] = useState(null);

  const load = () => api.adminOrders(0).then(setPage).catch(setError);
  useEffect(() => { load(); }, []);

  async function changeStatus(order, status) {
    setError(null);
    try {
      await api.updateOrderStatus(order.id, status);
      load();
    } catch (err) {
      setError(err);
    }
  }

  return (
    <>
      <h1 className="page-title" style={{ fontSize: 36 }}>Orders</h1>
      <ErrorMessage error={error} />
      <section className="panel" aria-label="All orders">
        {!page ? <Loading /> : page.content.length === 0 ? <p className="muted">No orders yet.</p> : (
          <div className="table-wrap">
            <table className="table" style={{ minWidth: 760 }}>
              <thead><tr><th scope="col">Order</th><th scope="col">Customer</th><th scope="col">Items</th><th scope="col">Total</th><th scope="col">Date</th><th scope="col">Status</th><th scope="col">Update</th></tr></thead>
              <tbody>
                {page.content.map((o) => {
                  const final = o.status === 'DELIVERED' || o.status === 'CANCELLED';
                  return (
                    <tr key={o.id}>
                      <td className="mono">{o.orderNumber}</td>
                      <td>{o.customerName}<br /><span className="muted small">{o.customerEmail}</span></td>
                      <td className="muted">{o.items.map((i) => `${i.productName} ×${i.quantity}`).join(', ')}</td>
                      <td><b>{rupees(o.totalAmount)}</b></td>
                      <td>{formatDate(o.createdAt)}</td>
                      <td><span className={`pill ${statusPill(o.status)}`}>{o.status}</span></td>
                      <td>
                        <label htmlFor={`st-${o.id}`} className="sr-only">Update status of {o.orderNumber}</label>
                        <select id={`st-${o.id}`} className="select" style={{ minWidth: 140 }} value={o.status} disabled={final}
                          onChange={(e) => changeStatus(o, e.target.value)}>
                          {ORDER_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                        </select>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
