import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import AIChat from '../components/AIChat.jsx';

describe('<AIChat>', () => {
  it('sends the question to /api/ai/ask and renders the grounded answer with products', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({
        answer: 'Two in-stock laptops fit your budget.',
        provider: 'ollama',
        fallback: false,
        products: [{ id: 1, name: 'Nimbus Pro 14', price: 68990, ramGb: 16, inStock: true, imageUrl: '/images/laptop-nimbus.jpg' }],
      }), { status: 200, headers: { 'Content-Type': 'application/json' } }),
    );

    render(<MemoryRouter><AIChat /></MemoryRouter>);
    await userEvent.type(screen.getByLabelText('Ask the assistant'), 'Show laptops under ₹70,000');
    await userEvent.click(screen.getByRole('button', { name: 'Send question' }));

    expect(await screen.findByText('Two in-stock laptops fit your budget.')).toBeInTheDocument();
    expect(screen.getByText('Nimbus Pro 14')).toBeInTheDocument();
    expect(screen.getByText('₹68,990')).toBeInTheDocument();
    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/ai/ask');
    expect(JSON.parse(options.body)).toEqual({ question: 'Show laptops under ₹70,000' });
  });

  it('shows a friendly message when the API fails', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('network'));
    render(<MemoryRouter><AIChat /></MemoryRouter>);
    await userEvent.type(screen.getByLabelText('Ask the assistant'), 'hello');
    await userEvent.click(screen.getByRole('button', { name: 'Send question' }));
    expect(await screen.findByText(/could not answer right now/i)).toBeInTheDocument();
  });
});
