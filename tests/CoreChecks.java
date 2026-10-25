import java.time.LocalDate;

/* Run with test.cmd (Windows) or bash test.sh (Linux/macOS).
 * These checks call our actual classes. They do not need MySQL or a test library.
 * A failed check throws AssertionError, so the command and CI fail visibly.
 */
public class CoreChecks {
    private static int passed;

    public static void main(String[] args) {
        FormInput form = new FormInput("name=Ravi+More&contact=ravi%40example.com&note=a%3Db&empty");
        equal("Form decodes spaces", "Ravi More", form.get("name"));
        equal("Form decodes email", "ravi@example.com", form.get("contact"));
        equal("Value may contain equals", "a=b", form.get("note"));
        equal("Field without value is empty", "", form.get("empty"));
        equal("Missing field is empty", "", form.get("missing"));
        equal("Null form is safe", "", new FormInput(null).get("name"));
        equal("Unicode form input", "किताब", new FormInput("name=%E0%A4%95%E0%A4%BF%E0%A4%A4%E0%A4%BE%E0%A4%AC").get("name"));
        rejects("Malformed URL encoding", () -> new FormInput("name=%ZZ").get("name"));

        equal("Text trims surrounding spaces", "Ravi", new FormInput("name=+Ravi+").text("name", "Name", 4));
        rejects("Blank text", () -> new FormInput("name=+++").text("name", "Name", 4));
        rejects("Text exceeds column length", () -> new FormInput("name=Ravii").text("name", "Name", 4));
        equal("Positive ID accepted", 42, new FormInput("id=42").number("id", "ID"));
        rejects("Zero ID", () -> new FormInput("id=0").number("id", "ID"));
        rejects("Negative ID", () -> new FormInput("id=-1").number("id", "ID"));
        rejects("Decimal ID", () -> new FormInput("id=1.5").number("id", "ID"));
        rejects("Integer overflow", () -> new FormInput("id=2147483648").number("id", "ID"));
        rejects("Missing ID", () -> new FormInput("").number("id", "ID"));

        LocalDate today = LocalDate.now();
        equal("Today's date accepted", today, new FormInput("date=" + today).date("date"));
        equal("Real leap day accepted", LocalDate.of(2024, 2, 29), new FormInput("date=2024-02-29").date("date"));
        rejects("Impossible calendar date", () -> new FormInput("date=2025-02-29").date("date"));
        rejects("Future date", () -> new FormInput("date=" + today.plusDays(1)).date("date"));

        equal("Null HTML value", "", WebPages.escape(null));
        equal("Plain text preserved", "Blue notebook", WebPages.escape("Blue notebook"));
        equal("HTML and attribute delimiters escaped", "&lt;script&gt;&quot;&#39;&amp;&lt;/script&gt;",
                WebPages.escape("<script>\"'&</script>"));
        equal("Existing entity remains literal text", "&amp;lt;", WebPages.escape("&lt;"));

        // Both references have type Item. The child object determines the method result.
        Item lost = new LostItem(1, 1, 1, 1, "Keys", "Blue ring", today);
        Item found = new FoundItem(2, 1, 1, 1, "Keys", "Blue ring", today);
        equal("LostItem dispatch through Item reference", "LOST", lost.getReportType());
        equal("FoundItem dispatch through Item reference", "FOUND", found.getReportType());

        System.out.println("\nPASS: " + passed + " core checks. No database was used.");
    }

    private static void equal(String label, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected [" + expected + "] but got [" + actual + "]");
        }
        passed++;
        System.out.println("PASS: " + label);
    }

    private static void rejects(String label, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            passed++;
            System.out.println("PASS: " + label);
            return;
        }
        throw new AssertionError(label + ": expected IllegalArgumentException");
    }
}
