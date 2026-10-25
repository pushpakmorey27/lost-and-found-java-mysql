import java.time.LocalDate;

// CONCEPT: Inheritance. A LostItem has all the common fields and getters in Item.
public class LostItem extends Item {
    public LostItem(int reportId, int studentId, int categoryId, int locationId,
                    String itemName, String description, LocalDate eventDate) {
        // super calls the parent constructor so we do not repeat its assignments.
        super(reportId, studentId, categoryId, locationId, itemName, description, eventDate);
    }

    // CONCEPT: Method overriding. This child supplies the lost-report version.
    @Override
    public String getReportType() {
        return "LOST";
    }
}
