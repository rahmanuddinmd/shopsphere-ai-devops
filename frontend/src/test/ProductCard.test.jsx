import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import ProductCard from '../components/ProductCard.jsx';

vi.mock('../context/AuthContext.jsx', () => ({ useAuth: () => ({ user: null }) }));
vi.mock('../context/CartContext.jsx', () => ({ useCart: () => ({ add: vi.fn() }) }));

const product = {
  id: 1, name: 'Nimbus Pro 14', price: 68990, mrp: 74990, inStock: true, badge: 'Developer pick',
  processor: 'AMD Ryzen 7', ramGb: 16, storageGb: 512, imageUrl: '/images/laptop-nimbus.jpg',
  category: { name: 'Laptops', slug: 'laptops' },
};

describe('<ProductCard>', () => {
  it('shows name, price, MRP, badge and image', () => {
    render(<MemoryRouter><ProductCard product={product} /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Nimbus Pro 14' })).toBeInTheDocument();
    expect(screen.getByText('₹68,990')).toBeInTheDocument();
    expect(screen.getByText('₹74,990')).toBeInTheDocument();
    expect(screen.getByText('Developer pick')).toBeInTheDocument();
    expect(screen.getByAltText('Nimbus Pro 14')).toHaveAttribute('src', '/images/laptop-nimbus.jpg');
  });

  it('disables add-to-cart when out of stock', () => {
    render(<MemoryRouter><ProductCard product={{ ...product, inStock: false }} /></MemoryRouter>);
    expect(screen.getByRole('button', { name: /add nimbus pro 14 to cart/i })).toBeDisabled();
    expect(screen.getByText('Out of stock')).toBeInTheDocument();
  });
});
