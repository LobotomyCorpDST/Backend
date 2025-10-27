-- Sample Data Initialization Script for Apartment Invoice Management System
-- This script creates 20 tenants, 36 rooms (12 per floor, 3 floors), and sample invoices/maintenance records

USE apartment_db;

-- Clean existing data (optional - comment out if you want to keep existing data)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE invoice;
TRUNCATE TABLE maintenance;
TRUNCATE TABLE lease;
TRUNCATE TABLE room;
TRUNCATE TABLE tenant;
SET FOREIGN_KEY_CHECKS = 1;

-- Insert 20 Tenants with varying information
INSERT INTO tenant (id, name, phone, line_id) VALUES
(1, 'สมชาย ใจดี', '081-234-5678', 'somchai_j'),
(2, 'สมหญิง รักสงบ', '082-345-6789', 'somying_r'),
(3, 'ประยุทธ์ มั่นคง', '083-456-7890', 'prayut_m'),
(4, 'อรุณี สว่างไสว', '084-567-8901', 'arunee_s'),
(5, 'วิชัย ขยันงาน', '085-678-9012', 'wichai_k'),
(6, 'สุดา อ่อนหวาน', '086-789-0123', 'suda_o'),
(7, 'ธนา ร่ำรวย', '087-890-1234', 'thana_r'),
(8, 'นภา แจ่มใส', '088-901-2345', 'napa_j'),
(9, 'ชัยชนะ เข้มแข็ง', '089-012-3456', 'chaichana_k'),
(10, 'มาลี หอมหวาน', '081-123-4567', 'malee_h'),
(11, 'บุญมี มีสุข', '082-234-5678', 'boonmee_m'),
(12, 'จันทร์เพ็ญ สดใส', '083-345-6789', 'chanpen_s'),
(13, 'ประเสริฐ ดีเลิศ', '084-456-7890', 'prasert_d'),
(14, 'วรรณา สุภาพ', '085-567-8901', 'wanna_s'),
(15, 'สมบัติ รุ่งเรือง', '086-678-9012', 'sombat_r'),
(16, 'ปิยะ น่ารัก', '087-789-0123', 'piya_n'),
(17, 'ธีรพล กล้าหาญ', '088-890-1234', 'teeraphon_k'),
(18, 'รัชนี เรียบร้อย', '089-901-2345', 'rachanee_r'),
(19, 'สุรชัย แข็งแกร่ง', '081-234-9876', 'surachai_k'),
(20, 'ชนิดา อ่อนโยน', '082-345-8765', 'chanida_o');

-- Insert 36 Rooms (12 rooms per floor: 201-212, 301-312, 401-412)
-- Floor 2 (201-212)
INSERT INTO room (id, number, status, tenant_id, common_fee_baht, garbage_fee_baht) VALUES
(1, 201, 'OCCUPIED', 1, 300.00, 50.00),
(2, 202, 'OCCUPIED', 2, 300.00, 50.00),
(3, 203, 'OCCUPIED', 3, 300.00, 50.00),
(4, 204, 'OCCUPIED', 4, 300.00, 50.00),
(5, 205, 'OCCUPIED', 5, 300.00, 50.00),
(6, 206, 'OCCUPIED', 6, 300.00, 50.00),
(7, 207, 'OCCUPIED', 7, 300.00, 50.00),
(8, 208, 'OCCUPIED', 8, 300.00, 50.00),
(9, 209, 'FREE', NULL, 300.00, 50.00),
(10, 210, 'FREE', NULL, 300.00, 50.00),
(11, 211, 'FREE', NULL, 300.00, 50.00),
(12, 212, 'FREE', NULL, 300.00, 50.00);

-- Floor 3 (301-312)
INSERT INTO room (id, number, status, tenant_id, common_fee_baht, garbage_fee_baht) VALUES
(13, 301, 'OCCUPIED', 9, 350.00, 50.00),
(14, 302, 'OCCUPIED', 10, 350.00, 50.00),
(15, 303, 'OCCUPIED', 11, 350.00, 50.00),
(16, 304, 'OCCUPIED', 12, 350.00, 50.00),
(17, 305, 'OCCUPIED', 13, 350.00, 50.00),
(18, 306, 'OCCUPIED', 14, 350.00, 50.00),
(19, 307, 'OCCUPIED', 15, 350.00, 50.00),
(20, 308, 'OCCUPIED', 16, 350.00, 50.00),
(21, 309, 'OCCUPIED', 17, 350.00, 50.00),
(22, 310, 'FREE', NULL, 350.00, 50.00),
(23, 311, 'FREE', NULL, 350.00, 50.00),
(24, 312, 'FREE', NULL, 350.00, 50.00);

