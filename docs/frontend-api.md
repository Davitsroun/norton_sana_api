# Customer API & auth inventory

**Base URL:** `http://localhost:8082` (from frontend `constant/baseurl.js`)  
**Auth token:** NextAuth session → Keycloak access token → `Authorization: Bearer <token>` (`getKeycloakToken()` in `constant/token.ts`).

Paths below are relative to the base host. This doc reflects how **norton_skincare_ui** actually calls the Spring API, plus backend auth reality.

---

## Response shapes

### Standard envelope (`ApiResponse<T>`)

```json
{
  "success": true,
  "message": "…",
  "status": "OK",
  "payload": { },
  "timestamps": "2026-05-18T11:04:33.275561Z"
}
```

### Paginated list (`ApiResponseWithPagination<T>`)

```json
{
  "items": [ ],
  "paginationResponse": {
    "totalElements": 100,
    "currentPage": 0,
    "pageSize": 10,
    "totalPages": 10
  }
}
```

**Tip:** Active/history user orders use `payload` (array). Catalog/admin lists use `items`.

---

## Guest session cart (backend)

Typical ecommerce identity before login: a **guest session cookie**, not a user id.

| Piece | Detail |
|-------|--------|
| Cookie | `GUEST_SESSION_ID` (HttpOnly, `Path=/`, `SameSite=Lax`, ~30 days) |
| Table | `guest_sessions` (`id`, `createdAt`, `expiresAt`, `lastSeenAt`) |
| Cart | Pending `orders` row with `userId = null`, `sessionId = <cookie>` + `order_item` lines |
| Checkout | `guestEmail` on the order; `userId` stays null until claim/login |

### Flow

1. `GET /api/v1/guest/session` (or any cart call) → sets cookie, returns `{ sessionId }`
2. Browse public `GET /products` (no login)
3. `POST /order-items` with cookie → cart under session
4. `GET /orders` with cookie → active guest cart
5. Optional: `POST /orders/guest-checkout` with email + fulfillment → sets `guestEmail`, status `processing`
6. After login: `POST /api/v1/cart/merge` (Bearer) → merges guest lines into user pending cart, clears cookie

### Frontend wiring (required later)

- Send cookies: `credentials: 'include'` on fetch/axios to `localhost:8082`
- CORS already uses `allowCredentials: true` + explicit `CORS_ALLOWED_ORIGINS` (e.g. `http://localhost:3000`)
- Open `proxy.ts` public routes for `/shop`, `/cart`, etc. (still login-walled in the UI today)

### Guest / cart endpoints

| Method | Path | Auth |
|--------|------|------|
| `GET` | `/api/v1/guest/session` | Public (sets cookie) |
| `GET` | `/api/v1/orders` | Public **or** JWT (cookie or Bearer) |
| `GET` | `/api/v1/orders/{id}` | Public **or** JWT (owner check) |
| `POST` | `/api/v1/orders/guest-checkout` | Public + guest cookie |
| `POST/PUT/DELETE` | `/api/v1/order-items…` | Public **or** JWT |
| `POST` | `/api/v1/cart/merge` | **JWT required** |
| `GET` | `/api/v1/orders/history` | JWT required |
| `POST` | `/api/v1/orders` | JWT required |

#### Guest checkout body

```json
{
  "guestEmail": "alice@email.com",
  "customerName": "Alice",
  "contactNumber": "+855…",
  "paymentMethod": "BAKONG",
  "fulfillmentMethod": "pickup",
  "deliveryAddress": null
}
```

---

## 1. Customer endpoints

