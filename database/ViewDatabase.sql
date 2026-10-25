-- READ ONLY: these queries show the existing database. They do not change data.
-- Run a SELECT on its own to see that table's records in the result grid.

SHOW FULL TABLES FROM lost_and_found;

SELECT * FROM lost_and_found.lf_student ORDER BY student_id;
SELECT * FROM lost_and_found.lf_category ORDER BY category_id;
SELECT * FROM lost_and_found.lf_location ORDER BY location_id;
SELECT * FROM lost_and_found.lf_item_report ORDER BY report_id;
SELECT * FROM lost_and_found.lf_item_return ORDER BY return_id;

-- These two views show names together, using joins across related tables.
SELECT * FROM lost_and_found.lf_report_details ORDER BY report_id;
SELECT * FROM lost_and_found.lf_return_details ORDER BY return_id;

-- See column names, types and keys for one table.
DESCRIBE lost_and_found.lf_item_report;
