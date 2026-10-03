import { useEffect, useState } from 'react';
import { api } from '../../api/client.js';
import { rupees } from '../../api/format.js';
import ErrorMessage from '../../components/ErrorMessage.jsx';
import Loading from '../../components/Loading.jsx';

const EMPTY = {
  sku: '', name: '', brand: '', description: '', price: '', mrp: '', stockQuantity: 0, imageUrl: '/images/',
  ramGb: '', storageGb: '', processor: '', display: '', badge: '', categorySlug: 'laptops',
};

const toForm = (p) => ({
  ...EMPTY, ...Object.fromEntries(Object.entries(p).map(([k, v]) => [k, v ?? ''])), categorySlug: p.category.slug,
});

const toRequest = (f) => ({
  ...f,
  price: Number(f.price),
  mrp: f.mrp === '' ? null : Number(f.mrp),
  stockQuantity: Number(f.stockQuantity),
  ramGb: f.ramGb === '' ? null : Number(f.ramGb),
  storageGb: f.storageGb === '' ? null : Number(f.storageGb),
});

export default function AdminProducts() {
  const [products, setProducts] = useState(null);
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  const load = () => api.products({ size: 50, sort: 'name' }).then((r) => setProducts(r.content)).catch(setError);
  useEffect(() => {
    load();
    api.categories().then(setCategories).catch(() => {});
  }, []);

  async function save(e) {
    e.preventDefault();
    setError(null);
    setMessage(null);
    try {
      if (editingId) await api.updateProduct(editingId, toRequest(form));
      else await api.createProduct(toRequest(form));
      setMessage(editingId ? 'Product updated.' : 'Product created.');
      setForm(EMPTY);
      setEditingId(null);
      load();
    } catch (err) {
      setError(err);
    }
  }

  async function remove(p) {
    if (!window.confirm(`Remove "${p.name}" from the shop? Past orders are kept.`)) return;
    try {
      await api.deleteProduct(p.id);
      setMessage(`${p.name} removed.`);
      load();
    } catch (err) {
      setError(err);
    }
  }

  const fe = error?.fieldErrors || {};
  const input = (id, label, attrs = {}) => (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} className="input" value={form[id]} aria-invalid={!!fe[id]}
        onChange={(e) => setForm({ ...form, [id]: e.target.value })} {...attrs} />
      {fe[id] && <span className="small" style={{ color: 'var(--red)' }}>{fe[id]}</span>}
    </div>
  );

  return (
    <>
      <h1 className="page-title" style={{ fontSize: 36 }}>Products</h1>
      {message && <div className="alert alert-success" role="status">{message}</div>}
      <ErrorMessage error={Object.keys(fe).length ? 'Please fix the highlighted fields.' : error} />
      <form className="panel stack" onSubmit={save} aria-labelledby="pf-h">
        <h2 id="pf-h" style={{ fontSize: 20 }}>{editingId ? `Edit product #${editingId}` : 'Add product'}</h2>
        <div className="form-grid">
          {input('sku', 'SKU', { required: true })}
          {input('name', 'Name', { required: true })}
          {input('brand', 'Brand')}
          <div className="field">
            <label htmlFor="categorySlug">Category</label>
            <select id="categorySlug" className="select" value={form.categorySlug} onChange={(e) => setForm({ ...form, categorySlug: e.target.value })}>
              {categories.map((c) => <option key={c.slug} value={c.slug}>{c.name}</option>)}
            </select>
          </div>
          {input('price', 'Price (₹)', { type: 'number', min: 1, required: true })}
          {input('mrp', 'MRP (₹, optional)', { type: 'number', min: 1 })}
          {input('stockQuantity', 'Stock', { type: 'number', min: 0, required: true })}
          {input('ramGb', 'RAM (GB)', { type: 'number', min: 0 })}
          {input('storageGb', 'Storage (GB)', { type: 'number', min: 0 })}
          {input('processor', 'Processor')}
          {input('display', 'Display')}
          {input('badge', 'Badge')}
          {input('imageUrl', 'Image URL')}
          <div className="field span-all">
            <label htmlFor="description">Description</label>
            <textarea id="description" className="textarea" style={{ minHeight: 90, fontFamily: 'inherit', fontSize: 14 }} value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
        </div>
        <div className="row">
          <button type="submit" className="btn btn-primary">{editingId ? 'Save changes' : 'Create product'}</button>
          {editingId && <button type="button" className="btn btn-secondary" onClick={() => { setEditingId(null); setForm(EMPTY); }}>Cancel</button>}
        </div>
      </form>
      <section className="panel" aria-label="Product list">
        {!products ? <Loading /> : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th scope="col">Product</th><th scope="col">Category</th><th scope="col">Price</th><th scope="col">Stock</th><th scope="col"><span className="sr-only">Actions</span></th></tr></thead>
              <tbody>
                {products.map((p) => (
                  <tr key={p.id}>
                    <td><span className="row" style={{ gap: 10, flexWrap: 'nowrap' }}><img src={p.imageUrl} alt="" style={{ width: 56, height: 42, objectFit: 'cover', borderRadius: 8 }} /><b>{p.name}</b></span></td>
                    <td>{p.category.name}</td>
                    <td>{rupees(p.price)}</td>
                    <td><span className={`pill ${p.stockQuantity <= 5 ? 'pill-red' : 'pill-green'}`}>{p.stockQuantity}</span></td>
                    <td className="row" style={{ gap: 8, flexWrap: 'nowrap' }}>
                      <button type="button" className="btn btn-secondary btn-sm" onClick={() => { setEditingId(p.id); setForm(toForm(p)); window.scrollTo?.(0, 0); }}>Edit</button>
                      <button type="button" className="btn btn-secondary btn-sm" onClick={() => remove(p)}>Remove</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
