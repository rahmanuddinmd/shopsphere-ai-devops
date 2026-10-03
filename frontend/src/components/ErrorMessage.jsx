export default function ErrorMessage({ error, onRetry }) {
  if (!error) return null;
  const message = typeof error === 'string' ? error : error.message;
  return (
    <div className="alert alert-error row between" role="alert">
      <span>{message}</span>
      {onRetry && <button type="button" className="link-btn" onClick={onRetry}>Try again</button>}
    </div>
  );
}
