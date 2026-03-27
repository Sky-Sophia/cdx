INSERT INTO users (username, display_name, password_hash, password_salt, role, status)
SELECT 'admin', '系统管理员', '4faf5873249d9a93b240f6f7b1e06b2fc6ea9caea1af17502d4e498dfd6821bc', 'pms_default_salt', 'ADMIN', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO users (username, display_name, password_hash, password_salt, role, status)
SELECT 'manager', '物业管家', '0f4b81e69839ada7b8d21a899f4b8fb241c65cdac0267d589f6af0d67dc21d48', 'pms_default_salt', 'STAFF', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'manager');

INSERT INTO users (username, display_name, password_hash, password_salt, role, status)
SELECT 'finance', '财务专员', 'e947e1917b5e0399489acac3b87416b1eeed61ebe58c15ff010c743743a3d44e', 'pms_default_salt', 'FINANCE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'finance');

INSERT INTO buildings (name, code, address, floor_count, unit_count)
SELECT '云栖花园 1 栋', 'B1', '长宁路 188 号', 18, 72
WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE code = 'B1');

INSERT INTO buildings (name, code, address, floor_count, unit_count)
SELECT '云栖花园 2 栋', 'B2', '长宁路 188 号', 22, 88
WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE code = 'B2');

INSERT INTO buildings (name, code, address, floor_count, unit_count)
SELECT '云栖花园 3 栋', 'B3', '长宁路 188 号', 16, 64
WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE code = 'B3');

INSERT INTO units (building_id, unit_no, floor_no, area_m2, occupancy_status, owner_name, owner_phone)
SELECT 1, '1-101', 1, 89.50, 'OCCUPIED', '王磊', '13800001111'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE building_id = 1 AND unit_no = '1-101');

INSERT INTO units (building_id, unit_no, floor_no, area_m2, occupancy_status, owner_name, owner_phone)
SELECT 1, '1-1202', 12, 116.20, 'RENTED', '李倩', '13800002222'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE building_id = 1 AND unit_no = '1-1202');

INSERT INTO units (building_id, unit_no, floor_no, area_m2, occupancy_status, owner_name, owner_phone)
SELECT 2, '2-803', 8, 98.00, 'OCCUPIED', '张程', '13800003333'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE building_id = 2 AND unit_no = '2-803');

INSERT INTO units (building_id, unit_no, floor_no, area_m2, occupancy_status, owner_name, owner_phone)
SELECT 3, '3-502', 5, 76.80, 'VACANT', '待分配', '13800004444'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE building_id = 3 AND unit_no = '3-502');

INSERT INTO residents (unit_id, name, phone, identity_no, resident_type, status, move_in_date)
SELECT 1, '王磊', '13800001111', '310101198803152116', 'OWNER', 'ACTIVE', DATE '2022-04-01'
WHERE NOT EXISTS (SELECT 1 FROM residents WHERE unit_id = 1 AND name = '王磊');

INSERT INTO residents (unit_id, name, phone, identity_no, resident_type, status, move_in_date)
SELECT 2, '赵菲', '13800005555', '310101199210102238', 'TENANT', 'ACTIVE', DATE '2024-09-15'
WHERE NOT EXISTS (SELECT 1 FROM residents WHERE unit_id = 2 AND name = '赵菲');

INSERT INTO work_orders (order_no, unit_id, resident_name, phone, category, priority, description, status, assignee, scheduled_at)
SELECT 'WO202603270001', 1, '王磊', '13800001111', '水电维修', 'HIGH', '厨房水槽下方渗水，影响橱柜。', 'IN_PROGRESS', '刘师傅', TIMESTAMP '2026-03-28 09:30:00'
WHERE NOT EXISTS (SELECT 1 FROM work_orders WHERE order_no = 'WO202603270001');

INSERT INTO work_orders (order_no, unit_id, resident_name, phone, category, priority, description, status, assignee)
SELECT 'WO202603270002', 2, '赵菲', '13800005555', '门禁设备', 'MEDIUM', '单元门禁卡刷卡无响应。', 'OPEN', '陈管家'
WHERE NOT EXISTS (SELECT 1 FROM work_orders WHERE order_no = 'WO202603270002');

INSERT INTO fee_bills (bill_no, unit_id, billing_month, amount, paid_amount, status, due_date, remarks)
SELECT 'BL202603270001', 1, '2026-03', 568.00, 568.00, 'PAID', DATE '2026-03-20', '物业费+公区能耗'
WHERE NOT EXISTS (SELECT 1 FROM fee_bills WHERE bill_no = 'BL202603270001');

INSERT INTO fee_bills (bill_no, unit_id, billing_month, amount, paid_amount, status, due_date, remarks)
SELECT 'BL202603270002', 2, '2026-03', 612.00, 300.00, 'PARTIAL', DATE '2026-03-20', '物业费+停车管理费'
WHERE NOT EXISTS (SELECT 1 FROM fee_bills WHERE bill_no = 'BL202603270002');

INSERT INTO fee_bills (bill_no, unit_id, billing_month, amount, paid_amount, status, due_date, remarks)
SELECT 'BL202603270003', 3, '2026-03', 489.00, 0.00, 'OVERDUE', DATE '2026-03-15', '物业费'
WHERE NOT EXISTS (SELECT 1 FROM fee_bills WHERE bill_no = 'BL202603270003');
