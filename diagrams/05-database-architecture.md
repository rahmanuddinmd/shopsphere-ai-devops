# 05 — Database Architecture (PostgreSQL `shopsphere`, Flyway-managed)

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : grants
    USERS ||--o| CART : owns
    CART ||--o{ CART_ITEMS : contains
    PRODUCTS ||--o{ CART_ITEMS : "added as"
    CATEGORIES ||--o{ PRODUCTS : groups
    USERS ||--o{ ORDERS : places
    ORDERS ||--|{ ORDER_ITEMS : contains
    PRODUCTS ||--o{ ORDER_ITEMS : "ordered as"

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        timestamptz created_at
    }
    ROLES {
        bigint id PK
        varchar name UK
    }
    USER_ROLES {
        bigint user_id PK, FK
        bigint role_id PK, FK
    }
    CATEGORIES {
        bigint id PK
        varchar name UK
    }
    PRODUCTS {
        bigint id PK
        varchar sku UK
        varchar name
        text description
        numeric price
        int stock_quantity
        int ram_gb
        int storage_gb
        varchar processor
        varchar image_url
        bigint version
        bigint category_id FK
        boolean active
    }
    CART {
        bigint id PK
        bigint user_id FK, UK
    }
    CART_ITEMS {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        int quantity
    }
    ORDERS {
        bigint id PK
        bigint user_id FK
        varchar status
        numeric total_amount
        timestamptz created_at
    }
    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        varchar product_name
        numeric unit_price
        int quantity
    }
```

Design notes: `order_items` snapshots `product_name` and `unit_price` so order history does
not change when a product price changes later. Structured spec columns (`ram_gb`, `storage_gb`,
`processor`, `display`) let SQL and the AI assistant filter on RAM and price; `version` enables
optimistic locking at checkout. Passwords are stored only as BCrypt hashes.
