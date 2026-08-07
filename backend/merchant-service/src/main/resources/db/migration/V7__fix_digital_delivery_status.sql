-- Fix digital_deliveries missing status column
ALTER TABLE digital_deliveries ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