-- Floor 4 (401-412)
INSERT INTO room (id, number, status, tenant_id, common_fee_baht, garbage_fee_baht) VALUES
(25, 401, 'OCCUPIED', 18, 400.00, 50.00),
(26, 402, 'OCCUPIED', 19, 400.00, 50.00),
(27, 403, 'OCCUPIED', 20, 400.00, 50.00),
(28, 404, 'FREE', NULL, 400.00, 50.00),
(29, 405, 'FREE', NULL, 400.00, 50.00),
(30, 406, 'FREE', NULL, 400.00, 50.00),
(31, 407, 'FREE', NULL, 400.00, 50.00),
(32, 408, 'FREE', NULL, 400.00, 50.00),
(33, 409, 'FREE', NULL, 400.00, 50.00),
(34, 410, 'FREE', NULL, 400.00, 50.00),
(35, 411, 'FREE', NULL, 400.00, 50.00),
(36, 412, 'FREE', NULL, 400.00, 50.00);

-- Insert Leases for occupied rooms
INSERT INTO lease (id, room_id, tenant_id, start_date, end_date, monthly_rent, deposit_baht, status, settled, custom_name, custom_id_card, custom_address) VALUES
-- Floor 2
(1, 1, 1, '2024-01-01', NULL, 4500.00, 9000.00, 'ACTIVE', FALSE, 'สมชาย ใจดี', '1-1234-56789-01-2', '123 ถ.สุขุมวิท กรุงเทพฯ'),
(2, 2, 2, '2024-01-15', NULL, 4500.00, 9000.00, 'ACTIVE', FALSE, 'สมหญิง รักสงบ', '1-2345-67890-12-3', '456 ถ.พระราม 4 กรุงเทพฯ'),
(3, 3, 3, '2024-02-01', NULL, 4500.00, 9000.00, 'ACTIVE', FALSE, 'ประยุทธ์ มั่นคง', '1-3456-78901-23-4', '789 ถ.รัชดา กรุงเทพฯ'),
(4, 4, 4, '2024-02-15', NULL, 4500.00, 9000.00, 'ACTIVE', FALSE, 'อรุณี สว่างไสว', '1-4567-89012-34-5', '321 ถ.เพชรบุรี กรุงเทพฯ'),
(5, 5, 5, '2024-03-01', NULL, 4500.00, 9000.00, 'ACTIVE', FALSE, 'วิชัย ขยันงาน', '1-5678-90123-45-6', '654 ถ.สาทร กรุงเทพฯ'),
(6, 6, 6, '2024-03-15', NULL, 4500.00, 9000.00, 'ACTIVE', FALSE, 'สุดา อ่อนหวาน', '1-6789-01234-56-7', '987 ถ.วิภาวดี กรุงเทพฯ'),
(7, 7, 7, '2024-04-01', NULL, 4500.00, 9000.00, 'ACTIVE', FALSE, 'ธนา ร่ำรวย', '1-7890-12345-67-8', '147 ถ.ลาดพร้าว กรุงเทพฯ'),
(8, 8, 8, '2024-04-15', NULL, 4500.00, 9000.00, 'ACTIVE', FALSE, 'นภา แจ่มใส', '1-8901-23456-78-9', '258 ถ.รามคำแหง กรุงเทพฯ'),
-- Floor 3
(9, 13, 9, '2024-01-01', NULL, 5000.00, 10000.00, 'ACTIVE', FALSE, 'ชัยชนะ เข้มแข็ง', '1-9012-34567-89-0', '369 ถ.บางนา กรุงเทพฯ'),
(10, 14, 10, '2024-01-15', NULL, 5000.00, 10000.00, 'ACTIVE', FALSE, 'มาลี หอมหวาน', '1-0123-45678-90-1', '741 ถ.ศรีนครินทร์ กรุงเทพฯ'),
(11, 15, 11, '2024-02-01', NULL, 5000.00, 10000.00, 'ACTIVE', FALSE, 'บุญมี มีสุข', '1-1234-56789-02-3', '852 ถ.พัฒนาการ กรุงเทพฯ'),
(12, 16, 12, '2024-02-15', NULL, 5000.00, 10000.00, 'ACTIVE', FALSE, 'จันทร์เพ็ญ สดใส', '1-2345-67890-13-4', '963 ถ.ประชาอุทิศ กรุงเทพฯ'),
(13, 17, 13, '2024-03-01', NULL, 5000.00, 10000.00, 'ACTIVE', FALSE, 'ประเสริฐ ดีเลิศ', '1-3456-78901-24-5', '159 ถ.งามวงศ์วาน กรุงเทพฯ'),
(14, 18, 14, '2024-03-15', NULL, 5000.00, 10000.00, 'ACTIVE', FALSE, 'วรรณา สุภาพ', '1-4567-89012-35-6', '357 ถ.รัชโยธิน กรุงเทพฯ'),
(15, 19, 15, '2024-04-01', NULL, 5000.00, 10000.00, 'ACTIVE', FALSE, 'สมบัติ รุ่งเรือง', '1-5678-90123-46-7', '951 ถ.พหลโยธิน กรุงเทพฯ'),
(16, 20, 16, '2024-04-15', NULL, 5000.00, 10000.00, 'ACTIVE', FALSE, 'ปิยะ น่ารัก', '1-6789-01234-57-8', '753 ถ.แจ้งวัฒนะ กรุงเทพฯ'),
(17, 21, 17, '2024-05-01', NULL, 5000.00, 10000.00, 'ACTIVE', FALSE, 'ธีรพล กล้าหาญ', '1-7890-12345-68-9', '159 ถ.บรมราชชนนี กรุงเทพฯ'),
-- Floor 4
(18, 25, 18, '2024-01-01', NULL, 5500.00, 11000.00, 'ACTIVE', FALSE, 'รัชนี เรียบร้อย', '1-8901-23456-79-0', '357 ถ.ติวานนท์ นนทบุรี'),
(19, 26, 19, '2024-02-01', NULL, 5500.00, 11000.00, 'ACTIVE', FALSE, 'สุรชัย แข็งแกร่ง', '1-9012-34567-80-1', '951 ถ.รัตนาธิเบศร์ นนทบุรี'),
(20, 27, 20, '2024-03-01', NULL, 5500.00, 11000.00, 'ACTIVE', FALSE, 'ชนิดา อ่อนโยน', '1-0123-45678-91-2', '753 ถ.แคราย นนทบุรี');

