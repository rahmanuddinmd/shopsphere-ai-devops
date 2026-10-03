import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client.js';
import { rupees } from '../api/format.js';
import { SendIcon, SparkIcon } from './Icons.jsx';

/** Chat with the ShopSphere AI Assistant (POST /api/ai/ask). Answers are grounded on live catalog data. */
export default function AIChat({ pendingQuestion, onConsumed }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const endRef = useRef(null);
  const consumed = useRef(null);

  async function send(question) {
    const q = question.trim();
    if (!q || busy) return;
    setMessages((m) => [...m, { role: 'user', text: q }]);
    setInput('');
    setBusy(true);
    try {
      const res = await api.ask(q);
      setMessages((m) => [...m, { role: 'ai', text: res.answer, products: res.products, fallback: res.fallback }]);
    } catch (e) {
      setMessages((m) => [...m, { role: 'ai', text: `Sorry, I could not answer right now. ${e.message}`, products: [] }]);
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    if (pendingQuestion && consumed.current !== pendingQuestion) {
      consumed.current = pendingQuestion; // guards against React StrictMode double effects
      send(pendingQuestion);
      onConsumed?.();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pendingQuestion]);

  useEffect(() => {
    endRef.current?.scrollIntoView?.({ behavior: 'smooth', block: 'end' });
  }, [messages, busy]);

  return (
    <section aria-label="Conversation" className="card chat">
      <div className="chat-log" aria-live="polite">
        {messages.length === 0 && (
          <p className="muted">Ask about budget, RAM, use case or compare two products. Example: “Show laptops under ₹70,000”.</p>
        )}
        {messages.map((m, i) =>
          m.role === 'user' ? (
            <div key={i} className="msg-user">{m.text}</div>
          ) : (
            <div key={i} className="msg-ai">
              <span className="who"><SparkIcon size={16} /> ShopSphere AI</span>
              <div className="text">{m.text}</div>
              {m.products?.map((p) => (
                <Link key={p.id} to={`/products/${p.id}`} className="mini-product">
                  <span className="row" style={{ gap: 12, flexWrap: 'nowrap' }}>
                    {p.imageUrl && <img src={p.imageUrl} alt="" />}
                    <span>
                      <b>{p.name}</b>
                      <br />
                      <span className="muted small">
                        {[p.processor, p.ramGb ? `${p.ramGb} GB RAM` : null].filter(Boolean).join(' · ')}
                        {!p.inStock && ' · out of stock'}
                      </span>
                    </span>
                  </span>
                  <span className="price" style={{ fontSize: 18 }}>{rupees(p.price)}</span>
                </Link>
              ))}
              {m.fallback && <span className="muted small">Answered from catalog data (AI model unavailable).</span>}
            </div>
          ),
        )}
        {busy && <div className="msg-ai"><span className="who"><SparkIcon size={16} /> Thinking…</span></div>}
        <div ref={endRef} />
      </div>
      <form className="chat-input" onSubmit={(e) => { e.preventDefault(); send(input); }}>
        <label htmlFor="ask" className="sr-only">Ask the assistant</label>
        <input id="ask" className="input" style={{ minHeight: 52, borderRadius: 14 }} value={input} maxLength={500}
          onChange={(e) => setInput(e.target.value)} placeholder="Ask about price, RAM, use case…" />
        <button type="submit" className="btn btn-primary" aria-label="Send question" disabled={busy || !input.trim()}
          style={{ width: 52, height: 52, padding: 0, borderRadius: 14 }}>
          <SendIcon />
        </button>
      </form>
      <p className="muted small" style={{ padding: '0 16px 16px' }}>AI answers can be wrong. Prices and stock are confirmed at checkout.</p>
    </section>
  );
}
