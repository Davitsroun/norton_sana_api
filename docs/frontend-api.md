# Frontend API reference

**Base URL:** `http://localhost:8082`  
**Auth:** `Authorization: Bearer <access_token>` (Keycloak JWT) unless marked **Public**.

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

**Frontend tip:** For user orders, read `payload` (array). For catalog/admin lists, read `items`.

---

## 1. Orders (user)

| Method | Path | Query | Body |
|--------|------|-------|------|
| `POST` | `/api/v1/orders` | — | Checkout |
| `GET` | `/api/v1/orders` | — | Active carts only |
| `GET` | `/api/v1/orders/history` | — | Paid / completed |
| `GET` | `/api/v1/orders/{orderId}` | — | One order |

- **No `page` / `size`** on list or history.
- After payment, **`GET /orders`** is often `[]`; use **`/orders/history`**.

### Checkout body (supports frontend field names)

```json
{
  "items": [{ "productId": "11111111-1111-1111-1111-111111111111", "quantity": 2 }],
  "customerName": "Jane Doe",
  "contactNumber": "+85512345678",
  "fulfillmentMethod": "delivery",
  "deliveryAddress": "123 Street",
  "paymentMethod": "khqr"
}
```

Also accepts `fulfillment` instead of `fulfillmentMethod`.

### Success: create order (`201`)

```json
{
  "success": true,
  "message": "Order created successfully.",
  "status": "CREATED",
  "payload": {
    "id": "8442f269-4e0c-4698-ac2c-e67cd3fca646",
    "date": "2026-05-13T11:02:57.782111Z",
    "items": [
      {
        "id": "4a4ff70e-3fcb-4b6d-8573-a532babef670",
        "productName": "The Ordinary Niacinamide 10% + Zinc 1%",
        "quantity": 2,
        "price": 15.0,
        "image": "https://images.unsplash.com/photo-..."
      }
    ],
    "total": 15.0,
    "status": "pending",
    "trackingNumber": null,
    "paymentMethod": "khqr",
    "fulfillment": "delivery",
    "customerName": "Jane Doe",
    "contactNumber": "+85512345678"
  },
  "timestamps": "2026-05-18T11:04:33.275561Z"
}
```

### Success: active orders (`GET /orders`)

```json
{
  "success": true,
  "message": "Active orders retrieved successfully.",
  "status": "OK",
  "payload": [ /* same order shape; status pending | processing */ ],
  "timestamps": "2026-05-18T11:04:33.275561Z"
}
```

### Success: history (`GET /orders/history`)

```json
{
  "success": true,
  "message": "Order history retrieved successfully.",
  "status": "OK",
  "payload": [
    {
      "id": "8442f269-4e0c-4698-ac2c-e67cd3fca646",
      "date": "2026-05-13T11:02:57.782111Z",
      "items": [ { "id": "…", "productName": "…", "quantity": 1, "price": 7.5, "image": "https://..." } ],
      "total": 7.5,
      "status": "paid",
      "trackingNumber": null,
      "paymentMethod": "khqr",
      "fulfillment": "delivery",
      "customerName": "Jane Doe",
      "contactNumber": "+85512345678"
    }
  ],
  "timestamps": "2026-05-18T11:04:33.275561Z"
}
```

---

## 2. Payments

| Method | Path | Body |
|--------|------|------|
| `POST` | `/api/v1/payments` | Create |
| `PUT` | `/api/v1/payments/{id}` | Update |

### Success payment body

```json
{
  "orderId": "8442f269-4e0c-4698-ac2c-e67cd3fca646",
  "paymentMethod": "khqr",
  "paymentStatus": "PAID",
  "transactionId": "txn-001",
  "paidAt": "2026-05-13T12:30:00Z"
}
```

**Success statuses:** `PAID`, `SUCCESS`, `COMPLETED`, `SUCCEEDED` (any case).

### Success response (`201`)

