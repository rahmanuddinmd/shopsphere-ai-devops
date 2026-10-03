import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/client.js';
import AIChat from '../components/AIChat.jsx';

const PROMPTS = [
  'Show laptops under ₹70,000.',
  'Which laptop is suitable for programming?',
  'Show products with 16 GB RAM.',
  'Compare Nimbus Pro 14 and Vertex Book 15.',
  'Recommend a monitor for software development.',
  'Which products are currently available?',
  'Recommend a phone under ₹25,000.',
  'Find products below ₹50,000.',
];

export default function AIAssistant() {
  const [params, setParams] = useSearchParams();
  const [pending, setPending] = useState(params.get('q'));
  const [status, setStatus] = useState(null);

  useEffect(() => {
    api.aiStatus().then(setStatus).catch(() => setStatus(null));
  }, []);

  return (
    <main className="container page">
      <div className="row between" style={{ alignItems: 'flex-end', marginBottom: 24 }}>
        <div>
          <h1 className="page-title">AI Assistant</h1>
          <p className="muted">Ask about products in plain words. Answers come from ShopSphere’s live catalog.</p>
        </div>
        {status && (
          <span className={`pill ${status.enabled ? 'pill-green' : 'pill-gray'}`} style={{ fontSize: 13, padding: '6px 12px' }}>
            Provider: {status.provider}{status.enabled ? ` · ${status.model}` : ' (catalog answers only)'}
          </span>
        )}
      </div>
      <div className="with-sidebar">
        <aside aria-labelledby="try-h" className="sidebar stack" style={{ gap: 10 }}>
          <h2 id="try-h" style={{ fontSize: 15, fontFamily: 'var(--font-body)' }}>Try asking</h2>
          {PROMPTS.map((p) => (
            <button key={p} type="button" className="suggestion" onClick={() => setPending(p)}>{p}</button>
          ))}
        </aside>
        <div className="main-col">
          <AIChat pendingQuestion={pending} onConsumed={() => { setPending(null); if (params.get('q')) setParams({}); }} />
        </div>
      </div>
    </main>
  );
}
