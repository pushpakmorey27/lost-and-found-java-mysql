import java.sql.SQLException;
import java.time.LocalDate;

/*
 * WHAT THIS FILE DOES
 * Lists the actions available in our Lost and Found project.
 *
 * OOP CONCEPT: Interface.
 * An interface says WHAT an object must be able to do.
 * Database supplies these actions using MySQL. PracticeData uses arrays instead.
 * This lets the console menu work with either version through one common type.
 *
 * "throws SQLException" tells the caller that a database operation can fail.
 * The menu or website catches the error and shows a useful message.
 */
public interface Actions {
    // Tell the menu whether it is using temporary practice data.
    boolean isPreview();

    // Save the details held in one Student object.
    void addStudent(Student student) throws SQLException;

    // Display all students in the console.
    void showStudents() throws SQLException;

    // Display the category choices, such as Books and Electronics.
    void showCategories() throws SQLException;

    // Display the location choices, such as Library and Canteen.
    void showLocations() throws SQLException;

    // Save a LostItem or FoundItem through their common parent type, Item.
    void addReport(Item report) throws SQLException;

    // Display reports matching a name, optionally limited to OPEN reports.
    void showReports(boolean openOnly, String keyword) throws SQLException;

    // Record who collected a found item and when they collected it.
    void recordReturn(int returnId, int reportId, int collectorId,
                      LocalDate returnDate) throws SQLException;

    // Close a lost report after the student has recovered the item.
    void resolveLostReport(int reportId) throws SQLException;

    // Display the history of items returned to students.
    void showReturns() throws SQLException;

    // Correct the description of a report that is still OPEN.
    void editDescription(int reportId, String description) throws SQLException;

    // Remove an incorrect report that is still OPEN.
    void deleteReport(int reportId) throws SQLException;

    // Cancel changes that have not yet been committed after an error.
    void rollback() throws SQLException;

    // Release the resources used by this version of the project.
    void close() throws SQLException;
}
