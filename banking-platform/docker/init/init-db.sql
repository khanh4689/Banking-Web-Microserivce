-- Create databases for auth-service and user-service
-- This script runs automatically on first PostgreSQL container start
SELECT 'CREATE DATABASE auth_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db')\gexec
SELECT 'CREATE DATABASE user_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'user_db')\gexec
SELECT 'CREATE DATABASE account_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'account_db')\gexec
SELECT 'CREATE DATABASE transaction_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'transaction_db')\gexec
