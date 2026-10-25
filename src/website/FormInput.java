import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/*
 * WHAT: Read and check the values sent by an HTML form.
 * CONCEPTS: A class, constructor, private field, arrays, loops and exceptions.
 * WHY: Checking input here avoids repeating the same checks for every page.
 * Example form data: id=5&name=Ravi&contact=ravi%40example.com
 */
public class FormInput {
    // "private" keeps this internal detail inside this class (encapsulation).
    // The array stores separate name=value pairs from one form submission.
    private final String[] fields;

    // The constructor prepares one FormInput object for one browser request.
    public FormInput(String encodedText) {
        if (encodedText == null) {
            fields = new String[0];
        } else {
            fields = encodedText.split("&");
        }
    }

    // A loop finds a field by its name, e.g. get("name") returns "Ravi".
    public String get(String fieldName) {
        for (String field : fields) {
            // Split only at the first '='. The value itself may contain '='.
            String[] nameAndValue = field.split("=", 2);
            String name = decode(nameAndValue[0]);

            if (name.equals(fieldName)) {
                if (nameAndValue.length == 2) {
                    return decode(nameAndValue[1]);
                }
                return "";
            }
        }
        return "";
    }

    // Browsers encode characters in transit: %40 represents @, and + a space.
    private String decode(String text) {
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }

    // trim removes surrounding spaces. The limit matches the database column.
    public String text(String fieldName, String label, int maximumLength) {
        String value = get(fieldName).trim();
        if (value.isEmpty() || value.length() > maximumLength) {
            throw new IllegalArgumentException(label + " must contain 1 to " + maximumLength + " characters.");
        }
        return value;
    }

    // IDs must be positive whole numbers. An exception lets Website show an error.
    public int number(String fieldName, String label) {
        try {
            int value = Integer.parseInt(get(fieldName).trim());
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException exception) {
            // Bad text, blank values and oversized numbers receive one clear message.
        }
        throw new IllegalArgumentException(label + " must be a positive whole number.");
    }

    // LocalDate represents a calendar date, without a time of day.
    public LocalDate date(String fieldName) {
        try {
            LocalDate date = LocalDate.parse(get(fieldName));
            if (!date.isAfter(LocalDate.now())) {
                return date;
            }
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Choose a real date.");
        }
        throw new IllegalArgumentException("The date cannot be in the future.");
    }
}
