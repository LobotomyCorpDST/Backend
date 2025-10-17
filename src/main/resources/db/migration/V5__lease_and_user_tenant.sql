-- เพิ่มความสัมพันธ์ tenant ↔ user + constraint ของ lease
ALTER TABLE user_account
ADD COLUMN IF NOT EXISTS tenant_id BIGINT NULL,
ADD CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

CREATE TABLE IF NOT EXISTS lease (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    rent_amount DECIMAL(10,2) DEFAULT 0,
    deposit DECIMAL(10,2) DEFAULT 0,
    active TINYINT(1) GENERATED ALWAYS AS (CASE WHEN end_date IS NULL THEN 1 ELSE 0 END) STORED,
    CONSTRAINT fk_lease_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_lease_room FOREIGN KEY (room_id) REFERENCES room(id)
);

-- บังคับไม่ให้มี lease ซ้ำในห้องเดียวกันขณะยัง active
CREATE UNIQUE INDEX ux_lease_room_active ON lease (room_id, active) WHERE active = 1;
