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

## Checkout fulfillment (before payment / QR)

After login, the customer must choose **PICKUP** or **DELIVERY** and submit contact + location data **before** Bakong QR / payment.

### Shared request body (`POST`/`PUT` `/api/v1/payment-profiles` or `PATCH` `/api/v1/orders/{orderId}/fulfillment`)

```json
{
  "deliveryOption": "PICKUP",
  "fullName": "Sokha Chan",
  "contactNumber": "+85512345678",
  "deliveryAddress": null,
  "latitude": null,
  "longitude": null,
  "province": null,
  "district": null,
  "commune": null,
  "placeId": null,
  "formattedAddress": null,
  "deliveryInstructions": null,
  "pickupNotes": "Collect after 5pm"
}
```

**Delivery example (Phnom Penh):**

```json
{
  "deliveryOption": "DELIVERY",
  "fullName": "Sokha Chan",
  "contactNumber": "012345678",
  "deliveryAddress": "Near Central Market gate 2",
  "latitude": 11.5564,
  "longitude": 104.9282,
  "province": "Phnom Penh",
  "district": "Daun Penh",
  "commune": null,
  "placeId": "ChIJ…",
  "formattedAddress": "Phnom Penh, Cambodia",
  "deliveryInstructions": "Call on arrival",
  "pickupNotes": null
}
```

`PATCH /fulfillment` also accepts `"fulfillmentMethod": "PICKUP"` as an alias for `deliveryOption`.

### Validation rules

| Rule | Detail |
|------|--------|
| PICKUP | `fullName` 2–100 chars, Cambodia phone, `latitude`/`longitude` must be null |
| DELIVERY | Same contact rules + `latitude`/`longitude` required inside Cambodia (~lat 10–15, lng 102–108) |
| Order PATCH | JWT user must own order; order status must be `pending` or `processing` |
| Payment profile save | Also copies fulfillment onto the user's open pending cart order |

### Error responses (400 / 409)

ProblemDetail JSON includes `fieldErrors` when validation fails:

```json
{
  "status": 400,
  "detail": "Fulfillment validation failed",
  "fieldErrors": {
    "contactNumber": "Invalid Cambodia phone number",
    "latitude": "latitude and longitude are required for DELIVERY"
  }
}
```

### Sample curl — pickup

```bash
curl -X POST http://localhost:8082/api/v1/payment-profiles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"deliveryOption":"PICKUP","fullName":"Sokha Chan","contactNumber":"012345678","pickupNotes":"After 5pm"}'
```

### Sample curl — delivery on order

```bash
curl -X PATCH "http://localhost:8082/api/v1/orders/$ORDER_ID/fulfillment" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"deliveryOption":"DELIVERY","fullName":"Sokha Chan","contactNumber":"012345678","latitude":11.5564,"longitude":104.9282,"formattedAddress":"Phnom Penh, Cambodia","deliveryAddress":"Near Central Market"}'
```

Optional: pass `"savedLocationId": "<uuid>"` on payment-profile or fulfillment to copy lat/lng/address from a saved place (name/phone still required on the request; explicit fields override saved ones).

---

## Saved delivery locations (max 3)

Authenticated customers can save up to **3** Cambodia pins for faster DELIVERY checkout. Guests cannot save (401). Pickup does not use this API.

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| `GET` | `/api/v1/delivery-locations` | JWT | List mine; default first |
| `POST` | `/api/v1/delivery-locations` | JWT | Create; 4th → 400 |
| `PUT` | `/api/v1/delivery-locations/{id}` | JWT | Update (owner only) |
| `DELETE` | `/api/v1/delivery-locations/{id}` | JWT | Delete (owner only) |
| `PATCH` | `/api/v1/delivery-locations/{id}/default` | JWT | Set default (clears others) |

**Create body:**

```json
{
  "label": "Home",
  "deliveryAddress": "Near Central Market",
  "formattedAddress": "Phnom Penh, Cambodia",
  "latitude": 11.5564,
  "longitude": 104.9282,
  "province": "Phnom Penh",
  "district": null,
  "commune": null,
  "placeId": null,
  "deliveryInstructions": "Call when arriving",
  "isDefault": true
}
```

**Rules:** label 1–40 chars; lat/lng required inside Cambodia; max 3 per user; first location becomes default if none set.