```json
{
  "success": true,
  "message": "Payment created successfully.",
  "status": "CREATED",
  "payload": {
    "paymentId": "pay-uuid",
    "paymentMethod": "khqr",
    "paymentStatus": "SUCCESS",
    "transactionId": "txn-001",
    "paidAt": "2026-05-13T12:30:00Z"
  },
  "timestamps": "2026-05-18T12:00:00Z"
}
```

**Side effects on success:** order → `paid`, notification created, other pending carts cleared.

### Failed / pending (no order status change)

```json
{
  "orderId": "8442f269-4e0c-4698-ac2c-e67cd3fca646",
  "paymentMethod": "khqr",
  "paymentStatus": "PENDING",
  "transactionId": null,
  "paidAt": null
}
```

---

## 3. Bakong (**Public**)

| Method | Path |
|--------|------|
| `POST` | `/api/v1/bakong/generate-qr` |
| `POST` | `/api/v1/bakong/get-qr-image` |
| `POST` | `/api/v1/bakong/check-transaction` |

### Check transaction (success → same as payment success)

```json
{
  "md5": "hash-from-qr",
  "orderId": "8442f269-4e0c-4698-ac2c-e67cd3fca646"
}
```

```json
{
  "responseCode": 0,
  "responseMessage": "Success",
  "errorCode": null,
  "data": { }
}
```

---

## 4. Products & categories (**Public**)

| Method | Path | Query |
|--------|------|-------|
| `GET` | `/api/v1/products` | `page`, `size`, `categoryId`, `minPrice`, `maxPrice` |
| `GET` | `/api/v1/products/{id}` | — |
| `GET` | `/api/v1/categories` | — |

### Success: product list

```json
{
  "items": [
    {
      "id": "prod-uuid",
      "brandId": null,
      "name": "Premium Serum",
      "price": 29.99,
      "originalPrice": null,
      "image": "https://...",
      "imageUrl2": "",
      "imageUrl3": "",
      "imageUrl4": "",
      "rating": 4.5,
      "reviews": 12,
      "category": "Serums",
      "description": "...",
      "badge": null
    }
  ],
  "paginationResponse": {
    "totalElements": 50,
    "currentPage": 0,
    "pageSize": 10,
    "totalPages": 5
  }
}
```

### Success: product detail

```json
{
  "success": true,
  "payload": {
    "product": { "id": "…", "name": "…", "price": 29.99 },
    "relateProduct": [ ],
    "reviewver": [ ]
  }
}
```

---

## 5. Order items (cart CRUD)

| Method | Path | Body |
|--------|------|------|
| `POST` | `/api/v1/order-items` | `{ "productId": "uuid", "quantity": 1 }` |
| `GET` | `/api/v1/order-items` | `page`, `size` |
| `PUT` | `/api/v1/order-items/{id}` | `{ "quantity": 2 }` |
| `DELETE` | `/api/v1/order-items/{id}` | — |

---

## 6. User notifications

| Method | Path | Body |
|--------|------|------|
| `GET` | `/api/v1/user-notifications` | — |
| `PUT` | `/api/v1/user-notifications/{id}` | `{ "read": true }` |

### Success: list (includes auto `PAYMENT_SUCCESS`)

```json
{
  "success": true,
  "payload": [
    {
      "notificationId": "uuid",
      "userId": "uuid",
      "type": "PAYMENT_SUCCESS",
      "title": "Payment successful",
      "body": "Your payment for order … was successful. Total: 30 USD.",
      "orderId": "8442f269-4e0c-4698-ac2c-e67cd3fca646",
      "paymentId": "uuid",
      "read": false,
      "createdAt": "2026-05-13T12:30:00Z"
    }
  ]
}
```

---

## 7. Admin API (`ROLE_admin` required)

### Dashboard

| Method | Path | Query |
|--------|------|-------|
| `GET` | `/api/v1/admin/dashboard/summary` | — |
| `GET` | `/api/v1/admin/dashboard/revenue-chart` | optional `from`, `to` (ISO-8601) |
| `GET` | `/api/v1/admin/orders/recent` | `limit` (default 10, max 50) |

#### Success: dashboard summary

