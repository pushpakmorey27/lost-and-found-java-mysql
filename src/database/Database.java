import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Properties;

/*
 * WHAT THIS FILE DOES
 * Connects Java to MySQL and carries out the project's SQL operations.
 * The website and console both use this class, so both use the same real data.
 *
 * CONCEPTS: interface, objects, methods, arrays, exceptions and file input/output.
 * JDBC is the extra Java topic that lets our program communicate with MySQL.
 * The SQL tables, keys, views, procedures and transactions are RDBMS concepts.
 */
public class Database implements Actions {
    // A Connection represents our open link to MySQL.
    // private keeps other classes from directly changing this link.
    // final means this field is assigned once, during construction.
    private final Connection connection;

    // Constructor: connect once when the program starts, then reuse that connection.
    public Database(KeyboardInput input)
            throws ClassNotFoundException, SQLException, IOException {
        File settingsFile = getSettingsFile();
        Properties settings = new Properties();
        boolean hasSavedLogin = settingsFile.exists();
        String url;
        String username;
        String password;

        if (hasSavedLogin) {
            // Properties holds small named settings, such as "url" and "username".
            // FileInputStream reads the saved file. try closes the file afterwards.
            try (FileInputStream file = new FileInputStream(settingsFile)) {
                settings.load(file);
            }
            url = settings.getProperty("url", "");
            username = settings.getProperty("username", "");
            password = settings.getProperty("password");

            if (url.isBlank() || username.isBlank() || password == null) {
                throw new IllegalArgumentException(
                        "Saved login is incomplete. Delete " + settingsFile + " and reopen.");
            }
            System.out.println("Connecting to MySQL using your saved login...");
        } else {
            System.out.println("ONE-TIME MYSQL SETUP - start MySQL Server before connecting.");
            System.out.println("After a successful login, the program will remember it for next time.");
            System.out.println("The settings file contains the database password. Keep it private.");

            url = input.readLine("JDBC URL (blank = local database): ").trim();
            if (url.isEmpty()) {
                url = "jdbc:mysql://127.0.0.1:3306/lost_and_found";
            }
            checkUrl(url);

            username = input.readLine("Database username (blank = lf_user): ").trim();
            if (username.isEmpty()) {
                username = "lf_user";
            }
            password = input.readPassword();
        }

        checkUrl(url);
        String savedUrl = url;

        // The driver options below require an encrypted database connection.
        // They also allow procedure calls without permission to read procedure bodies.
        String separator = "?";
        if (url.contains("?")) {
            separator = "&";
        }
        url = url + separator + "sslMode=REQUIRED&noAccessToProcedureBodies=true";

        // Connector/J is the driver: it translates Java JDBC calls into MySQL requests.
        Class.forName("com.mysql.cj.jdbc.Driver");
        DriverManager.setLoginTimeout(10);
        connection = DriverManager.getConnection(url, username, password);

        try {
            // With auto-commit off, we choose when a group of changes is saved.
            // commit() saves it; rollback() cancels changes not yet committed.
            connection.setAutoCommit(false);
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }

        // Remember a first-time login only AFTER MySQL accepts it.
        if (!hasSavedLogin) {
            settings.setProperty("url", savedUrl);
            settings.setProperty("username", username);
            settings.setProperty("password", password);
            saveSettings(settingsFile, settings);
        }
    }

    // Reject a URL for another database driver before trying to connect.
    private void checkUrl(String url) {
        if (!url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("The URL must start with jdbc:mysql://");
        }
    }

    // Find the private settings file outside the project and its shared ZIP.
    private static File getSettingsFile() {
        String localFolder = System.getenv("LOCALAPPDATA");
        if (localFolder == null) {
            localFolder = System.getProperty("user.home");
        }

        File normalFile = new File(localFolder, "LostAndFound/database.properties");
        // Checks can choose a temporary path without touching the student's login.
        String chosenPath = System.getProperty("lostfound.settings", normalFile.getPath());
        return new File(chosenPath);
    }

