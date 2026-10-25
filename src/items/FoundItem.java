import java.time.LocalDate;

// Inheritance lets FoundItem reuse Item's fields, constructor work and getters.
public class FoundItem extends Item {
    public FoundItem(int reportId, int studentId, int categoryId, int locationId,
                     String itemName, String description, LocalDate eventDate) {
        // Pass the common details to the parent Item constructor.
        super(reportId, studentId, categoryId, locationId, itemName, description, eventDate);
    }

    // The same method name returns FOUND for a FoundItem object.
    @Override
    public String getReportType() {
        return "FOUND";
    }
}