-- Insert 5 Invoices per occupied room (20 occupied rooms x 5 invoices = 100 invoices)
-- We'll create invoices for the past 5 months (December 2024 - April 2025)

-- Room 201 (Tenant 1)
INSERT INTO invoice (room_id, tenant_id, billing_year, billing_month, issue_date, due_date, paid_date,
    rent_baht, electricity_units, electricity_rate, electricity_baht, water_units, water_rate, water_baht,
    common_fee_baht, garbage_fee_baht, maintenance_baht, other_baht, total_baht, status)
VALUES
(1, 1, 2024, 12, '2024-12-01', '2024-12-07', '2024-12-05', 4500.00, 125.5, 4.50, 564.75, 15.2, 18.00, 273.60, 300.00, 50.00, 0.00, 0.00, 5688.35, 'PAID'),
(1, 1, 2025, 1, '2025-01-01', '2025-01-07', '2025-01-06', 4500.00, 138.2, 4.50, 621.90, 16.5, 18.00, 297.00, 300.00, 50.00, 0.00, 0.00, 5768.90, 'PAID'),
(1, 1, 2025, 2, '2025-02-01', '2025-02-07', '2025-02-05', 4500.00, 142.8, 4.50, 642.60, 17.8, 18.00, 320.40, 300.00, 50.00, 0.00, 0.00, 5813.00, 'PAID'),
(1, 1, 2025, 3, '2025-03-01', '2025-03-07', NULL, 4500.00, 155.3, 4.50, 698.85, 18.3, 18.00, 329.40, 300.00, 50.00, 0.00, 0.00, 5878.25, 'PENDING'),
(1, 1, 2025, 4, '2025-04-01', '2025-04-07', NULL, 4500.00, 148.7, 4.50, 669.15, 19.1, 18.00, 343.80, 300.00, 50.00, 0.00, 0.00, 5862.95, 'PENDING');

-- Room 202 (Tenant 2)
INSERT INTO invoice (room_id, tenant_id, billing_year, billing_month, issue_date, due_date, paid_date,
    rent_baht, electricity_units, electricity_rate, electricity_baht, water_units, water_rate, water_baht,
    common_fee_baht, garbage_fee_baht, maintenance_baht, other_baht, total_baht, status)