| Method | Path | Used from (UI / action) | Auth (backend) | Notes |
|--------|------|-------------------------|----------------|-------|
| `GET` | `/api/v1/products?page=&size=&minPrice=&maxPrice=&name=&categoryId=` | Shop `/shop`, Home `/home` via `getProductAction` | **Public** (Bearer optional) | `name` filter supported |
| `GET` | `/api/v1/products/{id}` | Product detail `/shop/[productId]` | **Public** | Includes related + reviews in payload |
| `GET` | `/api/v1/guest/session` | Call on first visit / before cart | **Public** | Sets `GUEST_SESSION_ID` |
| `POST` | `/api/v1/orders` | `createOrderAction` only — **not called by any UI** | **Required** | Atomic checkout body exists; UI unused |
| `GET` | `/api/v1/orders` | Cart / Nav badge | **Public or JWT** | Active pending/processing for user **or** guest session |
| `POST` | `/api/v1/orders/guest-checkout` | Guest checkout (wire in UI) | **Public + cookie** | Sets `guestEmail`; empty cart rejected |
| `POST` | `/api/v1/cart/merge` | After login | **JWT** | Merges guest cookie cart into user cart |
| `GET` | `/api/v1/orders/history` | History `/history` | **Required** | Paid/completed; no pagination |
| `GET` | `/api/v1/orders/{orderId}` | Order detail | **Public or JWT** | Owner = userId or sessionId |
| `POST` | `/api/v1/order-items` | Shop + product detail “Add to cart” | **Public or JWT** | Creates/attaches to open cart order |
| `GET` | `/api/v1/order-items?page=&size=` | `listOrderItemsAction` — no customer UI call | **Public or JWT** | |
| `GET` | `/api/v1/order-items/{id}` | Unused in UI | **Public or JWT** | Owner-scoped |
| `PUT` | `/api/v1/order-items/{id}` | Cart quantity sync | **Public or JWT** | |
| `DELETE` | `/api/v1/order-items/{id}` | Cart line remove | **Public or JWT** | |
| `GET` | `/api/v1/brands?page=&size=` | Admin brand picker — not customer pages | **Required** | |
| `GET` | `/api/v1/brands/{brandId}` | Admin / service only | **Required** | |
| `GET` | `/api/v1/categories` | Comment says Public; admin-wired in app | **Public** | Full list, no pagination |
| `GET` | `/api/v1/favorite-brands?page=&size=` | Favorites `/favorites`, Nav, product detail | **Required** | Brand favorites (not product IDs) |
| `POST` | `/api/v1/favorite-brands` body `{ brandId }` | Product detail heart toggle | **Required** | |
| `DELETE` | `/api/v1/favorite-brands/{favoriteBrandId}` | Favorites + product detail | **Required** | |
| `POST` | `/api/v1/payment-profiles` | Cart checkout (details step) | **Required** | |
| `PUT` | `/api/v1/payment-profiles/{id}` | Cart checkout (update stored profile) | **Required** | |
| `DELETE` | `/api/v1/payment-profiles/{id}` | Action exists — no cart UI delete | **Required** | |
| `POST` | `/api/v1/payments` | Cart “I Have Paid” (pickup/Bakong) | **Required** | |
| `PUT` | `/api/v1/payments/{id}` | `updatePaymentAction` — no customer UI | **Required** | |
| `POST` | `/api/v1/bakong/generate-qr` | Cart KHQR via `useBakongKhqr` | **Public** | |
| `POST` | `/api/v1/bakong/get-qr-image` | Same | **Public** | |
| `POST` | `/api/v1/bakong/check-transaction` | Action/service only — UI does not poll | **Public** | |
| `POST` | `/api/v1/reviews` | Product detail create review | **Required** | |
| `PUT` | `/api/v1/reviews/{id}` | Product detail edit review | **Required** | |
| `DELETE` | `/api/v1/reviews/{id}` | Product detail delete review | **Required** | |
| `GET` | `/api/v1/user-notifications` | `NotificationContext` (header) | **Required** | Backend returns full list (no page/size) |
| `PUT` | `/api/v1/user-notifications/{id}` body `{ read? }` | Mark read | **Required** | |
| `DELETE` | `/api/v1/user-notifications/{id}` | Dismiss | **Required** | |
| `POST` | `/api/v1/user-notifications` | Usually server-created on payment — no customer UI create | **Required** | |
| `POST` | `/api/v1/files/upload-file` | Profile image upload | **Public** | Frontend may still require NextAuth session before calling |

