-- SalonHub E2E seed data for the LOCAL database (salon_dbv2) only.
-- Safe to run more than one time.
--
-- Adds "Rami Test" (id 9014), an employee of the test salon 2 ("Omar Test Salon"),
-- who can do every service of salon 2. Without an employee, salon 2 has no free
-- booking slots (NO_QUALIFIED_EMPLOYEE) and the booking tests cannot run.
-- The password is in src/test/resources/test-accounts.properties (git-ignored);
-- this file holds only its BCrypt hash.
--
-- credentials_updated_at uses UTC_TIMESTAMP() minus one day: the DB clock is local
-- time, and a value in the future makes every token of this user 401.

INSERT INTO user (id, email, first_name, last_name, password, phone_number, role, user_name,
                  status, salon_id, career_profile_enabled, email_verified, salon_manager,
                  salon_joined_at, credentials_updated_at, total_login_count)
VALUES (9014, 'rami.employee@salonhub.test', 'Rami', 'Test',
        '$2a$10$KDIqC5tvqwH4Ncgkg2t97.FYx/wv1DlJakERg7Sdu6pY8KVeVC8h.',
        '0790000097', 3, 'rami.test', 0, 2, b'0', b'1', b'0',
        UTC_TIMESTAMP() - INTERVAL 1 DAY, UTC_TIMESTAMP() - INTERVAL 1 DAY, 0)
ON DUPLICATE KEY UPDATE salon_id = 2, role = 3, status = 0, email_verified = b'1',
                        password = VALUES(password);

DELETE FROM employee_service_capability WHERE employee_id = 9014;
INSERT INTO employee_service_capability (assigned_at, employee_id, service_id)
SELECT UTC_TIMESTAMP(), 9014, id FROM salon_service WHERE salon_id = 2;

UPDATE salon SET current_employees_number =
    (SELECT COUNT(*) FROM user WHERE salon_id = 2 AND role = 3)
WHERE id = 2;
