-- SalonHub E2E clean-up for the LOCAL database (salon_dbv2) only.
-- The API can only cancel bookings, disable services and close hiring posts,
-- so test rows stay in the database. This script deletes the finished ones.
-- It touches only rows that the tests made: names, titles and notes that start
-- with "E2E" in the test salon (salon 2). Safe to run at any time.

SET @salon := 2;

-- 1. Finished test bookings (cancelled, rejected or completed), their reviews and
--    the rows that point to them.
CREATE TEMPORARY TABLE e2e_bookings AS
SELECT id, payment_id FROM salon_booking
WHERE salon_id = @salon AND note LIKE 'E2E%' AND status IN ('CANCELLED', 'REJECTED', 'COMPLETED');

DELETE FROM salon_review WHERE booking_id IN (SELECT id FROM e2e_bookings);
DELETE FROM user_notification WHERE booking_id IN (SELECT id FROM e2e_bookings);
DELETE FROM booking_reschedule_proposal WHERE booking_id IN (SELECT id FROM e2e_bookings);
DELETE FROM booking_service_item WHERE booking_id IN (SELECT id FROM e2e_bookings);
DELETE FROM salon_booking WHERE id IN (SELECT id FROM e2e_bookings);
DELETE FROM payment_transaction WHERE payment_id IN (SELECT payment_id FROM e2e_bookings WHERE payment_id IS NOT NULL);
DELETE FROM booking_payment WHERE id IN (SELECT payment_id FROM e2e_bookings WHERE payment_id IS NOT NULL);
DROP TEMPORARY TABLE e2e_bookings;

-- 2. Disabled test services that no booking uses any more.
DELETE FROM employee_service_capability
WHERE service_id IN (SELECT id FROM (
    SELECT s.id FROM salon_service s
    WHERE s.salon_id = @salon AND s.name LIKE 'E2E %' AND s.active = b'0'
      AND NOT EXISTS (SELECT 1 FROM booking_service_item i WHERE i.source_service_id = s.id)) x);
DELETE s FROM salon_service s
WHERE s.salon_id = @salon AND s.name LIKE 'E2E %' AND s.active = b'0'
  AND NOT EXISTS (SELECT 1 FROM booking_service_item i WHERE i.source_service_id = s.id);

-- 3. Closed test hiring posts.
CREATE TEMPORARY TABLE e2e_posts AS
SELECT id FROM hiring_post WHERE title LIKE 'E2E %' AND status <> 'Open';
DELETE FROM hiring_post_required_skills WHERE hiring_post_id IN (SELECT id FROM e2e_posts);
DELETE FROM hiring_application WHERE hiring_post_id IN (SELECT id FROM e2e_posts);
DELETE FROM employment_request WHERE hiring_post_id IN (SELECT id FROM e2e_posts);
DELETE FROM hiring_post WHERE id IN (SELECT id FROM e2e_posts);
DROP TEMPORARY TABLE e2e_posts;
