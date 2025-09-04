CREATE DATABASE apartment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
use apartment_db;
INSERT INTO room (number, status) VALUES
 (101, 'OCCUPIED'),
 (102, 'OCCUPIED'),
 (201, 'FREE');

INSERT INTO tenant (name, phone, line_id) VALUES
 ('John Doe', '080-123-4567', 'johnline'),
 ('Jane Smith', '081-222-3333', 'jane_line');


SET SQL_SAFE_UPDATES = 0;

UPDATE room SET tenant_id = 1 WHERE number = 101;

UPDATE room SET tenant_id = 2 WHERE number = 102;

UPDATE room SET tenant_id = NULL WHERE number = 201;

select * from invoice;