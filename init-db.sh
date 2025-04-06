#!/bin/bash

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
until PGPASSWORD=postgres psql -h localhost -U postgres -d sample_application -c '\q'; do
  echo "PostgreSQL is unavailable - sleeping"
  sleep 1
done
echo "PostgreSQL is up - executing schema"

# Create tables
PGPASSWORD=postgres psql -h localhost -U postgres -d sample_application << EOF
-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
  id SERIAL PRIMARY KEY,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(20) NOT NULL,
  customer_id INTEGER NOT NULL
);

-- Create order_items table
CREATE TABLE IF NOT EXISTS order_items (
  id SERIAL PRIMARY KEY,
  order_id INTEGER NOT NULL REFERENCES orders(id),
  product_id INTEGER NOT NULL,
  quantity INTEGER NOT NULL
);

-- Create payments table
CREATE TABLE IF NOT EXISTS payments (
  id SERIAL PRIMARY KEY,
  order_id INTEGER NOT NULL REFERENCES orders(id),
  amount DECIMAL(10,2) NOT NULL,
  payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL,
  last_active_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- Create customer_addresses table
CREATE TABLE IF NOT EXISTS customer_addresses (
  id SERIAL PRIMARY KEY,
  customer_id INTEGER NOT NULL REFERENCES customers(id),
  address_line1 VARCHAR(100) NOT NULL,
  address_line2 VARCHAR(100),
  city VARCHAR(50) NOT NULL,
  state VARCHAR(50) NOT NULL,
  zip_code VARCHAR(20) NOT NULL,
  country VARCHAR(50) NOT NULL
);

-- Create cleanup_task_log table
CREATE TABLE IF NOT EXISTS cleanup_task_log (
  id SERIAL PRIMARY KEY,
  task_id VARCHAR(36) NOT NULL,
  entity_name VARCHAR(50) NOT NULL,
  start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  end_time TIMESTAMP,
  status VARCHAR(20) NOT NULL,
  records_processed INTEGER DEFAULT 0,
  records_deleted INTEGER DEFAULT 0,
  error_message TEXT
);

-- Insert sample data
INSERT INTO customers (name, email, last_active_date, account_status)
VALUES 
  ('John Doe', 'john@example.com', CURRENT_TIMESTAMP - INTERVAL '1 year', 'ACTIVE'),
  ('Jane Smith', 'jane@example.com', CURRENT_TIMESTAMP - INTERVAL '2 years', 'CLOSED'),
  ('Bob Johnson', 'bob@example.com', CURRENT_TIMESTAMP - INTERVAL '3 years', 'CLOSED');

-- Insert sample orders
INSERT INTO orders (created_at, status, customer_id)
VALUES 
  (CURRENT_TIMESTAMP - INTERVAL '2 years', 'COMPLETED', 1),
  (CURRENT_TIMESTAMP - INTERVAL '1 year', 'COMPLETED', 1),
  (CURRENT_TIMESTAMP - INTERVAL '6 months', 'CANCELLED', 2),
  (CURRENT_TIMESTAMP - INTERVAL '3 months', 'PENDING', 3);

-- Insert sample order items
INSERT INTO order_items (order_id, product_id, quantity)
VALUES 
  (1, 101, 2),
  (1, 102, 1),
  (2, 103, 3),
  (3, 104, 1),
  (4, 105, 2);

-- Insert sample payments
INSERT INTO payments (order_id, amount, payment_date)
VALUES 
  (1, 199.99, CURRENT_TIMESTAMP - INTERVAL '2 years'),
  (2, 299.99, CURRENT_TIMESTAMP - INTERVAL '1 year'),
  (3, 99.99, CURRENT_TIMESTAMP - INTERVAL '6 months');

-- Insert sample customer addresses
INSERT INTO customer_addresses (customer_id, address_line1, city, state, zip_code, country)
VALUES 
  (1, '123 Main St', 'New York', 'NY', '10001', 'USA'),
  (2, '456 Oak Ave', 'Los Angeles', 'CA', '90001', 'USA'),
  (3, '789 Pine Rd', 'Chicago', 'IL', '60601', 'USA');

EOF

echo "Database initialization completed!" 