```json
{
  "success": true,
  "payload": {
    "totalRevenue": 24580.5,
    "totalOrders": 1234,
    "totalUsers": 5678,
    "growthRatePercent": 8.2,
    "ordersDeltaPercent": 8.2,
    "usersDeltaPercent": 0.0
  }
}
```

#### Success: revenue chart

```json
{
  "success": true,
  "payload": [
    { "periodStart": "2026-01-01T00:00:00Z", "revenue": 1200.0 },
    { "periodStart": "2026-02-01T00:00:00Z", "revenue": 1850.5 }
  ]
}
```

#### Success: recent orders

```json
{
  "success": true,
  "payload": [
    {
      "id": "8442f269-4e0c-4698-ac2c-e67cd3fca646",
      "customerName": "Brooklyn Zoe",
      "customerEmail": "brooklyn@example.com",
      "deliveryAddress": "302 Snyder Street",
      "placedAt": "2020-07-31T10:00:00Z",
      "totalAmount": 64.0,
      "currency": "USD",
      "status": "PAID",
      "avatarUrl": "https://..."
    }
  ]
}
```

### Admin orders

| Method | Path | Query / body |
|--------|------|----------------|
| `GET` | `/api/v1/admin/orders` | `page`, `size`, optional `status` |
| `PATCH` | `/api/v1/admin/orders/{id}/status` | `{ "status": "shipped", "trackingNumber": "TRK-001" }` |

#### Success: admin order list

```json
{
  "items": [
    {
      "id": "8442f269-4e0c-4698-ac2c-e67cd3fca646",
      "customerName": "Brooklyn Zoe",
      "customerEmail": "brooklyn@example.com",
      "deliveryAddress": "302 Snyder Street",
      "placedAt": "2020-07-31T10:00:00Z",
      "totalAmount": 64.0,
      "currency": "USD",
      "status": "PENDING",
      "avatarUrl": "https://..."
    }
  ],
  "paginationResponse": { "totalElements": 1, "currentPage": 0, "pageSize": 10, "totalPages": 1 }
}
```

### Admin users

`GET /api/v1/admin/users?page=&size=`

```json
{
  "items": [
    {
      "id": "67fc41cd-3cce-4cc4-b28c-cf561421f7e1",
      "name": "Sroun Davit",
      "email": "sroundavit@gmail.com",
      "joinedAt": "2023-01-01T00:00:00Z",
      "orderCount": 12,
      "status": "ACTIVE",
      "role": "CUSTOMER",
      "avatarUrl": "https://..."
    }
  ],
  "paginationResponse": { "totalElements": 1, "currentPage": 0, "pageSize": 10, "totalPages": 1 }
}
```

### Admin products

| Method | Path |
|--------|------|
| `GET` | `/api/v1/admin/products` |
| `POST` | `/api/v1/admin/products` |
| `PUT` | `/api/v1/admin/products/{id}` |
| `DELETE` | `/api/v1/admin/products/{id}` |

```json
{
  "name": "Premium CBD Oil",
  "description": "...",
  "price": 49.99,
  "stockQuantity": 150,
  "imageUrl": "https://...",
  "categoryId": "category-uuid",
  "brandId": null
}
```

### Statistics

`GET /api/v1/admin/statistics` or `/api/v1/admin/statistics/overview`

```json
{
  "success": true,
  "payload": {
    "totalUsers": 120,
    "totalProducts": 45,
    "totalOrders": 890
  }
}
```

---

## 8. End-to-end checkout flow (frontend)

1. `POST /api/v1/orders` → get `payload.id`
2. Pay via `POST /api/v1/payments` with `paymentStatus: "PAID"` **or** Bakong `check-transaction` with `orderId`
3. `GET /api/v1/orders` → `payload: []`
4. `GET /api/v1/orders/history` → paid order
5. `GET /api/v1/user-notifications` → `PAYMENT_SUCCESS`

---

## Not implemented yet (UI may keep mocking)

- `GET /api/v1/admin/settings`
- Offers / promotions CRUD
- `DELETE /api/v1/admin/orders/{id}`
- Dedicated inventory / stock alerts endpoint

Swagger: `http://localhost:8082/swagger-ui.html`
