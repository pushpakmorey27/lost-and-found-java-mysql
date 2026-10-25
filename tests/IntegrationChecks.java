import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Properties;
import java.util.Scanner;

/* Optional checks against a disposable local MySQL 8.4+ database.
 * IMPORTANT: This deletes and recreates ONLY lost_and_found_test, never the app database.
 * Requires BOTH the command-line confirmation and explicit environment settings.
 * Run test.cmd --mysql --reset-test-database (or bash test.sh with the same arguments).
 */
public class IntegrationChecks {
    private static final String TEST_DATABASE = "lost_and_found_test";
    private static int passed;

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].equals("--reset-test-database")) {
            throw new IllegalArgumentException("Explicit --reset-test-database confirmation is required.");
        }
        String serverUrl = requiredEnvironment("LF_TEST_MYSQL_URL");
        // No database name, URL options, remote host or alternative schema can be supplied.
        if (!serverUrl.matches("jdbc:mysql://(127\\.0\\.0\\.1|localhost|\\[::1\\]):[0-9]{1,5}/")) {
            throw new IllegalArgumentException("LF_TEST_MYSQL_URL must be a local server URL, e.g. jdbc:mysql://127.0.0.1:3306/");
        }
        String username = requiredEnvironment("LF_TEST_MYSQL_USER");
        String password = requiredEnvironment("LF_TEST_MYSQL_PASSWORD");
        Class.forName("com.mysql.cj.jdbc.Driver");
        DriverManager.setLoginTimeout(10);

        // A temporary login file keeps tests away from the student's saved app login.
        Path folder = Files.createTempDirectory("lost-found-tests-");
        Path login = folder.resolve("database.properties");
        String previousSettings = System.getProperty("lostfound.settings");
        try (Connection admin = DriverManager.getConnection(
                serverUrl + "?sslMode=REQUIRED&noAccessToProcedureBodies=true", username, password)) {
            System.out.println("Resetting disposable schema: " + TEST_DATABASE);
            execute(admin, "DROP DATABASE IF EXISTS " + TEST_DATABASE);
            try {
                importSetup(admin);
                Properties settings = new Properties();
                settings.setProperty("url", serverUrl + TEST_DATABASE);
                settings.setProperty("username", username);
                settings.setProperty("password", password);
                try (OutputStream file = Files.newOutputStream(login)) {
                    settings.store(file, "Temporary integration-test login; deleted after checks.");
                }
                System.setProperty("lostfound.settings", login.toString());
                Database database = new Database(new KeyboardInput(new Scanner("")));
                try {
                    verifyDatabase(admin, database);
                } finally {
                    database.close();
                }
                System.out.println("\nPASS: " + passed + " MySQL integration checks.");
            } finally {
                // The name is a fixed constant, never user input or an app URL's database.
                execute(admin, "DROP DATABASE IF EXISTS " + TEST_DATABASE);
                System.out.println("Removed disposable schema: " + TEST_DATABASE);
            }
        } finally {
            if (previousSettings == null) {
                System.clearProperty("lostfound.settings");
            } else {
                System.setProperty("lostfound.settings", previousSettings);
            }
            Files.deleteIfExists(login);
            Files.deleteIfExists(folder);
        }
    }

    private static void verifyDatabase(Connection admin, Database database) throws SQLException {
        equal("Five base tables", "5", scalar(admin, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'"));
        equal("Two views", "2", scalar(admin, "SELECT COUNT(*) FROM information_schema.views WHERE table_schema = DATABASE()"));
        equal("Two procedures", "2", scalar(admin, "SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema = DATABASE() AND routine_type = 'PROCEDURE'"));
        equal("Six rule triggers", "6", scalar(admin, "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema = DATABASE()"));
        equal("Four fictional students", "4", scalar(admin, "SELECT COUNT(*) FROM lf_student"));
        equal("Four categories", "4", scalar(admin, "SELECT COUNT(*) FROM lf_category"));
        equal("Four locations", "4", scalar(admin, "SELECT COUNT(*) FROM lf_location"));
        equal("Six sample reports", "6", scalar(admin, "SELECT COUNT(*) FROM lf_item_report"));
        equal("One sample return", "1", scalar(admin, "SELECT COUNT(*) FROM lf_item_return"));
        equal("Report view joins all six reports", "6", scalar(admin, "SELECT COUNT(*) FROM lf_report_details"));
        equal("Return view includes collector", "Ravi", scalar(admin, "SELECT collected_by FROM lf_return_details WHERE return_id = 501"));

        // Test the production Java -> JDBC -> stored procedure -> tables path.
        database.recordReturn(901, 104, 2, LocalDate.now());
        equal("Successful return stores collector", "2", scalar(admin, "SELECT collected_by_student_id FROM lf_item_return WHERE return_id = 901"));
        equal("Successful return resolves its report", "RESOLVED", scalar(admin, "SELECT status FROM lf_item_report WHERE report_id = 104"));
        rejectsSql("Duplicate return rejected", () -> database.recordReturn(902, 104, 2, LocalDate.now()), "45000");
        equal("Duplicate created no second return", "1", scalar(admin, "SELECT COUNT(*) FROM lf_item_return WHERE found_report_id = 104"));

        // Force failure AFTER the return INSERT, during its report UPDATE.
        // The stored procedure must undo its first change, not leave half a handover.
        execute(admin, "CREATE TRIGGER lf_test_reject_resolve BEFORE UPDATE ON lf_item_report "
                + "FOR EACH ROW BEGIN IF OLD.report_id = 106 AND NEW.status = 'RESOLVED' THEN "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'TEST: injected report update failure'; END IF; END");
        try {
            rejectsSql("Injected second-step failure reaches Java", () -> database.recordReturn(903, 106, 4, LocalDate.now()), "45000");
            equal("Rollback removes the first-step return", "0", scalar(admin, "SELECT COUNT(*) FROM lf_item_return WHERE return_id = 903"));
            equal("Rollback leaves report OPEN", "OPEN", scalar(admin, "SELECT status FROM lf_item_report WHERE report_id = 106"));
        } finally {
            execute(admin, "DROP TRIGGER lf_test_reject_resolve");
        }
        database.recordReturn(903, 106, 4, LocalDate.now());
        equal("Connection can retry after rollback", "RESOLVED", scalar(admin, "SELECT status FROM lf_item_report WHERE report_id = 106"));

        database.resolveLostReport(103);
        equal("Lost report resolves after confirmation", "RESOLVED", scalar(admin, "SELECT status FROM lf_item_report WHERE report_id = 103"));
        rejectsSql("Cannot resolve the same lost report twice", () -> database.resolveLostReport(103), "45000");
        rejectsSql("Foreign key rejects a missing student", () -> database.addReport(
                new LostItem(904, 99999, 1, 1, "Test keys", "Test", LocalDate.now())), "23000");

        database.addStudent(new Student(900, "O'Connor", "test@example.com"));
        equal("Prepared statement preserves apostrophe", "O'Connor", scalar(admin, "SELECT student_name FROM lf_student WHERE student_id = 900"));
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Set " + name + " for a local disposable MySQL test server.");
        }
        return value;
    }

    // This small reader supports our checked-in setup file, including DELIMITER.
    // It is deliberately not a general SQL parser. Statements must end on a line.
    private static void importSetup(Connection connection) throws Exception {
        String sql = Files.readString(Path.of("database", "setup_mysql.sql"), StandardCharsets.UTF_8);
        String create = "CREATE DATABASE lost_and_found CHARACTER SET utf8mb4;";
        String use = "USE lost_and_found;";
        if (!sql.contains(create) || !sql.contains(use)) {
            throw new IllegalStateException("Setup declarations changed; update the test importer before running it.");
        }
        sql = sql.replace(create, "CREATE DATABASE " + TEST_DATABASE + " CHARACTER SET utf8mb4;")
                .replace(use, "USE " + TEST_DATABASE + ";");
        String delimiter = ";";
        StringBuilder statement = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            if (trimmed.startsWith("DELIMITER ")) {
                if (!statement.toString().isBlank()) {
                    throw new IllegalStateException("Unfinished SQL before DELIMITER.");
                }
                delimiter = trimmed.substring("DELIMITER ".length()).trim();
                continue;
            }
            statement.append(line).append('\n');
            if (trimmed.endsWith(delimiter)) {
                String complete = statement.toString().stripTrailing();
                if (complete.matches("(?is).*\\blost_and_found\\b.*")) {
                    throw new IllegalStateException("A setup statement still names the app database; refusing to execute it.");
                }
                execute(connection, complete.substring(0, complete.length() - delimiter.length()));
                statement.setLength(0);
            }
        }
        if (!statement.toString().isBlank()) {
            throw new IllegalStateException("Unfinished statement at end of setup file.");
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            if (!rows.next()) {
                throw new AssertionError("Query returned no row: " + sql);
            }
            return rows.getString(1);
        }
    }

    private static void equal(String label, String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected [" + expected + "] but got [" + actual + "]");
        }
        passed++;
        System.out.println("PASS: " + label);
    }

    private static void rejectsSql(String label, SqlAction action, String expectedState) throws SQLException {
        try {
            action.run();
        } catch (SQLException expected) {
            if (!expectedState.equals(expected.getSQLState())) {
                throw new AssertionError(label + ": wrong SQL state " + expected.getSQLState(), expected);
            }
            passed++;
            System.out.println("PASS: " + label);
            return;
        }
        throw new AssertionError(label + ": expected SQLException");
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}
