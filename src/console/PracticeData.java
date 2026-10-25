import java.time.LocalDate;

// This optional class lets us practise the text menu without MySQL.
// CONCEPT: An interface. PracticeData implements the same Actions as Database.
// All its data is temporary and disappears when this Java program closes.
public class PracticeData implements Actions {
    // static final gives this shared limit a name instead of repeating 200.
    private static final int MAX_RECORDS = 200;

    // CONCEPT: Arrays. Each array stores several objects of the same type.
    // The counts tell us how many positions contain records, starting at index 0.
    private final Student[] students = new Student[MAX_RECORDS];
    private final Item[] reports = new Item[MAX_RECORDS];
    private final ReturnRecord[] returns = new ReturnRecord[MAX_RECORDS];
    private int studentCount;
    private int reportCount;
    private int returnCount;

    // Index 0 is blank so that indexes 1 to 4 match our category/location IDs.
    // MySQL mode gets these choices from real tables instead of these arrays.
    private final String[] categories = {"", "Electronics", "Books", "Keys", "Other"};
    private final String[] locations = {"", "Library", "Canteen", "Classroom", "Other"};

    public PracticeData() {
        loadPreviewData();
    }

    @Override
    public boolean isPreview() {
        return true;
    }

    @Override
    public void addStudent(Student student) {
        if (findStudent(student.getStudentId()) != null) {
            throw new IllegalArgumentException("That student ID already exists.");
        }
        checkCapacity(studentCount);
        students[studentCount] = student;
        studentCount = studentCount + 1;
    }

    @Override
    public void showStudents() {
        System.out.println("ID | Student name | Contact");
        for (int index = 0; index < studentCount; index++) {
            Student student = students[index];
            System.out.println(student.getStudentId() + " | "
                    + student.getStudentName() + " | " + student.getContact());
        }
        if (studentCount == 0) {
            System.out.println("No students yet.");
        }
    }

    @Override
    public void showCategories() {
        System.out.println("Categories:");
        for (int index = 1; index < categories.length; index++) {
            System.out.println(index + ". " + categories[index]);
        }
    }

    @Override
    public void showLocations() {
        System.out.println("Locations:");
        for (int index = 1; index < locations.length; index++) {
            System.out.println(index + ". " + locations[index]);
        }
    }

    @Override
    public void addReport(Item report) {
        if (report.getEventDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("The lost/found date cannot be in the future.");
        }
        if (findReport(report.getReportId()) != null) {
            throw new IllegalArgumentException("That report ID already exists.");
        }
        requireStudent(report.getStudentId());
        if (report.getCategoryId() < 1 || report.getCategoryId() >= categories.length) {
            throw new IllegalArgumentException("Choose a category from the list.");
        }
        if (report.getLocationId() < 1 || report.getLocationId() >= locations.length) {
            throw new IllegalArgumentException("Choose a location from the list.");
        }

        checkCapacity(reportCount);
        reports[reportCount] = report;
        reportCount = reportCount + 1;
    }

    @Override
    public void showReports(boolean openOnly, String keyword) {
        String search = keyword.toLowerCase();
        boolean matchingReportFound = false;

        for (int index = 0; index < reportCount; index++) {
            Item report = reports[index];
            boolean matchesStatus = !openOnly || report.getStatus().equals("OPEN");
            boolean matchesName = report.getItemName().toLowerCase().contains(search);

            if (matchesStatus && matchesName) {
                Student student = findStudent(report.getStudentId());
                printReport(report.getReportId(), report.getReportType(), report.getStatus(),
                        report.getItemName(), student.getStudentName(), student.getContact(),
                        categories[report.getCategoryId()], locations[report.getLocationId()],
                        report.getEventDate().toString(), report.getDescription());
                matchingReportFound = true;
            }
        }
        if (!matchingReportFound) {
            System.out.println("No matching reports.");
        }
    }

    @Override
    public void recordReturn(int returnId, int reportId, int collectorId,
                             LocalDate returnDate) {
        Item report = requireOpenReport(reportId);
        if (!report.getReportType().equals("FOUND")) {
            throw new IllegalArgumentException("Only an OPEN FOUND report can be returned.");
        }
        requireStudent(collectorId);
        if (returnDate.isBefore(report.getEventDate()) || returnDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Return date must be between the finding date and today.");
        }
        for (int index = 0; index < returnCount; index++) {
            if (returns[index].getReturnId() == returnId) {
                throw new IllegalArgumentException("That return ID already exists.");
            }
            if (returns[index].getFoundReportId() == reportId) {
                throw new IllegalArgumentException("That report already has a return.");
            }
        }

        checkCapacity(returnCount);
        ReturnRecord returnRecord = new ReturnRecord(returnId, reportId, collectorId, returnDate);
        returns[returnCount] = returnRecord;
        returnCount = returnCount + 1;
        report.markResolved();
    }

