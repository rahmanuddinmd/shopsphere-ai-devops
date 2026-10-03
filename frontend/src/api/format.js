const inr = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 });

/** 68990 -> "₹68,990", 112460 -> "₹1,12,460" (Indian digit grouping). */
export function rupees(amount) {
  if (amount === null || amount === undefined || amount === '') return '';
  return `₹${inr.format(Number(amount))}`;
}

export function formatDate(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export function specLine(p) {
  return [p.processor, p.ramGb ? `${p.ramGb} GB RAM` : null, p.storageGb ? `${p.storageGb} GB` : null, !p.processor ? p.display : null]
    .filter(Boolean)
    .join(' · ');
}

export const ORDER_STATUSES = ['PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

export function statusPill(status) {
  return (
    {
      PLACED: 'pill-primary',
      CONFIRMED: 'pill-primary',
      SHIPPED: 'pill-saffron',
      DELIVERED: 'pill-green',
      CANCELLED: 'pill-red',
    }[status] || 'pill-gray'
  );
}
