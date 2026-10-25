import java.io.Console;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;

// This class reads keyboard input and repeats a question when the answer is invalid.
// The console menu and first-time database login both reuse these methods.
public class KeyboardInput {
    private final Scanner keyboard;

    public KeyboardInput(Scanner keyboard) {
        this.keyboard = keyboard;
    }

    // CONCEPT: Method overloading. Both methods are called readNumber.
    // This shorter version reads a positive ID; the other also accepts a range.
    public int readNumber(String prompt) {
        return readNumber(prompt, 1, Integer.MAX_VALUE);
    }

    public int readNumber(String prompt, int minimum, int maximum) {
        while (true) {
            System.out.print(prompt);
            try {
                String text = keyboard.nextLine().trim();
                int number = Integer.parseInt(text);
                if (number >= minimum && number <= maximum) {
                    return number;
                }
            } catch (NumberFormatException exception) {
                // CONCEPT: Exception handling. Letters cannot become a whole number.
                // We ask again instead of letting the program stop with an error.
            }
            System.out.println("Enter a whole number from " + minimum + " to " + maximum + ".");
        }
    }

    public String readText(String prompt, int maximumLength) {
        while (true) {
            System.out.print(prompt);
            String text = keyboard.nextLine().trim();
            if (!text.isEmpty() && text.length() <= maximumLength) {
                return text;
            }
            System.out.println("Enter 1 to " + maximumLength + " characters.");
        }
    }

    public LocalDate readDate(String label) {
        while (true) {
            System.out.print(label + " (YYYY-MM-DD, blank = today): ");
            String text = keyboard.nextLine().trim();
            try {
                LocalDate date;
                if (text.isEmpty()) {
                    date = LocalDate.now();
                } else {
                    date = LocalDate.parse(text);
                }

                if (!date.isAfter(LocalDate.now())) {
                    return date;
                }
                System.out.println("The date cannot be in the future.");
            } catch (DateTimeParseException exception) {
                System.out.println("Enter a real date in YYYY-MM-DD format.");
            }
        }
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return keyboard.nextLine();
    }

    public String readPassword() {
        // A real terminal can hide a password while it is typed.
        Console console = System.console();
        if (console != null) {
            char[] characters = console.readPassword(
                    "Database password (typing is hidden; press Enter when done): ");
            if (characters == null) {
                throw new NoSuchElementException();
            }
            return new String(characters);
        }
        return readLine("Database password (visible in this terminal): ");
    }

    public void close() {
        keyboard.close();
    }
}
