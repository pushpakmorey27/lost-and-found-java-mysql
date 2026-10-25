import java.time.LocalDate;

// One Item object describes a lost or found report, not a separate database table.
// CONCEPT: An abstract parent class shares the fields used by both kinds of report.
// We create LostItem or FoundItem objects instead of creating Item directly.
public abstract class Item {
    // Private fields protect the details. Getters let other classes read them.
    private final int reportId;
    private final int studentId;
    private final int categoryId;
    private final int locationId;
    private final String itemName;
    private String description;
    private final LocalDate eventDate;
    private String status;

    // The constructor fills the common details for either kind of item report.
    public Item(int reportId, int studentId, int categoryId, int locationId,
                String itemName, String description, LocalDate eventDate) {
        this.reportId = reportId;
        this.studentId = studentId;
        this.categoryId = categoryId;
        this.locationId = locationId;
        this.itemName = itemName;
        this.description = description;
        this.eventDate = eventDate;
        this.status = "OPEN";
    }

    // An abstract method has no body here. Each child supplies LOST or FOUND.
    public abstract String getReportType();

    public int getReportId() {
        return reportId;
    }

    public int getStudentId() {
        return studentId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public int getLocationId() {
        return locationId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public String getStatus() {
        return status;
    }

    // This setter changes only the description. The calling code checks OPEN first.
    public void setDescription(String description) {
        this.description = description;
    }

    // Practice mode calls this after an item is returned or a lost report is closed.
    public void markResolved() {
        status = "RESOLVED";
    }
}
