import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <main className="container page stack" style={{ alignItems: 'flex-start' }}>
      <h1 className="page-title">Page not found</h1>
      <p className="muted">The page you are looking for does not exist.</p>
      <Link to="/" className="btn btn-primary">Back to home</Link>
    </main>
  );
}