```bash
curl -X POST http://localhost:8082/api/v1/delivery-locations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"label":"Home","latitude":11.5564,"longitude":104.9282,"formattedAddress":"Phnom Penh, Cambodia","isDefault":true}'
```

### Order / admin responses

Customer `OrderViewResponse` and admin order detail/list now include:

`fulfillment`, `customerName`, `contactNumber`, `deliveryAddress`, `latitude`, `longitude`, `province`, `district`, `commune`, `placeId`, `formattedAddress`, `deliveryInstructions`, `pickupNotes`.

Line items include `productId`, `unitPrice`, and line `price` (total).

---

## Cart lifecycle & merge (Bug A / Bug B)

### Open cart vs history

| Endpoint | Returns |
|----------|---------|
| `GET /api/v1/orders` | **Active cart only** — `pending` or `processing` with line items |
| `GET /api/v1/orders/history` | **Paid/completed** orders only |

Orders stay linked to `userId` after logout. Re-login → `GET /orders` shows the same pending/processing cart.

### Guest → user merge (Bug B)

When the client sends **Bearer JWT** and a guest session via **cookie** `GUEST_SESSION_ID` **or** header **`X-Session-Id`**:

1. Backend finds guest open order + user open order
2. Merges lines (same `productId` → sum qty)
3. Deletes empty guest order, clears guest cookie
4. Auto-runs on `GET /orders`, `POST /order-items`, and `POST /cart/merge`

Frontend should send `credentials: 'include'` and `X-Session-Id: {sessionId}` on authenticated cart calls until merge completes.

### Multiple open orders

If a user somehow has more than one `pending`/`processing` order, the backend consolidates them into one on the next `GET /orders` or cart write.

### Abandon open order

```bash
curl -X DELETE "http://localhost:8082/api/v1/orders/$ORDER_ID/abandon" \
  -H "Authorization: Bearer $TOKEN"
```

Restores stock, sets status `cancelled`. Only `pending`/`processing`.

### Duplicate line items (same product)

`POST /order-items` **upserts** on the open cart: if `productId` already exists on the order, quantity is increased and the existing row is updated — no second row is inserted.

Legacy duplicate rows (from before this fix) are merged automatically when loading `GET /orders`, `GET /orders/history`, or `GET /admin/orders/{id}`.

---

## Admin order API (invoice / modal)

`GET /api/v1/admin/orders` and `GET /api/v1/admin/orders/{id}` return:

- `userId`, `customerName` (from checkout `fullName` on the order — **not** Keycloak username)
- `customerEmail` (account email, secondary)
- `contactNumber`, `fulfillment` (`PICKUP` | `DELIVERY`)
- Full delivery fields: `deliveryAddress`, `formattedAddress`, `latitude`, `longitude`, `province`, `district`, `commune`, `deliveryInstructions`, `pickupNotes`
- `paymentMethod`, `paymentStatus` (from linked payment row)
- `items[]`: `{ id, productId, productName, quantity, price, unitPrice, image }`

If checkout fulfillment was not submitted, `customerName` is `null` — frontend should show `"—"` on invoice.

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
| `PATCH` | `/api/v1/orders/{orderId}/fulfillment` | Checkout fulfillment step (before QR) | **JWT required** | Own order, pending/processing only |
| `DELETE` | `/api/v1/orders/{orderId}/abandon` | Cancel open cart / restore stock | **JWT or guest session** | Sets status `cancelled` |
| `POST` | `/api/v1/order-items` | Shop + product detail “Add to cart” | **Public or JWT** | Upserts: same `productId` on open order → increments qty (no duplicate rows) |
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
| `POST` | `/api/v1/payment-profiles` | Cart checkout (details step) | **Required** | Optional `savedLocationId` for DELIVERY |
| `PUT` | `/api/v1/payment-profiles/{id}` | Cart checkout (update stored profile) | **Required** | Optional `savedLocationId` |
| `DELETE` | `/api/v1/payment-profiles/{id}` | Action exists — no cart UI delete | **Required** | |
| `GET` | `/api/v1/delivery-locations` | Checkout saved places | **Required** | Max 3 |
| `POST` | `/api/v1/delivery-locations` | Save place for later | **Required** | 4th → 400 |
| `PUT` | `/api/v1/delivery-locations/{id}` | Update saved place | **Required** | Owner only |
| `DELETE` | `/api/v1/delivery-locations/{id}` | Remove saved place | **Required** | Owner only |
| `PATCH` | `/api/v1/delivery-locations/{id}/default` | Set default place | **Required** | Clears other defaults |
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
Checkout fulfillment (before payment / QR):
  POST or PUT /payment-profiles  (also syncs open pending order)
  OR PATCH /orders/{orderId}/fulfillment
  Pickup: fullName + contactNumber (+ optional pickupNotes)
  Delivery: fullName + contactNumber + latitude/longitude in Cambodia (+ address fields)
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
| Payment profile | See **Fulfillment** section below |
| Order fulfillment | `PATCH /orders/{orderId}/fulfillment` — same body shape as payment profile |
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
| Dashboard | `GET /api/v1/admin/dashboard/summary` (revenue, cost, profit, margin), `…/revenue-chart` & `…/profit-chart` (`groupBy=month\|year`), `…/orders/recent` |
| Orders | `GET /api/v1/admin/orders`, `GET /api/v1/admin/orders/{id}` (includes `items[]`), `PATCH /api/v1/admin/orders/{id}/status` |
| Users | `POST/GET/PATCH /api/v1/admin/users` (Keycloak staff management — admin only) |
| Products | CRUD `/api/v1/admin/products` (GET list/detail: **admin + cashier**, includes `stockQuantity`; write: admin only) |

