import java.sql.SQLException;
import java.time.LocalDate;

// The optional text menu sends each choice to a small method below.
// It asks for input; the Actions object performs the actual data operation.
public class ConsoleMenu {
    private final Actions data;
    private final KeyboardInput input;

    public ConsoleMenu(Actions data, KeyboardInput input) {
        this.data = data;
        this.input = input;
    }

    public void run() {
        int choice;
        // A do-while loop shows the menu at least once, then repeats until Exit.
        do {
            showMenu();
            choice = input.readNumber("Choose an option: ", 0, 11);
            try {
                switch (choice) {
                    case 1:
                        addStudent();
                        break;
                    case 2:
                        data.showStudents();
                        break;
                    case 3:
                        addReport(false);
                        break;
                    case 4:
                        addReport(true);
                        break;
                    case 5:
                        data.showReports(false, "");
                        break;
                    case 6:
                        String keyword = input.readText("Item name to search: ", 80);
                        data.showReports(true, keyword);
                        break;
                    case 7:
                        recordReturn();
                        break;
                    case 8:
                        int lostId = input.readNumber("OPEN LOST report ID: ");
                        data.resolveLostReport(lostId);
                        showSuccess("Lost report resolved.");
                        break;
                    case 9:
                        data.showReturns();
                        break;
                    case 10:
                        int editId = input.readNumber("OPEN report ID: ");
                        String description = input.readText("Corrected description: ", 250);
                        data.editDescription(editId, description);
                        showSuccess("Description updated.");
                        break;
                    case 11:
                        deleteReport();
                        break;
                    case 0:
                        System.out.println("Goodbye!");
                        break;
                }
            } catch (IllegalArgumentException exception) {
                if (!cancelAction()) {
                    return;
                }
                System.out.println("Please check: " + exception.getMessage());
            } catch (SQLException exception) {
                if (!cancelAction()) {
                    return;
                }
                System.out.println("Database error: " + exception.getMessage());
                System.out.println("Check the database records before retrying the action.");
            }
        } while (choice != 0);
    }

    private void showMenu() {
        System.out.println();
        System.out.println("========== LOST AND FOUND SYSTEM ==========");
        if (data.isPreview()) {
            System.out.println("Mode: PREVIEW (temporary data)");
        } else {
            System.out.println("Mode: MYSQL (saved data)");
        }
        System.out.println("1. Add student");
        System.out.println("2. View students");
        System.out.println("3. Report a LOST item");
        System.out.println("4. Report a FOUND item");
        System.out.println("5. View all reports");
        System.out.println("6. Search OPEN reports by item name");
        System.out.println("7. Record return of a FOUND item");
        System.out.println("8. Resolve a LOST report");
        System.out.println("9. View return history");
        System.out.println("10. Edit an OPEN report's description");
        System.out.println("11. Delete an incorrect OPEN report");
        System.out.println("0. Exit");
    }

    private void addStudent() throws SQLException {
        int id = input.readNumber("New student ID: ");
        String name = input.readText("Student name: ", 60);
        String contact = input.readText("Contact (email or phone): ", 80);

        Student student = new Student(id, name, contact);
        data.addStudent(student);
        showSuccess("Student added.");
    }

    private void addReport(boolean found) throws SQLException {
        System.out.println("Use an existing student, category and location ID from these lists.");
        data.showStudents();
        data.showCategories();
        data.showLocations();

        int id = input.readNumber("New report ID: ");
        String studentPrompt;
        String datePrompt;
        if (found) {
            studentPrompt = "Finder's student ID: ";
            datePrompt = "Date found";
        } else {
            studentPrompt = "Owner's student ID: ";
            datePrompt = "Date lost";
        }
        int studentId = input.readNumber(studentPrompt);
        int categoryId = input.readNumber("Category ID: ");
        int locationId = input.readNumber("Location ID: ");
        String name = input.readText("Item name: ", 80);
        String description = input.readText("Description: ", 250);
        LocalDate date = input.readDate(datePrompt);

        // CONCEPT: Polymorphism. One Item reference can hold either child object.
        Item report;
        if (found) {
            report = new FoundItem(id, studentId, categoryId, locationId, name, description, date);
        } else {
            report = new LostItem(id, studentId, categoryId, locationId, name, description, date);
        }
        data.addReport(report);
        showSuccess(report.getReportType() + " report added with status OPEN.");
    }

    private void recordReturn() throws SQLException {
        int returnId = input.readNumber("New return ID: ");
        int reportId = input.readNumber("OPEN FOUND report ID: ");
        data.showStudents();
        int collectorId = input.readNumber("Collector's student ID: ");
        LocalDate date = input.readDate("Return date");

        data.recordReturn(returnId, reportId, collectorId, date);
        showSuccess("Return recorded. The FOUND report is now RESOLVED.");
        System.out.println("If there is a matching LOST report, resolve it separately using option 8.");
    }

    private void deleteReport() throws SQLException {
        int deleteId = input.readNumber("Incorrect OPEN report ID: ");
        System.out.print("Delete this report? Type YES to confirm: ");
        String confirmation = input.readLine("").trim();
        if (confirmation.equals("YES")) {
            data.deleteReport(deleteId);
            showSuccess("Report deleted.");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private void showSuccess(String message) {
        System.out.println(message);
        if (data.isPreview()) {
            System.out.println("Preview change only; it will disappear when the program closes.");
        } else {
            System.out.println("Saved to MySQL.");
        }
    }

    private boolean cancelAction() {
        // Rollback cancels unfinished database work after an operation fails.
        try {
            data.rollback();
            return true;
        } catch (SQLException exception) {
            System.out.println("Connection failed. Close and restart the program.");
            return false;
        }
    }
}