    @Override
    public void resolveLostReport(int reportId) {
        Item report = requireOpenReport(reportId);
        if (!report.getReportType().equals("LOST")) {
            throw new IllegalArgumentException("Choose an OPEN LOST report.");
        }
        report.markResolved();
    }

    @Override
    public void showReturns() {
        for (int index = 0; index < returnCount; index++) {
            ReturnRecord returnRecord = returns[index];
            Item report = findReport(returnRecord.getFoundReportId());
            String finder = findStudent(report.getStudentId()).getStudentName();
            String collector = findStudent(returnRecord.getCollectedByStudentId()).getStudentName();
            printReturn(returnRecord.getReturnId(), report.getReportId(), report.getItemName(),
                    finder, collector, returnRecord.getReturnDate().toString());
        }
        if (returnCount == 0) {
            System.out.println("No returns recorded.");
        }
    }

    @Override
    public void editDescription(int reportId, String description) {
        Item report = requireOpenReport(reportId);
        report.setDescription(description);
    }

    @Override
    public void deleteReport(int reportId) {
        requireOpenReport(reportId);
        for (int index = 0; index < reportCount; index++) {
            if (reports[index].getReportId() == reportId) {
                // Shift later objects one place left to fill the deleted position.
                for (int nextIndex = index; nextIndex < reportCount - 1; nextIndex++) {
                    reports[nextIndex] = reports[nextIndex + 1];
                }
                reportCount = reportCount - 1;
                reports[reportCount] = null;
                return;
            }
        }
    }

    @Override
    public void rollback() {
        // No SQL transaction exists here: checks happen before array values change.
    }

    @Override
    public void close() {
        // No database connection exists in practice mode, so nothing needs closing.
    }

    private Student findStudent(int id) {
        // Linear search: check each stored student until the ID matches.
        for (int index = 0; index < studentCount; index++) {
            if (students[index].getStudentId() == id) {
                return students[index];
            }
        }
        return null;
    }

    private void requireStudent(int id) {
        if (findStudent(id) == null) {
            throw new IllegalArgumentException("Student ID not found. Add the student first.");
        }
    }

    private Item findReport(int id) {
        for (int index = 0; index < reportCount; index++) {
            if (reports[index].getReportId() == id) {
                return reports[index];
            }
        }
        return null;
    }

    private Item requireOpenReport(int id) {
        Item report = findReport(id);
        if (report == null || !report.getStatus().equals("OPEN")) {
            throw new IllegalArgumentException("Choose an existing OPEN report.");
        }
        return report;
    }

    private void checkCapacity(int count) {
        if (count >= MAX_RECORDS) {
            throw new IllegalArgumentException("Preview is full (200 records). Restart to reset it.");
        }
    }

    private void printReport(int id, String type, String status, String item,
                             String student, String contact, String category,
                             String location, String date, String description) {
        System.out.println(id + " | " + type + " | " + status + " | " + item);
        System.out.println("  Student: " + student + " | Category: " + category
                + " | Location: " + location + " | Date: " + date);
        System.out.println("  " + description + " | Contact: " + contact);
    }

    private void printReturn(int id, int reportId, String item, String finder,
                             String collector, String date) {
        System.out.println("Return " + id + " | Report " + reportId + " | " + item);
        System.out.println("  Found by: " + finder + " | Collected by: " + collector + " | Date: " + date);
    }

    private void loadPreviewData() {
        LocalDate today = LocalDate.now();

        // Fixed sample positions make the starting data easy to read.
        students[0] = new Student(1, "Asha", "asha@example.com");
        students[1] = new Student(2, "Ravi", "ravi@example.com");
        students[2] = new Student(3, "Neha", "neha@example.com");
        students[3] = new Student(4, "Arjun", "arjun@example.com");
        studentCount = 4;

        reports[0] = new LostItem(101, 2, 1, 1, "Calculator",
                "Black calculator with a red sticker", today.minusDays(3));
        reports[1] = new FoundItem(102, 1, 1, 1, "Calculator",
                "Black calculator with a red sticker", today.minusDays(2));
        reports[2] = new LostItem(103, 3, 2, 3, "Notebook",
                "Blue notebook with a white label", today.minusDays(1));
        reports[3] = new FoundItem(104, 4, 3, 2, "Keys",
                "Two keys on a green keyring", today.minusDays(1));
        reports[4] = new LostItem(105, 1, 4, 2, "Water bottle",
                "Steel bottle with a blue cap", today.minusDays(2));
        reports[5] = new FoundItem(106, 3, 2, 1, "Textbook",
                "Database textbook with a plain cover", today.minusDays(1));
        reportCount = 6;

        returns[0] = new ReturnRecord(501, 102, 2, today);
        returnCount = 1;
        reports[0].markResolved();
        reports[1].markResolved();
    }
}