---

## 9. RBAC — admin | cashier | user

| Role | Backend access |
|------|----------------|
| `admin` | Full `/api/v1/admin/**`, user management, dashboard, product CRUD |
| `cashier` | `/api/v1/cashier/**`, `/api/v1/admin/orders/**` (list/detail/status), cart + payments (**CASH** allowed) |
| `user` | Storefront APIs; **cannot** use `paymentMethod: CASH` |

### Admin user management (`ROLE_admin`)

`POST /api/v1/admin/users` — create cashier only:

```json
{
  "username": "counter1",
  "email": "counter1@shop.test",
  "firstName": "Counter",
  "lastName": "Staff",
  "password": "TempP@ss1",
  "role": "cashier",
  "enabled": true,
  "temporaryPassword": true
}
```

`GET /api/v1/admin/users?page=&size=&role=cashier|user|admin&search=`

`PATCH /api/v1/admin/users/{keycloakId}` — `{ "enabled": false }` or `{ "temporaryPassword": "NewP@ss1", "temporaryPasswordFlag": true }`

### Cashier POS (`ROLE_cashier` or `ROLE_admin`)

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/api/v1/cashier/stock?page=&size=&name=` | Read-only stock (qty, low-stock flag) |
| `POST` | `/api/v1/cashier/checkout` | Walk-in customer details on open cart |
| `GET` | `/api/v1/cashier/orders/today` | Today's orders for counter |

Checkout body:

```json
{
  "customerName": "Walk-in Customer",
  "contactNumber": "+855…",
  "fulfillmentMethod": "pickup",
  "paymentMethod": "CASH"
}
```

Then `POST /api/v1/payments` with `paymentMethod: "CASH"`, `paymentStatus: "PAID"` (staff JWT required).

Cashier status updates: `PATCH /api/v1/admin/orders/{id}/status` — limited to `processing`, `paid`, `completed`, `shipped`, `ready`, `ready_for_pickup`, `dispatched`.

Keycloak setup: see [docs/keycloak-rbac-setup.md](keycloak-rbac-setup.md).

### Product batches & expiry (FEFO inventory)

Skincare stock is tracked in **batches** per product. Expired batches are excluded from customer `stockQuantity`. Sales deduct **FEFO** (earliest expiry first). Timezone: `Asia/Phnom_Penh`.

**Status enum:** `ACTIVE` | `EXPIRED` | `DEPLETED` | `WRITTEN_OFF`

| Field | Meaning |
|-------|---------|
| `initialQuantity` | Original received qty (set on create) |
| `quantity` | Current remaining |
| `soldQuantity` | **Computed** `max(0, initialQuantity - quantity)` — not stored |

**Write-off:** sets `quantity = 0`, `status = WRITTEN_OFF`, stores `writeOffReason`.

#### Product-scoped (keep for modal)

| Method | Path | Role |
|--------|------|------|
| `GET` | `/api/v1/admin/products/{productId}/batches` | admin, cashier |
| `POST` | `/api/v1/admin/products/{productId}/batches` | admin |
| `PUT` | `/api/v1/admin/products/{productId}/batches/{batchId}` | admin |
| `DELETE` | `/api/v1/admin/products/{productId}/batches/{batchId}` | admin |
| `POST` | `/api/v1/admin/products/{productId}/batches/{batchId}/write-off` | admin |
| `GET` | `/api/v1/admin/stock/alerts` | admin |

#### Global Batches page (new)

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/api/v1/admin/batches` | Paginated list + filters |
| `GET` | `/api/v1/admin/batches/{batchId}` | Detail + product snapshot |
| `POST` | `/api/v1/admin/batches` | Create (`productId` in body) |
| `PUT` | `/api/v1/admin/batches/{batchId}` | Update |
| `POST` | `/api/v1/admin/batches/{batchId}/write-off` | `{ "reason": "..." }` required |

