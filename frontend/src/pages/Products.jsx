import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { rupees } from '../api/format.js';
import ProductGrid from '../components/ProductGrid.jsx';
import Loading from '../components/Loading.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';
import { SparkIcon } from '../components/Icons.jsx';

const RAM_OPTIONS = [8, 16, 32];

export default function Products() {
  const [params, setParams] = useSearchParams();
  const [categories, setCategories] = useState([]);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const filters = {
    q: params.get('q') || '',
    category: params.get('category') || '',
    maxPrice: params.get('maxPrice') || '',
    minRam: params.get('minRam') || '',
    inStock: params.get('inStock') === 'true',
    sort: params.get('sort') || '',
    page: Number(params.get('page') || 0),
  };

  function setFilter(key, value) {
    const next = new URLSearchParams(params);
    if (value === '' || value === false || value === null) next.delete(key);
    else next.set(key, value);
    if (key !== 'page') next.delete('page');
    setParams(next);
  }

  useEffect(() => {
    api.categories().then(setCategories).catch(() => setCategories([]));
  }, []);

  const query = params.toString();
  useEffect(() => {
    setResult(null);
    setError(null);
    api.products({ ...filters, size: 12 }).then(setResult).catch(setError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  return (
    <main className="container page">
      <nav aria-label="Breadcrumb" className="small muted row" style={{ gap: 8 }}>
        <Link to="/">Home</Link><span aria-hidden="true">/</span><span>Products</span>
      </nav>
      <div className="row between" style={{ margin: '12px 0 24px', alignItems: 'flex-end' }}>
        <div>
          <h1 className="page-title">{filters.q ? `Results for “${filters.q}”` : 'All products'}</h1>
          <p className="muted">{result ? `${result.totalElements} products` : ' '} · prices include GST</p>
        </div>
        <div className="field" style={{ minWidth: 220 }}>
          <label htmlFor="sort" className="small muted">Sort by</label>
          <select id="sort" className="select" value={filters.sort} onChange={(e) => setFilter('sort', e.target.value)}>
            <option value="">Recommended</option>
            <option value="price_asc">Price: low to high</option>
            <option value="price_desc">Price: high to low</option>
            <option value="newest">Newest</option>
          </select>
        </div>
      </div>

      <div className="with-sidebar">
        <aside aria-label="Filters" className="panel sidebar stack" style={{ gap: 24 }}>
          <div className="row between">
            <span className="price" style={{ fontSize: 18 }}>Filters</span>
            <button type="button" className="link-btn" onClick={() => setParams({})}>Clear all</button>
          </div>
          <fieldset>
            <legend>Category</legend>
            <label className="check"><input type="radio" name="cat" checked={!filters.category} onChange={() => setFilter('category', '')} /> All</label>
            {categories.map((c) => (
              <label key={c.slug} className="check">
                <input type="radio" name="cat" checked={filters.category === c.slug} onChange={() => setFilter('category', c.slug)} /> {c.name}
              </label>
            ))}
          </fieldset>
          <fieldset>
            <legend>Max price</legend>
            <label htmlFor="maxPrice" className="sr-only">Maximum price in rupees</label>
            <input id="maxPrice" type="number" min="0" step="1000" className="input" placeholder="e.g. 70000"
              defaultValue={filters.maxPrice} onBlur={(e) => setFilter('maxPrice', e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') setFilter('maxPrice', e.currentTarget.value); }} />
            {filters.maxPrice && <span className="muted small">Up to {rupees(filters.maxPrice)}</span>}
          </fieldset>
          <fieldset>
            <legend>RAM (at least)</legend>
            <div className="row" style={{ gap: 8 }}>
              {RAM_OPTIONS.map((r) => (
                <button key={r} type="button" className="chip" aria-pressed={filters.minRam === String(r)}
                  onClick={() => setFilter('minRam', filters.minRam === String(r) ? '' : String(r))}>{r} GB</button>
              ))}
            </div>
          </fieldset>
          <label className="check">
            <input type="checkbox" checked={filters.inStock} onChange={(e) => setFilter('inStock', e.target.checked)} /> In stock only
          </label>
          <Link to="/assistant" className="panel row" style={{ background: 'var(--primary-soft)', border: 0, padding: 14, gap: 10, textDecoration: 'none', color: 'var(--ink)', flexWrap: 'nowrap', alignItems: 'flex-start' }}>
            <span style={{ color: 'var(--primary)' }}><SparkIcon size={20} /></span>
            <span className="small"><b>Not sure which to pick?</b><br />Ask the AI assistant to compare for you.</span>
          </Link>
        </aside>

        <section aria-label="Results" className="main-col stack" style={{ gap: 20 }}>
          <ErrorMessage error={error} />
          {!result && !error ? <Loading label="Loading products…" /> : result && <ProductGrid products={result.content} minWidth={240} />}
          {result && result.totalPages > 1 && (
            <nav aria-label="Pagination" className="row" style={{ justifyContent: 'center', gap: 8 }}>
              {Array.from({ length: result.totalPages }, (_, i) => (
                <button key={i} type="button" className={`btn btn-sm ${i === result.page ? 'btn-dark' : 'btn-secondary'}`}
                  aria-current={i === result.page ? 'page' : undefined} onClick={() => setFilter('page', String(i))}>{i + 1}</button>
              ))}
            </nav>
          )}
        </section>
      </div>
    </main>
  );
}