### Auth / Keycloak (not Spring `/api/v1`)

| Flow | Path / mechanism | Used from | Auth? |
|------|------------------|-----------|-------|
| Login / session | NextAuth `app/api/auth/[...nextauth]` + Keycloak | `/login` | Public entry |
| Register | Keycloak Admin API via `registerAction` | `/register` | Public |
| Password reset OTP | `KEYCLOAK_PASSWORD_RESET_OTP_URL` `/send`, `/verify`, `/confirm` | `/forgot-password` | Public |
| Profile CRUD | Keycloak Admin via `profile-actions` | `/profile` | Yes (session) |
| Realm sessions revoke | Keycloak Admin via `session-actions` | Admin settings only | Yes (admin) |

`route/authRoute.ts` lists app paths (`/login`, `/register`, …), not backend URLs.

---

## 2. Pages that force login (frontend)

**Edge gate** — `proxy.ts` (Next 16; replaces middleware):

- Public only: `/login`, `/register`, `/forgot-password`
- Everything else (`/shop`, `/cart`, `/favorites`, `/profile`, `/home`, …) → `/login` if no JWT
- `/` → `/login` or `/home` (or `/admin` if admin)
- Admins on customer routes → `/admin`; non-admins on `/admin/*` → `/home`

**Client `ProtectedRoute`:** used on `/home`, `/about`, `/history`.  
Not wrapped (still blocked by proxy): `/shop`, `/shop/[productId]`, `/cart`, `/favorites`, `/profile`.

**Action-level:** almost all customer Spring calls throw “Sign in required” without token. Products alone attach Bearer optionally.

---

## 3. Cart / checkout flow (actual UI)

```
Browse products (GET /products)
        │
        ▼
Add to cart (authenticated):
  POST /order-items { productId, quantity }
  + localStorage "cart" via CartProvider (also written)
        │
        ▼
Cart UI (authenticated):
  GET /orders → latest pending/processing = "basket"
  qty: PUT /order-items/{id}
  remove: DELETE /order-items/{id}
  Nav badge = API order qty only (not localStorage)
        │
        ▼
Checkout details:
  POST or PUT /payment-profiles
  { deliveryOption: PICKUP|DELIVERY, fullName, contactNumber, deliveryAddress }
  paymentProfileId cached in sessionStorage `norton:paymentProfileId:{userId}`
        │
   ┌────┴────┐
   │         │
Delivery   Pickup
   │         │
   │    Bakong: POST /bakong/generate-qr + get-qr-image
   │    Confirm: POST /payments
   │      { orderId, paymentMethod: "BAKONG", paymentStatus: "PAID",
   │        transactionId: "bakong-md5:…" | "bakong:…", paidAt }
   │
   ▼
placeOrder() — local only:
  snapshot → localStorage `customer_order_history`
  clear local cart if not API basket
  navigate /history
```

**Important:**

- `POST /api/v1/orders` is **never** invoked from UI. Open cart is created implicitly via `order-items`.
- History page loads `GET /orders/history` only (ignores localStorage merge).
- Delivery path does not call `createPayment` (COD assumed after payment-profile save).
- UI does not poll `check-transaction`.

### Alternate backend path (defined, unused by UI)

`POST /api/v1/orders`:

```json
{
  "items": [{ "productId": "uuid", "quantity": 2 }],
  "customerName": "Jane Doe",
  "contactNumber": "+855…",
  "fulfillmentMethod": "delivery",
  "deliveryAddress": "123 Street",
  "paymentMethod": "khqr"
}
```

Also accepts `fulfillment` instead of `fulfillmentMethod`.

### Payloads the UI actually sends