    // Save the login for next time; a saving error must not cancel a valid connection.
    private void saveSettings(File settingsFile, Properties settings) {
        try {
            File folder = settingsFile.getAbsoluteFile().getParentFile();
            if (!folder.isDirectory() && !folder.mkdirs()) {
                throw new IOException("Could not create the settings folder.");
            }

            // FileOutputStream writes the file; Properties stores named values in it.
            // This is a plain-text password file, so it must stay private.
            try (FileOutputStream file = new FileOutputStream(settingsFile)) {
                settings.store(file,
                        "Private MySQL login. Contains your password; keep this file private.");
            }
            System.out.println("Login remembered. Future launches will connect automatically.");
        } catch (IOException exception) {
            System.out.println("Connected, but the login could not be saved. Next launch may ask again.");
        }
    }

    // This version uses MySQL, so it is not the temporary practice version.
    @Override
    public boolean isPreview() {
        return false;
    }

    /*
     * Read SQL results into a two-dimensional array for the website.
     * table[row][column] holds one value, just like a cell in a table.
     * String... means the caller may supply zero or more search values.
     */
    private String[][] readRows(String sql, String... values) throws SQLException {
        String[][] table = new String[16][];
        int rowCount = 0;

        // A PreparedStatement holds SQL with ? placeholders for values.
        // setString sends each value separately; typed input cannot become SQL code.
        // This is useful even for names containing an apostrophe, such as O'Brien.
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int valueIndex = 0; valueIndex < values.length; valueIndex++) {
                // Java arrays start at 0, but SQL parameter positions start at 1.
                statement.setString(valueIndex + 1, values[valueIndex]);
            }

