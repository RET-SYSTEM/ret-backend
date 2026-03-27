# RET Backend — Spring Boot API

Personal finance REST API for ingesting, categorizing, and managing Montenegrin fiscal receipts.

## Tech Stack

- Java 17, Spring Boot 3, Spring Data JPA
- PostgreSQL (via `ddl-auto=update`)
- Lombok, Jakarta Validation

## How to Run

```bash
# Set environment variables (or use .env)
export DB_HOST=localhost DB_PORT=5432 DB_NAME=ret
export DB_USERNAME=postgres DB_PASSWORD=yourpassword
export SPRING_PORT=8080
export PYTHON_WORKER_URL=http://localhost:5000

./mvnw spring-boot:run
```

---

## Entities

### Invoice
| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-generated |
| `iic` | VARCHAR (UNIQUE) | Nullable for manual entries (Plan C) |
| `date_time` | TIMESTAMP | |
| `total_amount` | DECIMAL(10,2) | |
| `store_name` | VARCHAR | |
| `is_card` | BOOLEAN | |

### InvoiceItem
| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-generated |
| `invoice_id` | FK → invoices | CASCADE delete (orphanRemoval) |
| `item_name` | VARCHAR NOT NULL | |
| `category_id` | FK → categories | Nullable |
| `price` | DECIMAL(10,2) | |
| `quantity` | DECIMAL(10,4) | |

### Category
| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-generated |
| `name` | VARCHAR UNIQUE | Case-insensitive lookup |

---

## API Endpoints

### Receipts

#### `POST /api/receipts/process`
Process a receipt via QR scan (Plan A), manual receipt fields (Plan B), or manual item entry (Plan C).

**Plan A** — QR URL:
```json
{ "url": "https://mapr.tax.gov.me/ic/#/verify?iic=abc123&tin=02697904&crtd=..." }
```

**Plan B** — Manual fields:
```json
{ "iic": "abc123", "tin": "02697904", "dateTimeCreated": "2026-03-14T12:20:54 01:00" }
```

**Plan C** — Manual entry (no receipt):
```json
{
  "manualEntry": true,
  "storeName": "Grocery Store",
  "totalAmount": 25.50,
  "isCard": true,
  "items": [
    { "itemName": "Bread", "category": "Groceries", "price": 1.50, "quantity": 2 }
  ]
}
```

**Response:** `201 Created` (new) or `200 OK` (duplicate) → `InvoiceResponse`

---

#### `GET /api/receipts?page=0&size=20`
Paginated list of all invoices, newest first.

**Response:** Spring `Page<InvoiceResponse>`

---

#### `PUT /api/receipts/{id}`
Update invoice header fields. Only non-null fields are updated (partial update).

```json
{ "storeName": "Updated Name", "isCard": false }
```

**Response:** `200 OK` → updated `InvoiceResponse`
**Error:** `404` if invoice not found

---

#### `DELETE /api/receipts/{id}`
Delete an invoice and all its items (cascade).

**Response:** `204 No Content`
**Error:** `404` if invoice not found

---

#### `PUT /api/receipts/{invoiceId}/items/{itemId}`
Update a single item on an invoice. Category is resolved by name (case-insensitive).

```json
{ "itemName": "Updated", "category": "Food", "price": 3.99, "quantity": 1 }
```

**Response:** `200 OK` → full `InvoiceResponse` (with all items)
**Error:** `404` if invoice or item not found

---

#### `DELETE /api/receipts/{invoiceId}/items/{itemId}`
Remove a single item from an invoice.

**Response:** `200 OK` → updated `InvoiceResponse` (without the deleted item)
**Error:** `404` if invoice or item not found

---

#### `GET /api/receipts/export`
Download all receipts as CSV.

**Response:** `200 OK` → `text/csv` file download

---

### Categories

#### `GET /api/categories`
List all categories (alphabetical).

**Response:** `200 OK` → `CategoryResponse[]`

#### `POST /api/categories`
Create a new category.

```json
{ "name": "Entertainment" }
```

**Response:** `201 Created` → `CategoryResponse`
**Error:** `400` if name is blank or already exists

#### `DELETE /api/categories/{id}`
Delete a category.

**Response:** `204 No Content`
**Error:** `400` if not found

---

## Response DTOs

### InvoiceResponse
```json
{
  "id": 1,
  "iic": "abc123",
  "dateTime": "2026-03-14T12:20:54",
  "totalAmount": 59.90,
  "storeName": "INDITEX Montenegro d.o.o.",
  "isCard": true,
  "items": [
    {
      "id": 8,
      "itemName": "PANTALONE",
      "category": "Clothing",
      "price": 29.95,
      "quantity": 1.0
    }
  ]
}
```

### CategoryResponse
```json
{ "id": 1, "name": "Groceries" }
```

### ApiErrorResponse
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Invoice with id 999 not found.",
  "timestamp": "2026-03-27T00:20:00",
  "details": null
}
```

---

## Architecture

```
controller/          REST endpoints (thin — delegates to service)
service/             Business logic interface
service/impl/        Business logic implementation
repository/          Spring Data JPA repositories
entity/              JPA entities (Invoice, InvoiceItem, Category)
dto/request/         Incoming request DTOs
dto/response/        Outgoing response DTOs
dto/worker/          Python worker response mapping
mapper/              Entity ↔ DTO conversion
exception/           Global exception handler + custom exceptions
config/              CORS, RestClient bean
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `ret` | Database name |
| `DB_USERNAME` | `postgres` | DB username |
| `DB_PASSWORD` | — | DB password |
| `SPRING_PORT` | `8080` | Server port |
| `PYTHON_WORKER_URL` | `http://localhost:5000` | Python worker base URL |
