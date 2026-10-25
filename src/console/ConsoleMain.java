import java.io.IOException;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Scanner;

// This is the starting point for the optional text menu.
// The website starts from Main.java instead, but both use the same item classes.
public class ConsoleMain {
    public static void main(String[] args) {
        KeyboardInput input = new KeyboardInput(new Scanner(System.in));

        // CONCEPT: An interface reference can hold either implementation.
        // Database saves to MySQL. PracticeData keeps temporary records in arrays.
        Actions data = null;

        try {
            boolean invalidChoice = args.length > 1;
            if (args.length == 1) {
                if (!args[0].equals("--preview") && !args[0].equals("--mysql")) {
                    invalidChoice = true;
                }
            }
            if (invalidChoice) {
                System.out.println("Use --preview or --mysql.");
                return;
            }

            if (args.length == 1 && args[0].equals("--preview")) {
                data = new PracticeData();
                System.out.println("PREVIEW MODE: temporary example data, stored only in memory.");
                System.out.println("Changes disappear when you exit. Nothing is saved to MySQL.");
            } else {
                data = new Database(input);
                System.out.println("Connected to MySQL. Successful changes will be saved.");
            }

            ConsoleMenu menu = new ConsoleMenu(data, input);
            menu.run();
        } catch (ClassNotFoundException exception) {
            System.out.println("MySQL driver not found. Keep the Connector/J jar inside lib.");
        } catch (SQLException exception) {
            System.out.println("Could not open MySQL: " + exception.getMessage());
            System.out.println("Check that the MySQL service is running. If your password changed,");
            System.out.println("remove the saved login using the instructions in docs/MYSQL-SETUP.md.");
        } catch (IOException exception) {
            System.out.println("Could not read the saved login. See docs/MYSQL-SETUP.md to reset it.");
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        } catch (NoSuchElementException exception) {
            System.out.println("Input ended. Closing the program.");
        } finally {
            // finally runs even if an error occurs, so we can close open resources.
            if (data != null) {
                try {
                    data.close();
                } catch (SQLException exception) {
                    System.out.println("Could not close the database connection cleanly.");
                }
            }
            input.close();
        }
    }
}