| Step | Body |
|------|------|
| Order line | `{ "productId", "quantity" }` → `POST /order-items` |
| Payment profile | `{ "deliveryOption", "fullName", "contactNumber", "deliveryAddress" }` |
| Payment | `{ "orderId", "paymentMethod", "paymentStatus", "transactionId", "paidAt" }` |
| Bakong generate | `{ "currency", "amount", "merchantName" }` |

**Success payment statuses (backend):** `PAID`, `SUCCESS`, `COMPLETED`, `SUCCEEDED` (any case).  
**Side effects on success:** order → `paid`, notification created, other pending carts cleared.

---

## 4. Cart storage model (frontend)

| Store | Key / resource | Role |
|-------|----------------|------|
| localStorage | `cart` | Client basket — always mirrored on add |
| API | `GET /orders` + order-items CRUD | Source of truth for signed-in cart UI + nav badge |
| localStorage | `customer_order_history` | Written on `placeOrder`; history page no longer reads it |
| localStorage | `favorites` | Product-id hearts on home/shop — **not** brand favorites API |
| sessionStorage | `norton:paymentProfileId:{userId}` | Last payment-profile id |

Both local + API for cart when logged in; API drives checkout UI when open order has lines.

---

## 5. Guest / session concepts

| Concept | Status |
|---------|--------|
| Guest checkout / anonymous cart API | **Backend ready** (`GUEST_SESSION_ID` + order-items + guest-checkout) |
| Guest session id / cookie cart | **Backend ready** |
| UI `isGuest` branches | Present but largely unreachable — proxy still forces login |
| Product detail add-to-cart | UI still requires login → `/login` until proxy opened |
| NextAuth + Keycloak session | Real auth; token refresh via JWT callback |
| Guest merge-on-login (API) | **`POST /api/v1/cart/merge`** (JWT + cookie) |
| Claim paid guest orders by email | Not yet (schema has `guestEmail`) |

---

## 6. Gaps vs guest-session ecommerce

- **UI still login-walls** storefront routes via `proxy.ts` — must open `/shop`, `/cart`, etc. and send cookies.
- Dual cart drift — localStorage + API; prefer API guest cart once UI is wired.
- Delivery checkout doesn’t always record payment; pickup does Bakong + `POST /payments` but no `check-transaction` polling.
- Two “favorites” systems — local product IDs vs authenticated brand favorites API.
- Categories/brands list not fully wired for customer catalog filtering.
- Claiming historical guest paid orders by email not implemented yet.

**Bottom line (backend):** guest session cookie cart + guest checkout + merge-on-login are implemented. **UI** must stop forcing login and send credentials for cookies.

---

## 7. Backend security (`SecurityConfig`)

| Rule | Paths |
|------|-------|
| `permitAll` | products, categories, files, bakong, auths, register, guest/**, order-items/**, `GET /orders`, `POST /orders/guest-checkout`, `GET /orders/*`, Swagger, … |
| JWT required | `GET /orders/history`, `POST /orders`, `POST /cart/merge`, payments, payment-profiles, favorites, reviews, notifications, … |
| `hasRole("admin")` | `/api/v1/admin/**` |

Reviews: public GET matcher is commented out — mutating reviews require JWT; product detail still embeds reviews via public `GET /products/{id}`.

Swagger: `http://localhost:8082/swagger-ui.html`

---

## 8. Admin API (`ROLE_admin`)

| Area | Paths |
|------|-------|
| Dashboard | `GET /api/v1/admin/dashboard/summary`, `…/revenue-chart`, `…/orders/recent` |
| Orders | `GET /api/v1/admin/orders`, `GET /api/v1/admin/orders/{id}` (includes `items[]`), `PATCH /api/v1/admin/orders/{id}/status` |
| Users | `GET /api/v1/admin/users` |
| Products | CRUD `/api/v1/admin/products` |
| Statistics | `GET /api/v1/admin/statistics` |

### Not implemented yet (UI may keep mocking)

- `GET /api/v1/admin/settings`
- Offers / promotions CRUD
- `DELETE /api/v1/admin/orders/{id}`
- Dedicated inventory / stock alerts endpoint
