import java.time.LocalDate;

// One object stores the details of a completed handover to a student.
// CONCEPT: Encapsulation. Private final fields and getters preserve the record.
public class ReturnRecord {
    private final int returnId;
    private final int foundReportId;
    private final int collectedByStudentId;
    private final LocalDate returnDate;

    // The constructor records which FOUND report was collected, by whom and when.
    public ReturnRecord(int returnId, int foundReportId, int collectedByStudentId,
                        LocalDate returnDate) {
        this.returnId = returnId;
        this.foundReportId = foundReportId;
        this.collectedByStudentId = collectedByStudentId;
        this.returnDate = returnDate;
    }

    public int getReturnId() {
        return returnId;
    }

    public int getFoundReportId() {
        return foundReportId;
    }

    public int getCollectedByStudentId() {
        return collectedByStudentId;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }
}