VALUES
(2, 2, 2024, 12, '2024-12-01', '2024-12-07', '2024-12-06', 4500.00, 112.3, 4.50, 505.35, 14.8, 18.00, 266.40, 300.00, 50.00, 0.00, 0.00, 5621.75, 'PAID'),
(2, 2, 2025, 1, '2025-01-01', '2025-01-07', '2025-01-04', 4500.00, 128.9, 4.50, 580.05, 15.3, 18.00, 275.40, 300.00, 50.00, 0.00, 0.00, 5705.45, 'PAID'),
(2, 2, 2025, 2, '2025-02-01', '2025-02-07', '2025-02-08', 4500.00, 135.4, 4.50, 609.30, 16.2, 18.00, 291.60, 300.00, 50.00, 0.00, 0.00, 5750.90, 'PAID'),
(2, 2, 2025, 3, '2025-03-01', '2025-03-07', NULL, 4500.00, 141.2, 4.50, 635.40, 17.5, 18.00, 315.00, 300.00, 50.00, 0.00, 0.00, 5800.40, 'PENDING'),
(2, 2, 2025, 4, '2025-04-01', '2025-04-07', NULL, 4500.00, 133.8, 4.50, 602.10, 16.9, 18.00, 304.20, 300.00, 50.00, 0.00, 0.00, 5756.30, 'PENDING');

