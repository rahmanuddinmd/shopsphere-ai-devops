import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { api } from '../api/client.js';
import OrderCard from '../components/OrderCard.jsx';
import Loading from '../components/Loading.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';

export default function Orders() {
  const location = useLocation();
  const [orders, setOrders] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.orders().then(setOrders).catch(setError);
  }, []);

  return (
    <main className="container page stack">
      <h1 className="page-title">Order history</h1>
      {location.state?.placed && (
        <div className="alert alert-success" role="status">Thank you! Order {location.state.placed} has been placed.</div>
      )}
      <ErrorMessage error={error} />
      {!orders && !error && <Loading />}
      {orders?.length === 0 && <p className="muted">You have not placed any orders yet.</p>}
      <div className="grid-auto" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))' }}>
        {orders?.map((o) => <OrderCard key={o.id} order={o} />)}
      </div>
    </main>
  );
}
