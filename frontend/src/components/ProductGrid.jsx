import ProductCard from './ProductCard.jsx';

export default function ProductGrid({ products, minWidth = 250 }) {
  if (!products?.length) {
    return <p className="muted">No products match these filters.</p>;
  }
  return (
    <div className="grid-auto" style={{ gridTemplateColumns: `repeat(auto-fill, minmax(${minWidth}px, 1fr))` }}>
      {products.map((p) => <ProductCard key={p.id} product={p} />)}
    </div>
  );
}