**List query params:** `page`, `size`, `search` (product name / batchCode), `productId`, `status` (`ACTIVE`\|`EXPIRED`\|`DEPLETED`\|`WRITTEN_OFF`\|`ALL`), `expiringWithinDays` (e.g. `30`), `sort` (`expiryDateAsc`\|`expiryDateDesc`\|`receivedDateDesc`\|`quantityAsc`).

**List / detail item JSON:**

```json
{
  "id": "batch-uuid",
  "productId": "product-uuid",
  "productName": "Vitamin C Serum",
  "productImage": "https://.../image.jpg",
  "productBrand": "The Ordinary",
  "productCategory": "Serum",
  "batchCode": "LOT-2026-A",
  "expiryDate": "2026-06-01",
  "receivedDate": "2026-01-10",
  "initialQuantity": 100,
  "quantity": 42,
  "soldQuantity": 58,
  "costPrice": 8.5,
  "status": "ACTIVE",
  "writeOffReason": null,
  "createdAt": "2026-01-10T08:00:00Z",
  "updatedAt": "2026-03-01T12:00:00Z"
}
```

**Create (global):**

```bash
curl -X POST http://localhost:8082/api/v1/admin/batches \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":"33333333-3333-3333-3333-333333333334","batchCode":"LOT-2026-A","expiryDate":"2026-12-31","receivedDate":"2026-03-01","quantity":100,"costPrice":8.5}'
```

**List:**

```bash
curl "http://localhost:8082/api/v1/admin/batches?page=0&size=20&status=ACTIVE&sort=expiryDateAsc" \
  -H "Authorization: Bearer $TOKEN"
```

On create: `initialQuantity = quantity`; `status = ACTIVE` (or `EXPIRED` if `expiryDate < today` and `allowPastExpiry=true`).

**Public catalog:** `stockQuantity` = sum of non-expired active batches only. Batches are **not** exposed to customers.

**Migration:** `src/main/resources/db/migrate-product-batches.sql`

### Admin product cost & profit

**Product create/update** — add `costPrice` (what you pay per unit; not shown on public catalog):

```json
{
  "name": "Premium Serum",
  "price": 29.99,
  "costPrice": 12.00,
  "stockQuantity": 100,
  "categoryId": "…"
}
```

**Order lines** snapshot `unitCost` from `product.costPrice` when items are added (historical profit stays correct if cost changes later).

**Dashboard summary** — `GET /api/v1/admin/dashboard/summary`:

```json
{
  "totalRevenue": 50000,
  "totalCost": 22000,
  "totalProfit": 28000,
  "profitMarginPercent": 56.0,
  "totalOrders": 120,
  "totalUsers": 80,
  "growthRatePercent": 8.2,
  "ordersDeltaPercent": 5.0,
  "usersDeltaPercent": 2.1
}
```

**Chart** — `GET /api/v1/admin/dashboard/revenue-chart?from=&to=&groupBy=month|year` (same shape as `profit-chart`):

```json
[
  { "periodStart": "2026-01-01T00:00:00Z", "revenue": 5000, "cost": 2100, "profit": 2900 }
]
```

Only **paid / shipped / completed** orders count toward revenue, cost, and profit.
| Statistics | `GET /api/v1/admin/statistics` |

### Not implemented yet (UI may keep mocking)

- `GET /api/v1/admin/settings`
- Offers / promotions CRUD
- `DELETE /api/v1/admin/orders/{id}`
- Dedicated inventory / stock alerts endpoint
