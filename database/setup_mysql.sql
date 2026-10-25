-- LOST AND FOUND: MySQL 8.4 or newer (NOT Oracle/APEX SQL).
-- Run this WHOLE file ONCE in MySQL Workbench, connected as root.
-- File > Open SQL Script, then Query > Execute (All or Selection).
-- Keep 'Stop Script Execution On Errors' enabled. No text should be selected.
-- Creates a NEW database. Does not drop or reset any existing database.
-- 5 tables, 2 views, 2 procedures and 6 small triggers.
-- MySQL needs a separate trigger for each INSERT, UPDATE or DELETE event.

CREATE DATABASE lost_and_found CHARACTER SET utf8mb4;
USE lost_and_found;

CREATE TABLE lf_student (
    student_id INT PRIMARY KEY,
    student_name VARCHAR(60) NOT NULL,
    contact VARCHAR(80) NOT NULL
) ENGINE = InnoDB;

CREATE TABLE lf_category (
    category_id INT PRIMARY KEY,
    category_name VARCHAR(40) NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE lf_location (
    location_id INT PRIMARY KEY,
    location_name VARCHAR(60) NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE lf_item_report (
    report_id INT PRIMARY KEY,
    student_id INT NOT NULL,
    category_id INT NOT NULL,
    location_id INT NOT NULL,
    item_name VARCHAR(80) NOT NULL,
    description VARCHAR(250) NOT NULL,
    event_date DATE NOT NULL,
    report_type VARCHAR(5) NOT NULL CHECK (report_type IN ('LOST', 'FOUND')),
    status VARCHAR(8) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
    FOREIGN KEY (student_id) REFERENCES lf_student(student_id),
    FOREIGN KEY (category_id) REFERENCES lf_category(category_id),
    FOREIGN KEY (location_id) REFERENCES lf_location(location_id)
) ENGINE = InnoDB;

CREATE TABLE lf_item_return (
    return_id INT PRIMARY KEY,
    found_report_id INT NOT NULL UNIQUE,
    collected_by_student_id INT NOT NULL,
    return_date DATE NOT NULL,
    FOREIGN KEY (found_report_id) REFERENCES lf_item_report(report_id),
    FOREIGN KEY (collected_by_student_id) REFERENCES lf_student(student_id)
) ENGINE = InnoDB;

-- DELIMITER lets a whole BEGIN...END block contain normal semicolons.
DELIMITER $$

CREATE TRIGGER lf_return_insert BEFORE INSERT ON lf_item_return
FOR EACH ROW
BEGIN
    DECLARE v_type VARCHAR(5);
    DECLARE v_status VARCHAR(8);
    DECLARE v_date DATE;
    DECLARE v_count INT;

    SELECT COUNT(*) INTO v_count FROM lf_item_report
    WHERE report_id = NEW.found_report_id;
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The found report does not exist.';
    END IF;
    SELECT report_type, status, event_date INTO v_type, v_status, v_date
    FROM lf_item_report WHERE report_id = NEW.found_report_id;
    IF v_type <> 'FOUND' OR v_status <> 'OPEN' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only an OPEN FOUND report can be returned.';
    END IF;
    IF NEW.return_date IS NULL OR NEW.return_date < v_date OR NEW.return_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Return date must be between the finding date and today.';
    END IF;
END$$

CREATE TRIGGER lf_return_update BEFORE UPDATE ON lf_item_return
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Completed return records cannot be edited.';
END$$

CREATE TRIGGER lf_return_delete BEFORE DELETE ON lf_item_return
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Completed return records cannot be deleted.';
END$$

CREATE TRIGGER lf_report_insert BEFORE INSERT ON lf_item_report
FOR EACH ROW
BEGIN
    IF NEW.event_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The lost/found date cannot be in the future.';
    END IF;
    IF NEW.status <> 'OPEN' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A new report must start with status OPEN.';
    END IF;
END$$

CREATE TRIGGER lf_report_update BEFORE UPDATE ON lf_item_report
FOR EACH ROW
BEGIN
    DECLARE v_returns INT;
    IF NEW.event_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The lost/found date cannot be in the future.';
    END IF;
    SELECT COUNT(*) INTO v_returns FROM lf_item_return WHERE found_report_id = OLD.report_id;
    IF v_returns > 0 THEN
        IF OLD.status = 'RESOLVED' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'This returned report is read-only.';
        END IF;
        IF NEW.report_type <> 'FOUND' OR NEW.status <> 'RESOLVED' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A returned report must be FOUND and RESOLVED.';
        END IF;
    END IF;
    IF NEW.report_type = 'FOUND' AND NEW.status = 'RESOLVED' AND v_returns = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Use LF_RECORD_RETURN to resolve a found report.';
    END IF;
END$$

CREATE TRIGGER lf_report_delete BEFORE DELETE ON lf_item_report
FOR EACH ROW
BEGIN
    DECLARE v_returns INT;
    SELECT COUNT(*) INTO v_returns FROM lf_item_return WHERE found_report_id = OLD.report_id;
    IF v_returns > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A report with a return cannot be deleted.';
    END IF;
END$$

-- ALWAYS call this for a handover. Do not INSERT a return on its own.
-- This standalone procedure owns its transaction: BOTH changes succeed or
-- BOTH are rolled back. Call it after committing any unrelated pending work.
CREATE PROCEDURE lf_record_return (
    IN p_return_id INT, IN p_found_report_id INT,
    IN p_collected_by_student_id INT, IN p_return_date DATE
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    START TRANSACTION;
    INSERT INTO lf_item_return VALUES (
        p_return_id, p_found_report_id, p_collected_by_student_id, p_return_date
    );
    UPDATE lf_item_report SET status = 'RESOLVED' WHERE report_id = p_found_report_id;
    COMMIT;
END$$

CREATE PROCEDURE lf_resolve_lost_report (IN p_report_id INT)
BEGIN
    UPDATE lf_item_report SET status = 'RESOLVED'
    WHERE report_id = p_report_id AND report_type = 'LOST' AND status = 'OPEN';
    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Choose an existing OPEN LOST report.';
    END IF;
END$$

DELIMITER ;

CREATE VIEW lf_report_details AS
SELECT r.report_id, r.item_name, r.description, r.report_type, r.status,
       s.student_name AS reported_by, s.contact,
       c.category_name, l.location_name, r.event_date
FROM lf_item_report r
JOIN lf_student s ON s.student_id = r.student_id
JOIN lf_category c ON c.category_id = r.category_id
JOIN lf_location l ON l.location_id = r.location_id;

CREATE VIEW lf_return_details AS
SELECT t.return_id, r.report_id, r.item_name,
       finder.student_name AS found_by,
       collector.student_name AS collected_by, t.return_date
FROM lf_item_return t
JOIN lf_item_report r ON r.report_id = t.found_report_id
JOIN lf_student finder ON finder.student_id = r.student_id
JOIN lf_student collector ON collector.student_id = t.collected_by_student_id;

-- Fictional sample data. Run setup once, not before every demonstration.
INSERT INTO lf_student VALUES
    (1, 'Asha', 'asha@example.com'), (2, 'Ravi', 'ravi@example.com'),
    (3, 'Neha', 'neha@example.com'), (4, 'Arjun', 'arjun@example.com');
INSERT INTO lf_category VALUES
    (1, 'Electronics'), (2, 'Books'), (3, 'Keys'), (4, 'Other');
INSERT INTO lf_location VALUES
    (1, 'Library'), (2, 'Canteen'), (3, 'Classroom'), (4, 'Other');
INSERT INTO lf_item_report VALUES
    (101, 2, 1, 1, 'Calculator', 'Black calculator with a red sticker',
     DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'LOST', 'OPEN'),
    (102, 1, 1, 1, 'Calculator', 'Black calculator with a red sticker',
     DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'FOUND', 'OPEN'),
    (103, 3, 2, 3, 'Notebook', 'Blue notebook with a white label',
     DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'LOST', 'OPEN'),
    (104, 4, 3, 2, 'Keys', 'Two keys on a green keyring',
     DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'FOUND', 'OPEN'),
    (105, 1, 4, 2, 'Water bottle', 'Steel bottle with a blue cap',
     DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'LOST', 'OPEN'),
    (106, 3, 2, 1, 'Textbook', 'Database textbook with a plain cover',
     DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'FOUND', 'OPEN');
CALL lf_record_return(501, 102, 2, CURDATE());
CALL lf_resolve_lost_report(101);
COMMIT;

-- Expect 4 students, 4 categories, 4 locations, 6 reports, 1 return.
SELECT 'Students' AS record_type, COUNT(*) AS total FROM lf_student
UNION ALL SELECT 'Categories', COUNT(*) FROM lf_category
UNION ALL SELECT 'Locations', COUNT(*) FROM lf_location
UNION ALL SELECT 'Reports', COUNT(*) FROM lf_item_report
UNION ALL SELECT 'Returns', COUNT(*) FROM lf_item_return;
SELECT * FROM lf_report_details ORDER BY report_id;
SELECT * FROM lf_return_details ORDER BY return_id;
SELECT report_type, status, COUNT(*) AS total
FROM lf_item_report GROUP BY report_type, status ORDER BY report_type, status;

-- Expect 5 BASE TABLEs, 2 VIEWs, 2 PROCEDUREs and 6 TRIGGERs.
SELECT table_type AS object_type, table_name AS object_name
FROM information_schema.tables WHERE table_schema = DATABASE()
UNION ALL
SELECT routine_type, routine_name FROM information_schema.routines
WHERE routine_schema = DATABASE()
UNION ALL
SELECT 'TRIGGER', trigger_name FROM information_schema.triggers
WHERE trigger_schema = DATABASE()
ORDER BY object_type, object_name;

-- Java user: create it separately in Workbench after choosing YOUR password.
-- Replace the placeholder; do not use the example text as your real password.
-- CREATE USER 'lf_user'@'localhost' IDENTIFIED BY 'REPLACE_WITH_YOUR_PASSWORD';
-- GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON lost_and_found.* TO 'lf_user'@'localhost';
-- The Java user has no permission to create/drop tables or manage accounts.
