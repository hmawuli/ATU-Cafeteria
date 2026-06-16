-- ===========================================================================
-- ACCRA TECHNICAL UNIVERSITY (ATU) CAFETERIA APP & FINAL YEAR PROJECT
-- PostgreSQL Database System Schema Design
-- ===========================================================================

-- Enable UUID Extension for robust ID generation if required
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ===========================================================================
-- Table 1: USERS (Core accounts registry for Students, Vendors, Admins)
-- ===========================================================================
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'student' CHECK (role IN ('student', 'vendor', 'administrator')),
    info_text VARCHAR(255), -- Serves as Student ID (e.g. 'Index: 0123456') or Vendor Booth info
    profile_picture_url VARCHAR(512),
    loyalty_points INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast user authentication lookup
CREATE INDEX idx_users_email ON users(email);

-- ===========================================================================
-- Table 2: PROJECTS (Academic registry supporting final-year projects)
-- ===========================================================================
CREATE TABLE academic_projects (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    department VARCHAR(150) NOT NULL,
    supervisor_id INT REFERENCES users(id) ON DELETE SET NULL,
    student_id INT REFERENCES users(id) ON DELETE SET NULL,
    github_repository_url VARCHAR(512),
    project_status VARCHAR(40) DEFAULT 'PROPOSED' CHECK (project_status IN ('PROPOSED', 'APPROVED', 'IN_PROGRESS', 'REVIEWED', 'COMPLETED')),
    submission_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for rapid relationship lookups
CREATE INDEX idx_projects_student ON academic_projects(student_id);
CREATE INDEX idx_projects_supervisor ON academic_projects(supervisor_id);

-- ===========================================================================
-- Table 3: FOOD_ITEMS (Menu items provided by vendors)
-- ===========================================================================
CREATE TABLE food_items (
    id SERIAL PRIMARY KEY,
    vendor_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    category VARCHAR(80) NOT NULL, -- e.g., 'Breakfast', 'Local Dish', 'Fast Food', 'Dessert'
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    calories INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_food_items_vendor ON food_items(vendor_id);
CREATE INDEX idx_food_items_category ON food_items(category);

-- ===========================================================================
-- Table 4: ORDERS (Real-time tracking of pre-orders placed by students)
-- ===========================================================================
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    student_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vendor_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_item_id INT NOT NULL REFERENCES food_items(id) ON DELETE RESTRICT,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    total_price NUMERIC(10, 2) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PREPARING', 'READY', 'COMPLETED', 'DELIVERED', 'DECLINED', 'CANCELLED')),
    payment_method VARCHAR(40) NOT NULL DEFAULT 'WALLET' CHECK (payment_method IN ('WALLET', 'MOBILE_MONEY', 'CASH')),
    qr_code_token VARCHAR(255) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_student ON orders(student_id);
CREATE INDEX idx_orders_vendor ON orders(vendor_id);
CREATE INDEX idx_orders_status ON orders(status);

-- ===========================================================================
-- Table 5: FEEDBACK & REVIEWS (Multi-metric evaluation logs)
-- ===========================================================================
CREATE TABLE feedback (
    id SERIAL PRIMARY KEY,
    order_id INT UNIQUE REFERENCES orders(id) ON DELETE SET NULL,
    customer_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vendor_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    comment TEXT,
    rating_food_quality INT NOT NULL CHECK (rating_food_quality BETWEEN 1 AND 5),
    rating_cleanliness INT NOT NULL CHECK (rating_cleanliness BETWEEN 1 AND 5),
    rating_service_speed INT NOT NULL CHECK (rating_service_speed BETWEEN 1 AND 5),
    rating_price_value INT NOT NULL CHECK (rating_price_value BETWEEN 1 AND 5),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_feedback_vendor ON feedback(vendor_id);

-- ===========================================================================
-- Table 6: WALLET_TRANSACTIONS (Loyalty cash deposits and pre-payments ledger)
-- ===========================================================================
CREATE TABLE wallet_transactions (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount NUMERIC(10, 2) NOT NULL,
    transaction_type VARCHAR(40) NOT NULL CHECK (transaction_type IN ('DEPOSIT', 'PURCHASE', 'REFUND')),
    reference_tag VARCHAR(180) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_user ON wallet_transactions(user_id);

-- ===========================================================================
-- UTILITY TRIGGERS: Automatic `updated_at` Refresh
-- ===========================================================================

CREATE OR REPLACE FUNCTION update_timestamp_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply timestamp updater triggers
CREATE TRIGGER trigger_update_users BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();

CREATE TRIGGER trigger_update_academic_projects BEFORE UPDATE ON academic_projects
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();

CREATE TRIGGER trigger_update_food_items BEFORE UPDATE ON food_items
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();

CREATE TRIGGER trigger_update_orders BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
