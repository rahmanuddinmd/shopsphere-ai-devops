import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client.js';
import { formatDate, rupees, statusPill } from '../../api/format.js';
import Loading from '../../components/Loading.jsx';
import ErrorMessage from '../../components/ErrorMessage.jsx';

export default function AdminDashboard() {
  const [data, setData] = useState(null);
  const [orders, setOrders] = useState([]);
  const [health, setHealth] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.dashboard().then(setData).catch(setError);
    api.adminOrders(0).then((r) => setOrders(r.content.slice(0, 5))).catch(() => {});
    fetch('/actuator/health')
      .then((r) => r.json())
      .then(setHealth)
      .catch(() => setHealth({ status: 'UNKNOWN' }));
  }, []);

  if (error) return <ErrorMessage error={error} />;
  if (!data) return <Loading />;

  const kpis = [
    ['Active products', data.activeProducts, 'In the catalog'],
    ['Orders · last 7 days', data.ordersLast7Days, `${data.ordersAwaitingShipment} awaiting shipment`],
    ['Revenue · last 7 days', rupees(data.revenueLast7Days), 'Including GST'],
    ['Low stock', data.lowStock.length, data.lowStock[0] ? `${data.lowStock[0].name} · ${data.lowStock[0].stockQuantity} left` : 'All good'],
  ];

  return (
    <>
      <div className="row between" style={{ alignItems: 'flex-end' }}>
        <div>
          <h1 className="page-title" style={{ fontSize: 36 }}>Dashboard</h1>
          <p className="muted">Signed in as ADMIN</p>
        </div>
        <Link to="/admin/products" className="btn btn-primary">+ Add product</Link>
      </div>
      <div className="kpis">
        {kpis.map(([label, value, sub]) => (
          <div key={label} className="panel kpi">
            <span className="small muted" style={{ fontWeight: 600 }}>{label}</span>
            <span className="value">{value}</span>
            <span className="small muted">{sub}</span>
          </div>
        ))}
      </div>
      <div className="row" style={{ alignItems: 'flex-start', gap: 24 }}>
        <section className="panel" style={{ flex: '999 1 520px', minWidth: 0 }} aria-labelledby="recent-h">
          <div className="row between" style={{ marginBottom: 8 }}>
            <h2 id="recent-h" style={{ fontSize: 20 }}>Recent orders</h2>
            <Link to="/admin/orders" className="small" style={{ fontWeight: 600 }}>All orders →</Link>
          </div>
          {orders.length === 0 ? <p className="muted">No orders yet.</p> : (
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th scope="col">Order</th><th scope="col">Customer</th><th scope="col">Date</th><th scope="col">Total</th><th scope="col">Status</th></tr></thead>
                <tbody>
                  {orders.map((o) => (
                    <tr key={o.id}>
                      <td className="mono">{o.orderNumber}</td>
                      <td>{o.customerName}</td>
                      <td>{formatDate(o.createdAt)}</td>
                      <td><b>{rupees(o.totalAmount)}</b></td>
                      <td><span className={`pill ${statusPill(o.status)}`}>{o.status}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
        <section className="panel stack" style={{ flex: '1 1 280px' }} aria-labelledby="sys-h">
          <h2 id="sys-h" style={{ fontSize: 20 }}>System health</h2>
          <div className="row between"><span><b>Backend API</b><br /><span className="mono muted">/actuator/health</span></span>
            <span className={`pill ${health?.status === 'UP' ? 'pill-green' : 'pill-red'}`}>{health?.status || '…'}</span></div>
          <div className="row between"><span><b>AI provider</b><br /><span className="mono muted">AI_PROVIDER={data.aiProvider}</span></span>
            <span className={`pill ${data.aiProvider === 'disabled' ? 'pill-gray' : 'pill-green'}`}>{data.aiProvider === 'disabled' ? 'OFF' : 'ON'}</span></div>
          <div className="mono muted small">backend version {data.version}</div>
          {data.lowStock.length > 0 && (
            <div className="stack-sm">
              <b>Low stock</b>
              {data.lowStock.map((p) => (
                <div key={p.id} className="row" style={{ gap: 10 }}>
                  <img src={p.imageUrl} alt="" style={{ width: 56, height: 42, objectFit: 'cover', borderRadius: 8 }} />
                  <span style={{ flex: 1 }}>{p.name}</span>
                  <span className="pill pill-red">{p.stockQuantity} left</span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </>
  );
}