            // executeQuery runs a SELECT. ResultSet lets us read its returned rows.
            try (ResultSet rows = statement.executeQuery()) {
                int columnCount = rows.getMetaData().getColumnCount();
                while (rows.next()) {
                    // An array has a fixed size, so copy it into a larger array if full.
                    if (rowCount == table.length) {
                        table = Arrays.copyOf(table, table.length * 2);
                    }
                    table[rowCount] = new String[columnCount];

                    for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                        table[rowCount][columnIndex] = rows.getString(columnIndex + 1);
                    }
                    rowCount++;
                }
            }

            // Auto-commit is off, so even a SELECT can leave a read transaction open.
            // End it here: the next page can then see changes made in the MySQL app.
            // Binding ? values above and ending this read transaction are separate jobs.
            connection.commit();
        }

        // Remove unused empty spaces so the website receives only actual rows.
        return Arrays.copyOf(table, rowCount);
    }

    // Return students for the website, ordered by their ID.
    public String[][] getStudents() throws SQLException {
        return readRows("SELECT student_id, student_name, contact FROM lf_student ORDER BY student_id");
    }

    // Return the category choices used in the website's drop-down list.
    public String[][] getCategories() throws SQLException {
        return readRows("SELECT category_id, category_name FROM lf_category ORDER BY category_id");
    }

    // Return the location choices used in the website's drop-down list.
    public String[][] getLocations() throws SQLException {
        return readRows("SELECT location_id, location_name FROM lf_location ORDER BY location_id");
    }

    // Return matching reports; the SQL view joins the report to its related names.
    public String[][] getReports(boolean openOnly, String keyword) throws SQLException {
        String sql = "SELECT report_id, report_type, status, item_name, reported_by, contact, "
                + "category_name, location_name, event_date, description FROM lf_report_details "
                + "WHERE LOWER(item_name) LIKE ? ";
        if (openOnly) {
            sql = sql + "AND status = 'OPEN' ";
        }
        sql = sql + "ORDER BY report_id DESC";
        String searchValue = "%" + keyword.toLowerCase() + "%";
        return readRows(sql, searchValue);
    }

    // Return one report for its details or edit form, using the same column order.
    public String[][] getReport(int id) throws SQLException {
        String sql = "SELECT report_id, report_type, status, item_name, reported_by, contact, "
                + "category_name, location_name, event_date, description "
                + "FROM lf_report_details WHERE report_id = ?";
        return readRows(sql, String.valueOf(id));
    }

    // Return the website's history, with the latest return ID first.
    public String[][] getReturns() throws SQLException {
        String sql = "SELECT return_id, report_id, item_name, found_by, collected_by, return_date "
                + "FROM lf_return_details ORDER BY return_id DESC";
        return readRows(sql);
    }

    // Insert a student using the values held by a Student object.
    @Override
    public void addStudent(Student student) throws SQLException {
        String sql = "INSERT INTO lf_student (student_id, student_name, contact) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, student.getStudentId());
            statement.setString(2, student.getStudentName());
            statement.setString(3, student.getContact());
            // executeUpdate runs INSERT, UPDATE or DELETE, rather than a SELECT.
            statement.executeUpdate();
            connection.commit();
        }
    }

    // Print students for the original console interface.
    @Override
    public void showStudents() throws SQLException {
        System.out.println("ID | Student name | Contact");
        String sql = "SELECT student_id, student_name, contact FROM lf_student ORDER BY student_id";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            boolean found = false;
            while (rows.next()) {
                found = true;
                System.out.println(rows.getInt("student_id") + " | "
                        + rows.getString("student_name") + " | " + rows.getString("contact"));
            }
            if (!found) {
                System.out.println("No students yet.");
            }
        }
    }

    // Print category IDs so a console user can choose an existing category.
    @Override
    public void showCategories() throws SQLException {
        System.out.println("Categories:");
        String sql = "SELECT category_id, category_name FROM lf_category ORDER BY category_id";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                System.out.println(rows.getInt(1) + ". " + rows.getString(2));
            }
        }
    }

    // Print location IDs so a console user can choose an existing location.
    @Override
    public void showLocations() throws SQLException {
        System.out.println("Locations:");
        String sql = "SELECT location_id, location_name FROM lf_location ORDER BY location_id";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                System.out.println(rows.getInt(1) + ". " + rows.getString(2));
            }
        }
    }

    // Insert either kind of report through the common Item parent class.
    @Override
    public void addReport(Item report) throws SQLException {
        if (report.getEventDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("The lost/found date cannot be in the future.");
        }

        // report_id is the primary key: it uniquely identifies this report.
        // Student, category and location IDs are foreign keys: they refer to other tables.
        // MySQL checks those keys, so a report cannot refer to a missing student.
        String sql = "INSERT INTO lf_item_report (report_id, student_id, category_id, "
                + "location_id, item_name, description, event_date, report_type, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, report.getReportId());
            statement.setInt(2, report.getStudentId());
            statement.setInt(3, report.getCategoryId());
            statement.setInt(4, report.getLocationId());
            statement.setString(5, report.getItemName());
            statement.setString(6, report.getDescription());
            statement.setDate(7, Date.valueOf(report.getEventDate()));

            // Polymorphism: LostItem returns LOST; FoundItem returns FOUND.
            // The overridden method chooses the type through an Item reference.
            statement.setString(8, report.getReportType());
            statement.executeUpdate();
            connection.commit();
        }
    }

    // Search by item name and print matching reports in the console.
    @Override
    public void showReports(boolean openOnly, String keyword) throws SQLException {
        String search = keyword.toLowerCase();
        boolean found = false;
        String sql = "SELECT * FROM lf_report_details WHERE LOWER(item_name) LIKE ? ";
        if (openOnly) {
            sql = sql + "AND status = 'OPEN' ";
        }
        sql = sql + "ORDER BY report_id";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + search + "%");
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    printReport(rows.getInt("report_id"), rows.getString("report_type"),
                            rows.getString("status"), rows.getString("item_name"),
                            rows.getString("reported_by"), rows.getString("contact"),
                            rows.getString("category_name"), rows.getString("location_name"),
                            rows.getDate("event_date").toString(), rows.getString("description"));
                    found = true;
                }
            }
        }
        if (!found) {
            System.out.println("No matching reports.");
        }
    }

    // Ask the stored procedure to record a return and resolve its FOUND report together.
    @Override
    public void recordReturn(int returnId, int reportId, int collectorId,
                             LocalDate returnDate) throws SQLException {
        // CallableStatement calls a procedure already stored in MySQL.
        // This procedure owns a transaction: both changes succeed together or neither does.
        String sql = "{call lf_record_return(?, ?, ?, ?)}";
        try (CallableStatement statement = connection.prepareCall(sql)) {
            statement.setInt(1, returnId);
            statement.setInt(2, reportId);
            statement.setInt(3, collectorId);
            statement.setDate(4, Date.valueOf(returnDate));
            statement.execute();
            connection.commit();
        }
    }

    // Resolve an OPEN LOST report after the student has recovered the item.
    @Override
    public void resolveLostReport(int reportId) throws SQLException {
        String sql = "{call lf_resolve_lost_report(?)}";
        try (CallableStatement statement = connection.prepareCall(sql)) {
            statement.setInt(1, reportId);
            statement.execute();
            connection.commit();
        }
    }

    // Print return history from the view that joins reports and student names.
    @Override
    public void showReturns() throws SQLException {
        boolean found = false;
        String sql = "SELECT * FROM lf_return_details ORDER BY return_id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                printReturn(rows.getInt("return_id"), rows.getInt("report_id"),
                        rows.getString("item_name"), rows.getString("found_by"),
                        rows.getString("collected_by"), rows.getDate("return_date").toString());
                found = true;
            }
            if (!found) {
                System.out.println("No returns recorded.");
            }
        }
    }

    // Update only OPEN reports, keeping already completed records unchanged.
    @Override
    public void editDescription(int reportId, String description) throws SQLException {
        String sql = "UPDATE lf_item_report SET description = ? "
                + "WHERE report_id = ? AND status = 'OPEN'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, description);
            statement.setInt(2, reportId);
            int changedRows = statement.executeUpdate();
            if (changedRows == 0) {
                throw new IllegalArgumentException("Choose an existing OPEN report.");
            }
            connection.commit();
        }
    }

    // Delete an incorrect OPEN report after the interface has requested confirmation.
    @Override
    public void deleteReport(int reportId) throws SQLException {
        String sql = "DELETE FROM lf_item_report WHERE report_id = ? AND status = 'OPEN'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reportId);
            int changedRows = statement.executeUpdate();
            if (changedRows == 0) {
                throw new IllegalArgumentException("Choose an existing OPEN report.");
            }
            connection.commit();
        }
    }

    // The interface calls this after a failed action to cancel uncommitted changes.
    @Override
    public void rollback() throws SQLException {
        connection.rollback();
    }

    // Cancel any unfinished transaction, then close the database connection.
    @Override
    public void close() throws SQLException {
        try {
            connection.rollback();
        } finally {
            // finally runs even if rollback fails, so we still attempt to close MySQL.
            connection.close();
        }
    }

    // Keep the console layout for a report in one place instead of repeating it.
    private void printReport(int id, String type, String status, String item,
                             String student, String contact, String category,
                             String location, String date, String description) {
        System.out.println(id + " | " + type + " | " + status + " | " + item);
        System.out.println("  Student: " + student + " | Category: " + category
                + " | Location: " + location + " | Date: " + date);
        System.out.println("  " + description + " | Contact: " + contact);
    }

    // Keep the console layout for a return in one place.
    private void printReturn(int id, int reportId, String item, String finder,
                             String collector, String date) {
        System.out.println("Return " + id + " | Report " + reportId + " | " + item);
        System.out.println("  Found by: " + finder + " | Collected by: " + collector + " | Date: " + date);
    }
}
