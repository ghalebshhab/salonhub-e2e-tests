-- SalonHub E2E seed for an EMPTY database (CI). The backend must start once
-- first, so Flyway makes the schema. All people and salons here are made up.
--
-- The caller sets the BCrypt hash of each test password before this file:
--   SET @hash_customer = '...', @hash_employee = '...', @hash_owner = '...',
--       @hash_salon2employee = '...';
-- (.github/workflows/e2e.yml makes them from the GitHub secrets.)
--
-- Same ids as the local test data, so config.properties works unchanged:
--   salon 2 "Omar Test Salon"  owner Omar (9013), employee Rami (9014)  - tests change it
--   salon 1 "E2E Other Salon"  owner 14 (no login), employee Sara (16)  - tests only read it
--   customer Lina (17)
--
-- credentials_updated_at is one day in the past: a value in the future makes
-- every token of the user 401.

SET @past := UTC_TIMESTAMP() - INTERVAL 1 DAY;

INSERT INTO user (id, email, first_name, last_name, password, phone_number, role, user_name,
                  status, salon_id, career_profile_enabled, email_verified, salon_manager,
                  salon_joined_at, credentials_updated_at, total_login_count)
VALUES
    (14,   'other.owner@salonhub.test',   'Other', 'Owner', '!no-login', '0790000090', 1, 'other.owner',
           0, NULL, b'0', b'1', b'0', NULL,  @past, 0),
    (16,   'sara.employee@salonhub.test', 'Sara',  'Test',  @hash_employee,       '0790000001', 3, 'sara.test',
           0, NULL, b'0', b'1', b'0', @past, @past, 0),
    (17,   'lina.customer@salonhub.test', 'Lina',  'Test',  @hash_customer,       '0790000002', 0, 'lina.test',
           0, NULL, b'0', b'1', b'0', NULL,  @past, 0),
    (9013, 'omar.owner@salonhub.test',    'Omar',  'Owner', @hash_owner,          '0790000099', 1, 'omar.owner',
           0, NULL, b'0', b'1', b'0', NULL,  @past, 0),
    (9014, 'rami.employee@salonhub.test', 'Rami',  'Test',  @hash_salon2employee, '0790000097', 3, 'rami.test',
           0, NULL, b'0', b'1', b'0', @past, @past, 0);

-- status 2 = approved and visible.
INSERT INTO salon (id, name, description, email, location, opening_time, closing_time, created_at,
                   current_employees_number, max_employees_number, owner_phone_number, salon_phone_number,
                   status, owner_id, version, blocked_by_admin, blocked_by_owner, resubmission_count,
                   google_maps_url, latitude, longitude)
VALUES
    (1, 'E2E Other Salon', 'A second salon for "not your salon" checks.', 'other.salon@salonhub.test',
        'Irbid', '09:00:00', '23:59:00', CURDATE() - INTERVAL 30 DAY, 1, 4, '0790000090', '0790000091',
        2, 14, 0, b'0', b'0', 0,
        'https://www.google.com/maps/search/?api=1&query=32.5556,35.8500', 32.5556, 35.8500),
    (2, 'Omar Test Salon', 'Local test salon for owner page checks.', 'owner.test.salon@salonhub.test',
        'Amman', '09:00:00', '23:59:00', CURDATE() - INTERVAL 30 DAY, 1, 4, '0790000099', '0790000098',
        2, 9013, 0, b'0', b'0', 0,
        'https://www.google.com/maps/search/?api=1&query=31.9508123,35.9241456', 31.9508123, 35.9241456);

UPDATE user SET salon_id = 1 WHERE id = 16;
UPDATE user SET salon_id = 2 WHERE id = 9014;

INSERT INTO salon_service (id, active, created_at, updated_at, description, duration_minutes, name, price, salon_id)
VALUES
    (9007, b'1', NOW(), NOW(), 'Hair cut in the best styles.', 40, 'Haircut', 5.00, 2),
    (9008, b'1', NOW(), NOW(), 'Wash, cut and blow-dry finish.', 45, 'Haircut and styling', 15.00, 2),
    (9009, b'1', NOW(), NOW(), 'Shape and line-up with hot towel.', 15, 'Beard trim', 7.00, 2),
    (9010, b'1', NOW(), NOW(), 'Zero to blend skin fade with a styled top.', 45, 'Skin fade', 8.00, 2),
    (9101, b'1', NOW(), NOW(), 'Classic cut for the other salon.', 30, 'Classic haircut', 6.00, 1);

INSERT INTO employee_service_capability (assigned_at, employee_id, service_id)
SELECT UTC_TIMESTAMP(), 9014, id FROM salon_service WHERE salon_id = 2;
INSERT INTO employee_service_capability (assigned_at, employee_id, service_id)
SELECT UTC_TIMESTAMP(), 16, id FROM salon_service WHERE salon_id = 1;
