import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Scanner;

/*
 * START HERE: This is the website's starting file.
 *
 * WHAT IT DOES:
 * 1. Reads the CSS file that controls the page's appearance.
 * 2. Opens our existing MySQL database connection.
 * 3. Starts the local website and opens the browser.
 *
 * CONCEPTS: main method, objects, method calls, files and exception handling.
 * EXTRA LIBRARY: HttpServer is Java's built-in helper for talking to a browser.
 * The actual project actions are in Website.java and Database.java.
 */
public class Main {

    // Java starts running here. "static" means no Main object is needed first.
    public static void main(String[] args) {
        KeyboardInput keyboard = new KeyboardInput(new Scanner(System.in));
        Database database = null;
        HttpServer server = null;
        boolean websiteStarted = false;

        try {
            // 8080 is the website port. The optional setting is useful for testing.
            // This is separate from MySQL's database port, which is normally 3306.
            int port = Integer.parseInt(System.getProperty("lostfound.port", "8080"));
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Choose a valid website port.");
            }

            byte[] css = Files.readAllBytes(Path.of("web", "style.css"));

            // 127.0.0.1 means this computer. Other computers cannot open this server.
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            database = new Database(keyboard);

            // An object keeps the connection and the page-making code together.
            Website website = new Website(database, port, css);
            server.createContext("/", website);
            server.setExecutor(null);

            prepareForClosing(server, website);
            server.start();
            websiteStarted = true;

            String address = "http://127.0.0.1:" + port;
            System.out.println("\nLOST AND FOUND WEBSITE IS READY");
            System.out.println("Open " + address + " in your browser.");
            System.out.println("Keep this window open. Press Ctrl+C to stop the website.");
            System.out.println("Your forms save to the same MySQL database as the console program.");

            if (!Boolean.getBoolean("lostfound.noBrowser")) {
                openBrowser(address);
            }
        } catch (ClassNotFoundException exception) {
            System.out.println("MySQL driver missing. Keep the Connector/J jar in lib.");
        } catch (SQLException exception) {
            System.out.println("Could not connect to MySQL: " + exception.getMessage());
            System.out.println("Check the MySQL service and the saved login. See docs/MYSQL-SETUP.md.");
        } catch (BindException exception) {
            System.out.println("Website port is already in use. An existing copy may be running.");
            System.out.println("Try http://127.0.0.1:" + System.getProperty("lostfound.port", "8080"));
        } catch (IOException exception) {
            System.out.println("Could not open the website files or connection settings.");
            System.out.println("Open the whole project folder and use run-website.cmd. See docs/WEBSITE.md.");
        } catch (IllegalArgumentException | NoSuchElementException exception) {
            System.out.println(exception.getMessage());
        } finally {
            // "finally" also runs when startup fails, so unused resources are closed.
            // A successfully started website stays open until the user stops Java.
            if (!websiteStarted) {
                if (server != null) {
                    server.stop(0);
                }
                if (database != null) {
                    try {
                        database.close();
                    } catch (SQLException exception) {
                        System.out.println("Could not close the connection cleanly.");
                    }
                }
            }
            keyboard.close();
        }
    }

    // Register a task to run when Java closes, including when Ctrl+C is pressed.
    // A Thread is a separate path of execution. Java calls this small task at exit.
    // The () -> { ... } syntax simply supplies the task's instructions to Thread.
    private static void prepareForClosing(HttpServer server, Website website) {
        Thread closingTask = new Thread(() -> {
            server.stop(1);
            website.close();
        });
        Runtime.getRuntime().addShutdownHook(closingTask);
    }

    // Opening the browser is only a convenience: the printed address also works.
    private static void openBrowser(String address) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(address));
            }
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            System.out.println("Open the address above manually in your browser.");
        }
    }
}
