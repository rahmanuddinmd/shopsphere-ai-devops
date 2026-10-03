import { describe, expect, it } from 'vitest';
import { rupees, specLine, statusPill } from '../api/format.js';

describe('format helpers', () => {
  it('formats rupees with Indian digit grouping', () => {
    expect(rupees(68990)).toBe('₹68,990');
    expect(rupees(112460)).toBe('₹1,12,460');
    expect(rupees(null)).toBe('');
  });

  it('builds a short spec line', () => {
    expect(specLine({ processor: 'Ryzen 7', ramGb: 16, storageGb: 512 })).toBe('Ryzen 7 · 16 GB RAM · 512 GB');
    expect(specLine({ display: '27" 4K IPS' })).toBe('27" 4K IPS');
  });

  it('maps order status to a pill colour', () => {
    expect(statusPill('DELIVERED')).toBe('pill-green');
    expect(statusPill('UNKNOWN')).toBe('pill-gray');
  });
});
