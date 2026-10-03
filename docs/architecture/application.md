# How the ShopSphere application works

## 1. Request flow (one product search)

1. The browser loads the React app from Nginx (`frontend` container/pod).
2. React calls `GET /api/products?category=laptops&maxPrice=70000`.
3. Locally the Vite dev server proxies `/api` to `localhost:8080`; in Docker Compose Nginx proxies it
   to `backend:8080`; on EKS the ALB Ingress sends `/api` straight to the `backend` Service.
4. Spring Security checks the route (catalog reads are public) → `ProductController` →
   `ProductService` builds JPA Specifications → PostgreSQL query using indexes → DTOs → JSON.
5. Micrometer records the request (`http_server_requests_seconds`) for Prometheus.

## 2. Backend modules (package `com.shopsphere`)

| Package | Responsibility |
|---|---|
| `controller` | REST endpoints, request validation (`@Valid`) |
| `dto` | Request/response records - entities never leave the service layer |
| `service` | Business rules: cart limits, stock checks, checkout transaction, admin stats |
| `repository` | Spring Data JPA repositories + `ProductSpecifications` (dynamic filters) |
| `entity` | JPA entities mapped to the Flyway schema (`ddl-auto: validate`) |
| `security` | JWT issuing (`JwtTokenService`), resource-server config, RBAC rules |
| `ai` | Provider-neutral `AiClient` (Ollama/Bedrock/disabled), Product Assistant, DevOps Assistant |
| `exception` | Global handler → RFC 9457 problem+json, no stack traces to clients |
| `config` | Typed settings (`AppProperties`), bootstrap admin user |

## 3. Key business flows

**Register / login** - `POST /api/auth/register|login` → BCrypt (cost 12) → JWT (HS256, 60 min) with
claims `sub` (email), `uid` (user id), `roles` ([ADMIN|CUSTOMER]). The React app stores the token in
`sessionStorage` and sends `Authorization: Bearer <token>`.

**Cart** - one cart per user, max 10 per line, can never exceed stock.

**Checkout** (`POST /api/orders`) - single database transaction: validate every line → decrement stock →
create order + items (name and price are *snapshotted*) → empty the cart → increment metric
`shopsphere_orders_placed_total`. Product rows carry `@Version`, so two buyers cannot both take
the last unit (one gets HTTP 409).

**Admin** - product CRUD (delete is a soft delete), all orders, status changes
(`PLACED → CONFIRMED → SHIPPED → DELIVERED`, or `CANCELLED` which returns stock).

## 4. REST API

| Method & path | Access | Purpose |
|---|---|---|
| `POST /api/auth/register` | public | Create customer account, returns JWT |
| `POST /api/auth/login` | public | Returns JWT |
| `GET /api/auth/me` | user | Current profile |
| `PUT /api/users/me` | user | Update name/phone |
| `GET /api/products` | public | Search: `q, category, minPrice, maxPrice, minRam, inStock, sort, page, size` |
| `GET /api/products/{id}` | public | Product details |
| `POST/PUT/DELETE /api/products[/{id}]` | ADMIN | Manage catalog |
| `GET /api/categories` | public | Categories |
| `GET /api/cart` · `POST /api/cart/items` · `PUT/DELETE /api/cart/items/{id}` | user | Cart |
| `POST /api/orders` · `GET /api/orders` · `GET /api/orders/{id}` | user | Checkout & history |
| `GET /api/admin/dashboard` · `GET /api/admin/orders` · `PUT /api/admin/orders/{id}/status` | ADMIN | Admin |
| `POST /api/ai/ask` | public | ShopSphere AI Assistant |
| `GET /api/ai/status` | public | Active AI provider |
| `POST /api/ai/ops/analyze` | ADMIN | AI DevOps Assistant |
| `GET /actuator/health[/liveness|/readiness]` | public | Probes |
| `GET /actuator/prometheus` | internal | Metrics scrape |
| `GET /actuator/info`, `/actuator/metrics` | ADMIN | Build info, metrics |

Errors use `application/problem+json`, e.g. `{"title":"Validation failed","status":400,"errors":{"pin":"must be a 6-digit PIN code"}}`.
A ready-made Postman collection is in `postman/shopsphere-api.json`.

## 5. Database (Flyway)

`V1__create_schema.sql` creates `users, roles, user_roles, categories, products, cart, cart_items,
orders, order_items` with primary/foreign keys, CHECK constraints and indexes (price, category,
order date, status). `V2__seed_reference_data.sql` inserts roles, 4 categories and 8 products.
Never edit an applied migration - add `V3__...sql`. The admin user is created from
`ADMIN_EMAIL`/`ADMIN_PASSWORD` at startup, so no password exists in SQL.

## 6. Frontend

React 19 + React Router 7 + Vite. `src/api/client.js` is the only place that talks HTTP.
`AuthContext` (token + user) and `CartContext` (cart badge) wrap the app. Admin routes are guarded
in the UI *and* enforced by the backend. Product photos live in `frontend/public/images` and are
referenced by `products.image_url`.
