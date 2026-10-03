// Central HTTP client for the ShopSphere REST API.
// Base URL: VITE_API_URL (build-time) or the relative path /api, which the Vite dev proxy,
// Nginx (Docker Compose) or the Kubernetes Ingress routes to the Spring Boot backend.
export const API_BASE = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '');

const TOKEN_KEY = 'shopsphere.token';

// sessionStorage: the token disappears when the tab closes (smaller exposure than localStorage).
export const tokenStore = {
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (token) => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY),
};

export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

let onUnauthorized = () => {};
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

export async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = tokenStore.get();
  if (auth && token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Please check your connection and try again.');
  }

  if (response.status === 204) return null;
  const isJson = (response.headers.get('content-type') || '').includes('json');
  const data = isJson ? await response.json() : null;

  if (!response.ok) {
    if (response.status === 401 && token) onUnauthorized();
    const message = data?.detail || data?.title || `Request failed (${response.status})`;
    throw new ApiError(response.status, message, data?.errors || {});
  }
  return data;
}

function toQuery(params) {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '' && v !== false) q.set(k, v);
  });
  const s = q.toString();
  return s ? `?${s}` : '';
}

export const api = {
  // auth
  register: (body) => request('/auth/register', { method: 'POST', body, auth: false }),
  login: (body) => request('/auth/login', { method: 'POST', body, auth: false }),
  me: () => request('/auth/me'),
  updateProfile: (body) => request('/users/me', { method: 'PUT', body }),
  // catalog
  products: (params = {}) => request(`/products${toQuery(params)}`, { auth: false }),
  product: (id) => request(`/products/${id}`, { auth: false }),
  categories: () => request('/categories', { auth: false }),
  createProduct: (body) => request('/products', { method: 'POST', body }),
  updateProduct: (id, body) => request(`/products/${id}`, { method: 'PUT', body }),
  deleteProduct: (id) => request(`/products/${id}`, { method: 'DELETE' }),
  // cart
  cart: () => request('/cart'),
  addToCart: (productId, quantity = 1) => request('/cart/items', { method: 'POST', body: { productId, quantity } }),
  updateCartItem: (itemId, quantity) => request(`/cart/items/${itemId}`, { method: 'PUT', body: { quantity } }),
  removeCartItem: (itemId) => request(`/cart/items/${itemId}`, { method: 'DELETE' }),
  // orders
  placeOrder: (body) => request('/orders', { method: 'POST', body }),
  orders: () => request('/orders'),
  order: (id) => request(`/orders/${id}`),
  // admin
  dashboard: () => request('/admin/dashboard'),
  adminOrders: (page = 0) => request(`/admin/orders?page=${page}&size=20`),
  updateOrderStatus: (id, status) => request(`/admin/orders/${id}/status`, { method: 'PUT', body: { status } }),
  // AI
  ask: (question) => request('/ai/ask', { method: 'POST', body: { question }, auth: false }),
  aiStatus: () => request('/ai/status', { auth: false }),
  analyzeLog: (source, log) => request('/ai/ops/analyze', { method: 'POST', body: { source, log } }),
};