-- Continue pattern for remaining occupied rooms (I'll create a subset to keep it manageable)
-- Room 203 (Tenant 3)
INSERT INTO invoice (room_id, tenant_id, billing_year, billing_month, issue_date, due_date, paid_date,
    rent_baht, electricity_units, electricity_rate, electricity_baht, water_units, water_rate, water_baht,
    common_fee_baht, garbage_fee_baht, maintenance_baht, other_baht, total_baht, status)
VALUES
(3, 3, 2024, 12, '2024-12-01', '2024-12-07', '2024-12-04', 4500.00, 145.2, 4.50, 653.40, 18.5, 18.00, 333.00, 300.00, 50.00, 0.00, 0.00, 5836.40, 'PAID'),
(3, 3, 2025, 1, '2025-01-01', '2025-01-07', '2025-01-05', 4500.00, 152.8, 4.50, 687.60, 19.2, 18.00, 345.60, 300.00, 50.00, 0.00, 0.00, 5883.20, 'PAID'),
(3, 3, 2025, 2, '2025-02-01', '2025-02-07', '2025-02-06', 4500.00, 158.3, 4.50, 712.35, 20.1, 18.00, 361.80, 300.00, 50.00, 200.00, 0.00, 6124.15, 'PAID'),
(3, 3, 2025, 3, '2025-03-01', '2025-03-07', NULL, 4500.00, 162.5, 4.50, 731.25, 21.3, 18.00, 383.40, 300.00, 50.00, 0.00, 0.00, 5964.65, 'PENDING'),
(3, 3, 2025, 4, '2025-04-01', '2025-04-07', NULL, 4500.00, 149.8, 4.50, 674.10, 19.8, 18.00, 356.40, 300.00, 50.00, 0.00, 0.00, 5880.50, 'PENDING');

-- For brevity, I'll add a few more rooms then use a DELIMITER to create remaining invoices programmatically
-- Room 204 (Tenant 4) - 5 invoices
INSERT INTO invoice (room_id, tenant_id, billing_year, billing_month, issue_date, due_date, paid_date,
    rent_baht, electricity_units, electricity_rate, electricity_baht, water_units, water_rate, water_baht,
    common_fee_baht, garbage_fee_baht, maintenance_baht, other_baht, total_baht, status)
VALUES
(4, 4, 2024, 12, '2024-12-01', '2024-12-07', '2024-12-07', 4500.00, 118.5, 4.50, 533.25, 13.7, 18.00, 246.60, 300.00, 50.00, 0.00, 0.00, 5629.85, 'PAID'),
(4, 4, 2025, 1, '2025-01-01', '2025-01-07', '2025-01-06', 4500.00, 125.3, 4.50, 563.85, 14.9, 18.00, 268.20, 300.00, 50.00, 0.00, 0.00, 5682.05, 'PAID'),
(4, 4, 2025, 2, '2025-02-01', '2025-02-07', '2025-02-07', 4500.00, 132.1, 4.50, 594.45, 15.8, 18.00, 284.40, 300.00, 50.00, 0.00, 0.00, 5728.85, 'PAID'),
(4, 4, 2025, 3, '2025-03-01', '2025-03-07', '2025-03-05', 4500.00, 138.9, 4.50, 625.05, 16.4, 18.00, 295.20, 300.00, 50.00, 0.00, 0.00, 5770.25, 'PAID'),
(4, 4, 2025, 4, '2025-04-01', '2025-04-07', NULL, 4500.00, 142.7, 4.50, 642.15, 17.2, 18.00, 309.60, 300.00, 50.00, 0.00, 0.00, 5801.75, 'PENDING');

-- Room 205 (Tenant 5) - 5 invoices
INSERT INTO invoice (room_id, tenant_id, billing_year, billing_month, issue_date, due_date, paid_date,
    rent_baht, electricity_units, electricity_rate, electricity_baht, water_units, water_rate, water_baht,
    common_fee_baht, garbage_fee_baht, maintenance_baht, other_baht, total_baht, status)
VALUES
(5, 5, 2024, 12, '2024-12-01', '2024-12-07', '2024-12-03', 4500.00, 165.8, 4.50, 746.10, 22.3, 18.00, 401.40, 300.00, 50.00, 150.00, 0.00, 6147.50, 'PAID'),
(5, 5, 2025, 1, '2025-01-01', '2025-01-07', '2025-01-05', 4500.00, 172.4, 4.50, 775.80, 23.1, 18.00, 415.80, 300.00, 50.00, 0.00, 0.00, 6041.60, 'PAID'),
(5, 5, 2025, 2, '2025-02-01', '2025-02-07', '2025-02-04', 4500.00, 168.9, 4.50, 760.05, 24.5, 18.00, 441.00, 300.00, 50.00, 0.00, 0.00, 6051.05, 'PAID'),
(5, 5, 2025, 3, '2025-03-01', '2025-03-07', NULL, 4500.00, 175.3, 4.50, 788.85, 25.8, 18.00, 464.40, 300.00, 50.00, 0.00, 0.00, 6103.25, 'PENDING'),
(5, 5, 2025, 4, '2025-04-01', '2025-04-07', NULL, 4500.00, 181.2, 4.50, 815.40, 26.3, 18.00, 473.40, 300.00, 50.00, 0.00, 0.00, 6138.80, 'PENDING');

-- Insert Maintenance Requests (5 per occupied room = 100 total)
-- Room 201 - 5 maintenance requests
INSERT INTO maintenance (room_id, description, status, scheduled_date, completed_date, cost_baht) VALUES
(1, 'ซ่อมก๊อกน้ำรั่ว', 'COMPLETED', '2024-12-05', '2024-12-05', 250.00),
(1, 'เปลี่ยนหลอดไฟในห้องนอน', 'COMPLETED', '2025-01-15', '2025-01-15', 150.00),
(1, 'ทำความสะอาดแอร์', 'COMPLETED', '2025-02-10', '2025-02-10', 500.00),
(1, 'ซ่อมประตูห้องน้ำ', 'IN_PROGRESS', '2025-03-20', NULL, NULL),
(1, 'เช็คระบบไฟฟ้า', 'PLANNED', '2025-05-01', NULL, NULL);

-- Room 202 - 5 maintenance requests
INSERT INTO maintenance (room_id, description, status, scheduled_date, completed_date, cost_baht) VALUES
(2, 'ซ่อมเครื่องทำน้ำอุ่น', 'COMPLETED', '2024-12-10', '2024-12-10', 800.00),
(2, 'ทาสีผนังห้องนั่งเล่น', 'COMPLETED', '2025-01-05', '2025-01-05', 1200.00),
(2, 'เปลี่ยนมุ้งลวดหน้าต่าง', 'COMPLETED', '2025-02-15', '2025-02-15', 350.00),
(2, 'ซ่อมเตาแก๊ส', 'COMPLETED', '2025-03-10', '2025-03-10', 450.00),
(2, 'เปลี่ยนกุญแจประตู', 'IN_PROGRESS', '2025-04-05', NULL, NULL);

-- Room 203 - 5 maintenance requests
INSERT INTO maintenance (room_id, description, status, scheduled_date, completed_date, cost_baht) VALUES
(3, 'ล้างแอร์', 'COMPLETED', '2024-12-20', '2024-12-20', 600.00),
(3, 'เปลี่ยนโถส้วม', 'COMPLETED', '2025-01-25', '2025-01-25', 2500.00),
(3, 'ซ่อมท่อน้ำรั่ว', 'COMPLETED', '2025-02-18', '2025-02-18', 350.00),
(3, 'เปลี่ยนกระเบื้องห้องครัว', 'PLANNED', '2025-04-15', NULL, NULL),
(3, 'ซ่อมระบบระบายน้ำ', 'PLANNED', '2025-05-10', NULL, NULL);

-- Room 204 - 5 maintenance requests
INSERT INTO maintenance (room_id, description, status, scheduled_date, completed_date, cost_baht) VALUES
(4, 'เปลี่ยนหลอดไฟทั้งหมด', 'COMPLETED', '2025-01-10', '2025-01-10', 300.00),
(4, 'ซ่อมพัดลมเพดาน', 'COMPLETED', '2025-02-05', '2025-02-05', 400.00),
(4, 'ทำความสะอาดครัว', 'COMPLETED', '2025-03-01', '2025-03-01', 200.00),
(4, 'เปลี่ยนม่านห้องนอน', 'IN_PROGRESS', '2025-04-10', NULL, NULL),
(4, 'ซ่อมหน้าต่างกระจก', 'PLANNED', '2025-05-05', NULL, NULL);

-- Room 205 - 5 maintenance requests
INSERT INTO maintenance (room_id, description, status, scheduled_date, completed_date, cost_baht) VALUES
(5, 'เปลี่ยนชุดฝักบัว', 'COMPLETED', '2024-12-15', '2024-12-15', 450.00),
(5, 'ซ่อมเครื่องดูดควัน', 'COMPLETED', '2025-01-20', '2025-01-20', 550.00),
(5, 'ล้างถังเก็บน้ำ', 'COMPLETED', '2025-02-25', '2025-02-25', 300.00),
(5, 'เปลี่ยนสายน้ำ', 'COMPLETED', '2025-03-15', '2025-03-15', 250.00),
(5, 'เช็คระบบไฟฟ้า', 'PLANNED', '2025-04-20', NULL, NULL);

-- Add more invoices and maintenance for remaining occupied rooms (6-8, 13-21, 25-27)
-- I'll add simplified versions for the remaining rooms

-- Rooms 206-208 Invoices
INSERT INTO invoice (room_id, tenant_id, billing_year, billing_month, issue_date, due_date, rent_baht, electricity_units, electricity_rate, electricity_baht, water_units, water_rate, water_baht, common_fee_baht, garbage_fee_baht, total_baht, status)
SELECT room_id, tenant_id, year, month,
    CONCAT(year, '-', LPAD(month, 2, '0'), '-01'),
    CONCAT(year, '-', LPAD(month, 2, '0'), '-07'),
    4500.00,
    120 + (RAND() * 60),
    4.50,
    (120 + (RAND() * 60)) * 4.50,
    15 + (RAND() * 10),
    18.00,
    (15 + (RAND() * 10)) * 18.00,
    300.00,
    50.00,
    4500.00 + ((120 + (RAND() * 60)) * 4.50) + ((15 + (RAND() * 10)) * 18.00) + 350.00,
    IF(month < 4, 'PAID', 'PENDING')
FROM (
    SELECT 6 as room_id, 6 as tenant_id, 2024 as year, 12 as month
    UNION ALL SELECT 6, 6, 2025, 1
    UNION ALL SELECT 6, 6, 2025, 2
    UNION ALL SELECT 6, 6, 2025, 3
    UNION ALL SELECT 6, 6, 2025, 4
    UNION ALL SELECT 7, 7, 2024, 12
    UNION ALL SELECT 7, 7, 2025, 1
    UNION ALL SELECT 7, 7, 2025, 2
    UNION ALL SELECT 7, 7, 2025, 3
    UNION ALL SELECT 7, 7, 2025, 4
    UNION ALL SELECT 8, 8, 2024, 12
    UNION ALL SELECT 8, 8, 2025, 1
    UNION ALL SELECT 8, 8, 2025, 2
    UNION ALL SELECT 8, 8, 2025, 3
    UNION ALL SELECT 8, 8, 2025, 4
) AS invoice_data;

-- Floor 3 rooms (13-21) invoices
INSERT INTO invoice (room_id, tenant_id, billing_year, billing_month, issue_date, due_date, rent_baht, electricity_units, electricity_rate, electricity_baht, water_units, water_rate, water_baht, common_fee_baht, garbage_fee_baht, total_baht, status)
SELECT room_id, tenant_id, year, month,
    CONCAT(year, '-', LPAD(month, 2, '0'), '-01'),
    CONCAT(year, '-', LPAD(month, 2, '0'), '-07'),
    5000.00,
    130 + (RAND() * 70),
    4.50,
    (130 + (RAND() * 70)) * 4.50,
    16 + (RAND() * 12),
    18.00,
    (16 + (RAND() * 12)) * 18.00,
    350.00,
    50.00,
    5000.00 + ((130 + (RAND() * 70)) * 4.50) + ((16 + (RAND() * 12)) * 18.00) + 400.00,
    IF(month < 4, 'PAID', 'PENDING')
FROM (
    SELECT 13 as room_id, 9 as tenant_id, 2024 as year, 12 as month
    UNION ALL SELECT 13, 9, 2025, 1
    UNION ALL SELECT 13, 9, 2025, 2
    UNION ALL SELECT 13, 9, 2025, 3
    UNION ALL SELECT 13, 9, 2025, 4
    UNION ALL SELECT 14, 10, 2024, 12
    UNION ALL SELECT 14, 10, 2025, 1
    UNION ALL SELECT 14, 10, 2025, 2
    UNION ALL SELECT 14, 10, 2025, 3
    UNION ALL SELECT 14, 10, 2025, 4
    UNION ALL SELECT 15, 11, 2024, 12
    UNION ALL SELECT 15, 11, 2025, 1
    UNION ALL SELECT 15, 11, 2025, 2
    UNION ALL SELECT 15, 11, 2025, 3
    UNION ALL SELECT 15, 11, 2025, 4
    UNION ALL SELECT 16, 12, 2024, 12
    UNION ALL SELECT 16, 12, 2025, 1
    UNION ALL SELECT 16, 12, 2025, 2
    UNION ALL SELECT 16, 12, 2025, 3
    UNION ALL SELECT 16, 12, 2025, 4
    UNION ALL SELECT 17, 13, 2024, 12
    UNION ALL SELECT 17, 13, 2025, 1
    UNION ALL SELECT 17, 13, 2025, 2
    UNION ALL SELECT 17, 13, 2025, 3
    UNION ALL SELECT 17, 13, 2025, 4
    UNION ALL SELECT 18, 14, 2024, 12
    UNION ALL SELECT 18, 14, 2025, 1
    UNION ALL SELECT 18, 14, 2025, 2
    UNION ALL SELECT 18, 14, 2025, 3
    UNION ALL SELECT 18, 14, 2025, 4
    UNION ALL SELECT 19, 15, 2024, 12
    UNION ALL SELECT 19, 15, 2025, 1
    UNION ALL SELECT 19, 15, 2025, 2
    UNION ALL SELECT 19, 15, 2025, 3
    UNION ALL SELECT 19, 15, 2025, 4
    UNION ALL SELECT 20, 16, 2024, 12
    UNION ALL SELECT 20, 16, 2025, 1
    UNION ALL SELECT 20, 16, 2025, 2
    UNION ALL SELECT 20, 16, 2025, 3
    UNION ALL SELECT 20, 16, 2025, 4
    UNION ALL SELECT 21, 17, 2024, 12
    UNION ALL SELECT 21, 17, 2025, 1
    UNION ALL SELECT 21, 17, 2025, 2
    UNION ALL SELECT 21, 17, 2025, 3
    UNION ALL SELECT 21, 17, 2025, 4
) AS invoice_data;

-- Floor 4 rooms (25-27) invoices
INSERT INTO invoice (room_id, tenant_id, billing_year, billing_month, issue_date, due_date, rent_baht, electricity_units, electricity_rate, electricity_baht, water_units, water_rate, water_baht, common_fee_baht, garbage_fee_baht, total_baht, status)
SELECT room_id, tenant_id, year, month,
    CONCAT(year, '-', LPAD(month, 2, '0'), '-01'),
    CONCAT(year, '-', LPAD(month, 2, '0'), '-07'),
    5500.00,
    140 + (RAND() * 80),
    4.50,
    (140 + (RAND() * 80)) * 4.50,
    17 + (RAND() * 14),
    18.00,
    (17 + (RAND() * 14)) * 18.00,
    400.00,
    50.00,
    5500.00 + ((140 + (RAND() * 80)) * 4.50) + ((17 + (RAND() * 14)) * 18.00) + 450.00,
    IF(month < 4, 'PAID', 'PENDING')
FROM (
    SELECT 25 as room_id, 18 as tenant_id, 2024 as year, 12 as month
    UNION ALL SELECT 25, 18, 2025, 1
    UNION ALL SELECT 25, 18, 2025, 2
    UNION ALL SELECT 25, 18, 2025, 3
    UNION ALL SELECT 25, 18, 2025, 4
    UNION ALL SELECT 26, 19, 2024, 12
    UNION ALL SELECT 26, 19, 2025, 1
    UNION ALL SELECT 26, 19, 2025, 2
    UNION ALL SELECT 26, 19, 2025, 3
    UNION ALL SELECT 26, 19, 2025, 4
    UNION ALL SELECT 27, 20, 2024, 12
    UNION ALL SELECT 27, 20, 2025, 1
    UNION ALL SELECT 27, 20, 2025, 2
    UNION ALL SELECT 27, 20, 2025, 3
    UNION ALL SELECT 27, 20, 2025, 4
) AS invoice_data;

-- Add maintenance for remaining rooms
INSERT INTO maintenance (room_id, description, status, scheduled_date, completed_date, cost_baht)
SELECT room_id, description, status, scheduled_date, completed_date, cost_baht
FROM (
    SELECT 6 as room_id, 'ซ่อมประตู' as description, 'COMPLETED' as status, '2024-12-10' as scheduled_date, '2024-12-10' as completed_date, 300.00 as cost_baht
    UNION ALL SELECT 6, 'เปลี่ยนหลอดไฟ', 'COMPLETED', '2025-01-15', '2025-01-15', 150.00
    UNION ALL SELECT 6, 'ทำความสะอาดแอร์', 'COMPLETED', '2025-02-20', '2025-02-20', 500.00
    UNION ALL SELECT 6, 'ซ่อมหน้าต่าง', 'IN_PROGRESS', '2025-03-25', NULL, NULL
    UNION ALL SELECT 6, 'เช็คระบบไฟ', 'PLANNED', '2025-05-01', NULL, NULL
    UNION ALL SELECT 7, 'ซ่อมก๊อกน้ำ', 'COMPLETED', '2024-12-12', '2024-12-12', 250.00
    UNION ALL SELECT 7, 'ทาสีห้อง', 'COMPLETED', '2025-01-18', '2025-01-18', 1500.00
    UNION ALL SELECT 7, 'เปลี่ยนชักโครก', 'COMPLETED', '2025-02-22', '2025-02-22', 2000.00
    UNION ALL SELECT 7, 'ซ่อมเตา', 'IN_PROGRESS', '2025-03-28', NULL, NULL
    UNION ALL SELECT 7, 'ล้างท่อ', 'PLANNED', '2025-04-30', NULL, NULL
    UNION ALL SELECT 8, 'เปลี่ยนกุญแจ', 'COMPLETED', '2024-12-14', '2024-12-14', 400.00
    UNION ALL SELECT 8, 'ซ่อมแอร์', 'COMPLETED', '2025-01-20', '2025-01-20', 800.00
    UNION ALL SELECT 8, 'ทำความสะอาด', 'COMPLETED', '2025-02-24', '2025-02-24', 200.00
    UNION ALL SELECT 8, 'เปลี่ยนม่าน', 'IN_PROGRESS', '2025-03-30', NULL, NULL
    UNION ALL SELECT 8, 'เช็คระบบน้ำ', 'PLANNED', '2025-05-02', NULL, NULL
) AS maint_data;

-- Add more maintenance for floor 3 and 4 rooms (simplified batch insert)
INSERT INTO maintenance (room_id, description, status, scheduled_date, cost_baht)
SELECT room_id,
    CONCAT('งานซ่อมบำรุงห้อง ', room_id, ' ครั้งที่ ', seq) as description,
    CASE WHEN seq <= 3 THEN 'COMPLETED' WHEN seq = 4 THEN 'IN_PROGRESS' ELSE 'PLANNED' END as status,
    DATE_ADD('2024-12-01', INTERVAL (seq * 20) DAY) as scheduled_date,
    CASE WHEN seq <= 3 THEN 300 + (seq * 100) ELSE NULL END as cost_baht
FROM (
    SELECT 13 as room_id UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16
    UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
    UNION ALL SELECT 21 UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27
) AS rooms
CROSS JOIN (
    SELECT 1 as seq UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) AS sequences;

-- Summary statistics
SELECT 'Data Initialization Complete!' as Status;
SELECT COUNT(*) as TotalTenants FROM tenant;
SELECT COUNT(*) as TotalRooms FROM room;
SELECT COUNT(*) as OccupiedRooms FROM room WHERE status = 'OCCUPIED';
SELECT COUNT(*) as FreeRooms FROM room WHERE status = 'FREE';
SELECT COUNT(*) as TotalLeases FROM lease;
SELECT COUNT(*) as TotalInvoices FROM invoice;
SELECT COUNT(*) as TotalMaintenance FROM maintenance;
