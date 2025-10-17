-- -- Initialize database schema for CUI Integration MCP Server
-- -- This is a minimal schema for testing purposes

-- CREATE DATABASE IF NOT EXISTS cui_integration;
-- USE cui_integration;

-- -- Create admin schema
-- CREATE SCHEMA IF NOT EXISTS te_admin;

-- -- Users table
-- CREATE TABLE IF NOT EXISTS te_admin.tb_users (
--     uid BIGINT PRIMARY KEY,
--     name VARCHAR(255),
--     email VARCHAR(255) NOT NULL UNIQUE,
--     flag_registered BOOLEAN DEFAULT FALSE,
--     create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     delete_time TIMESTAMP NULL
-- );

-- -- Organizations table
-- CREATE TABLE IF NOT EXISTS te_admin.tb_organizations (
--     org_id BIGINT PRIMARY KEY,
--     organization_name VARCHAR(255) NOT NULL,
--     flag_cui_migrated BOOLEAN DEFAULT FALSE,
--     date_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     delete_time TIMESTAMP NULL
-- );

-- -- Accounts table
-- CREATE TABLE IF NOT EXISTS te_admin.tb_accounts (
--     aid BIGINT PRIMARY KEY,
--     org_id BIGINT NOT NULL,
--     account_name VARCHAR(255),
--     create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     delete_time TIMESTAMP NULL,
--     FOREIGN KEY (org_id) REFERENCES te_admin.tb_organizations(org_id)
-- );

-- -- User accounts mapping
-- CREATE TABLE IF NOT EXISTS te_admin.tb_users_accounts (
--     uid BIGINT,
--     aid BIGINT,
--     create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     PRIMARY KEY (uid, aid),
--     FOREIGN KEY (uid) REFERENCES te_admin.tb_users(uid),
--     FOREIGN KEY (aid) REFERENCES te_admin.tb_accounts(aid)
-- );

-- -- CUI tenant mapping
-- CREATE TABLE IF NOT EXISTS te_admin.tb_organization_cui_tenant_mapping (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     org_id BIGINT NOT NULL,
--     cui_tenant_id VARCHAR(255),
--     cui_org_id VARCHAR(255),
--     cui_cluster_url VARCHAR(255),
--     status VARCHAR(50) DEFAULT 'PENDING',
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     FOREIGN KEY (org_id) REFERENCES te_admin.tb_organizations(org_id)
-- );

-- -- User properties for CUI metadata
-- CREATE TABLE IF NOT EXISTS te_admin.tb_users_metadata (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     uid BIGINT NOT NULL,
--     property_name VARCHAR(255) NOT NULL,
--     property_value TEXT,
--     create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     FOREIGN KEY (uid) REFERENCES te_admin.tb_users(uid),
--     UNIQUE KEY unique_user_property (uid, property_name)
-- );

-- -- CUI entity sync retry status
-- CREATE TABLE IF NOT EXISTS te_admin.tb_cui_entity_sync_retry_status (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     entity_id VARCHAR(255) NOT NULL,
--     entity_type VARCHAR(50) NOT NULL,
--     sync_type VARCHAR(50),
--     status VARCHAR(50) DEFAULT 'PENDING',
--     error_message TEXT,
--     retry_count INT DEFAULT 0,
--     max_retries INT DEFAULT 3,
--     last_attempt TIMESTAMP NULL,
--     next_retry TIMESTAMP NULL,
--     create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     INDEX idx_entity (entity_id, entity_type),
--     INDEX idx_status (status),
--     INDEX idx_create_time (create_time)
-- );

-- -- Insert sample data for testing
-- INSERT IGNORE INTO te_admin.tb_organizations (org_id, organization_name, cui_migration_flag) VALUES
-- (1, 'Test Organization 1', TRUE),
-- (2, 'Test Organization 2', FALSE),
-- (3, 'Demo Organization', TRUE);

-- INSERT IGNORE INTO te_admin.tb_users (uid, name, email, flag_registered) VALUES
-- (1001, 'John Doe', 'john.doe@example.com', TRUE),
-- (1002, 'Jane Smith', 'jane.smith@example.com', TRUE),
-- (1003, 'Bob Johnson', 'bob.johnson@example.com', FALSE);

-- INSERT IGNORE INTO te_admin.tb_accounts (aid, org_id, account_name) VALUES
-- (2001, 1, 'Test Account 1'),
-- (2002, 1, 'Test Account 2'),
-- (2003, 2, 'Demo Account'),
-- (2004, 3, 'Sample Account');

-- INSERT IGNORE INTO te_admin.tb_users_accounts (uid, aid) VALUES
-- (1001, 2001),
-- (1001, 2002),
-- (1002, 2001),
-- (1002, 2003),
-- (1003, 2004);

-- INSERT IGNORE INTO te_admin.tb_organization_cui_tenant_mapping (org_id, cui_tenant_id, cui_org_id, cui_cluster_url, status) VALUES
-- (1, 'tenant-123', 'org-456', 'https://cui-cluster-1.example.com', 'SUCCESS'),
-- (3, 'tenant-789', 'org-012', 'https://cui-cluster-2.example.com', 'SUCCESS');

-- INSERT IGNORE INTO te_admin.tb_user_properties (uid, property_name, property_value) VALUES
-- (1001, 'cui_user_metadata', '{"cuiUserId": "cui-user-1001", "cuiOrgId": "org-456"}'),
-- (1002, 'cui_user_metadata', '{"cuiUserId": "cui-user-1002", "cuiOrgId": "org-456"}');

-- INSERT IGNORE INTO te_admin.tb_cui_entity_sync_retry_status (entity_id, entity_type, sync_type, status, retry_count) VALUES
-- ('1001', 'USER', 'PROFILE_SYNC', 'SUCCESS', 0),
-- ('1002', 'USER', 'TENANT_SYNC', 'FAILED', 2),
-- ('1', 'ORGANIZATION', 'TENANT_MIRROR', 'SUCCESS', 0),
-- ('2', 'ORGANIZATION', 'SSO_CONFIG', 'PENDING', 1);

-- COMMIT;
