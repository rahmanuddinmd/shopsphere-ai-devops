-- =====================================================================
-- ShopSphere AI - V2 reference and sample catalog data.
-- Image URLs point to files served by the frontend (frontend/public/images).
-- The admin user is NOT seeded here; it is created from ADMIN_EMAIL /
-- ADMIN_PASSWORD at startup (AdminUserInitializer).
-- =====================================================================

INSERT INTO roles (name) VALUES ('ADMIN'), ('CUSTOMER');

INSERT INTO categories (name, slug) VALUES
    ('Laptops', 'laptops'),
    ('Smartphones', 'smartphones'),
    ('Monitors', 'monitors'),
    ('Accessories', 'accessories');

INSERT INTO products (sku, name, brand, description, price, mrp, stock_quantity, image_url,
                      ram_gb, storage_gb, processor, display, badge, category_id)
VALUES
    ('LAP-NIM-14', 'Nimbus Pro 14', 'Nimbus',
     'A 14-inch workhorse for coding: 16 GB RAM for IDEs, Docker and browsers side by side, with a sharp 2.8K 120 Hz screen and all-day battery.',
     68990, 74990, 14, '/images/laptop-nimbus.jpg', 16, 512, 'AMD Ryzen 7, 8 cores', '14" 2.8K IPS, 120 Hz', 'Developer pick',
     (SELECT id FROM categories WHERE slug = 'laptops')),
    ('PHN-AUR-X5', 'Aurora X5', 'Aurora',
     'Flagship-class smartphone with a 6.5-inch AMOLED display, 8 GB RAM, 256 GB storage and a triple camera.',
     34999, 39999, 3, '/images/phone-aurora.jpg', 8, 256, 'Octa-core 4 nm', '6.5" AMOLED, 120 Hz', 'New',
     (SELECT id FROM categories WHERE slug = 'smartphones')),
    ('LAP-VTX-15', 'Vertex Book 15', 'Vertex',
     'Value 15.6-inch laptop with 16 GB RAM - ideal on a desk with an external monitor.',
     62490, NULL, 9, '/images/laptop-vertex.jpg', 16, 512, 'Intel Core i5, 10 cores', '15.6" Full HD IPS', NULL,
     (SELECT id FROM categories WHERE slug = 'laptops')),
    ('PHN-NOV-12', 'Nova 12', 'Nova',
     'Best-value smartphone with OLED display, 8 GB RAM and a 5000 mAh battery.',
     21499, 24999, 31, '/images/phone-nova.jpg', 8, 128, 'Octa-core 6 nm', '6.4" OLED, 90 Hz', 'Best value',
     (SELECT id FROM categories WHERE slug = 'smartphones')),
    ('MON-ORB-27', 'Orbit 27 4K', 'Orbit',
     '27-inch 4K IPS monitor with USB-C 65 W charging - one cable for laptop power, video and data. Great for reading code.',
     28990, NULL, 22, '/images/monitor-orbit.jpg', NULL, NULL, NULL, '27" 4K IPS, USB-C 65 W', 'Coder favourite',
     (SELECT id FROM categories WHERE slug = 'monitors')),
    ('LAP-KES-13', 'Kestrel Air 13', 'Kestrel',
     'Lightweight 1.2 kg laptop for students and travel.',
     45990, NULL, 18, '/images/laptop-kestrel.jpg', 8, 256, 'AMD Ryzen 5, 6 cores', '13.3" Full HD IPS', 'Lightweight',
     (SELECT id FROM categories WHERE slug = 'laptops')),
    ('ACC-PUL-TKL', 'Pulse TKL', 'Pulse',
     'Tenkeyless mechanical keyboard with hot-swappable switches and USB-C.',
     5490, NULL, 40, '/images/keyboard-pulse.jpg', NULL, NULL, NULL, NULL, NULL,
     (SELECT id FROM categories WHERE slug = 'accessories')),
    ('ACC-ECH-ANC', 'Echo ANC', 'Echo',
     'Over-ear noise-cancelling headphones with 40-hour battery life.',
     8990, 10990, 25, '/images/headphones-echo.jpg', NULL, NULL, NULL, NULL, NULL,
     (SELECT id FROM categories WHERE slug = 'accessories'));
