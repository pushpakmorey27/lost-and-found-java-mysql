import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

/*
 * WHAT: Receive a browser request and choose the correct page or project action.
 * READ AFTER: Student, Item, LostItem, FoundItem, FormInput and Database.
 *
 * GET = show a page or search result. POST = accept a submitted form.
 * CONCEPTS: Objects, methods, if/else, overriding, exceptions and synchronization.
 * EXTRA LIBRARY: HttpHandler/HttpExchange are Java's browser communication helpers.
 * The MySQL code stays in Database.java; the HTML stays in WebPages.java.
 */
public class Website implements HttpHandler {
    private final Database database;
    private final WebPages pages;
    private final String formToken;
    private final int port;
    private final byte[] stylesheet;

    // Constructor: remember the objects and values this website needs.
    public Website(Database database, int port, byte[] stylesheet) {
        this.database = database;
        this.port = port;
        this.stylesheet = stylesheet;

        // This random value belongs to this run of the program, not to a student.
        // Only forms carrying this value are accepted for database changes.
        formToken = UUID.randomUUID().toString();
        pages = new WebPages(database, formToken);
    }

    // Java's server calls handle whenever a browser sends a request.
    // @Override implements the method required by the HttpHandler interface.
    // synchronized allows only one action at a time on our shared DB connection.
    @Override
    public synchronized void handle(HttpExchange request) throws IOException {
        try {
            if (!isLocalRequest(request)) {
                sendText(request, 403, "text/plain", "Use the local website address.");
                return;
            }

            String method = request.getRequestMethod();
            String path = request.getRequestURI().getPath();

            if (method.equals("GET")) {
                showPage(request, path);
            } else if (method.equals("POST")) {
                acceptForm(request, path);
            } else {
                request.getResponseHeaders().set("Allow", "GET, POST");
                sendError(request, 405, "Use the website's links and forms.");
            }
        } catch (IllegalArgumentException exception) {
            cancelFailedAction();
            sendError(request, 400, exception.getMessage());
        } catch (SQLException exception) {
            cancelFailedAction();
            sendError(request, 400, explainDatabaseError(exception));
        } finally {
            // Close this browser request after sending its response.
            request.close();
        }
    }

    // Check the Host header too: an unrelated hostname must not use our local app.
    private boolean isLocalRequest(HttpExchange request) {
        String host = request.getRequestHeaders().getFirst("Host");
        String numericAddress = "127.0.0.1:" + port;
        String localAddress = "localhost:" + port;
        return numericAddress.equals(host) || localAddress.equals(host);
    }

    // GET requests only read/display information. None of these actions changes rows.
    private void showPage(HttpExchange request, String path) throws IOException, SQLException {
        if (path.equals("/style.css")) {
            sendBytes(request, 200, "text/css", stylesheet);
            return;
        }
        if (path.equals("/favicon.ico")) {
            request.sendResponseHeaders(204, -1);
            return;
        }

        FormInput query = new FormInput(request.getRequestURI().getRawQuery());
        String html;

        if (path.equals("/")) {
            html = pages.reports(query);
        } else if (path.equals("/students")) {
            html = pages.students(query.get("notice"));
        } else if (path.equals("/report")) {
            html = pages.newReport(query.get("type"));
        } else if (path.equals("/return")) {
            html = pages.returnItem(query.get("id"));
        } else if (path.equals("/history")) {
            html = pages.history(query.get("notice"));
        } else if (path.equals("/lists")) {
            html = pages.lists();
        } else if (path.equals("/edit")) {
            html = pages.changeReport(query.number("id", "Report ID"), false);
        } else if (path.equals("/delete")) {
            html = pages.changeReport(query.number("id", "Report ID"), true);
        } else {
            sendError(request, 404, "Page not found.");
            return;
        }

        sendText(request, 200, "text/html", html);
    }

    // Check the browser submission before allowing it to change the database.
    private void acceptForm(HttpExchange request, String path) throws IOException, SQLException {
        String contentType = request.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.split(";", 2)[0].equals(
                "application/x-www-form-urlencoded")) {
            sendError(request, 415, "Submit an ordinary website form.");
            return;
        }

        // A normal form is small. Do not read an unlimited amount into memory.
        byte[] body = request.getRequestBody().readNBytes(16385);
        if (body.length > 16384) {
            sendError(request, 413, "The submitted form is too large.");
            return;
        }

        FormInput form = new FormInput(new String(body, StandardCharsets.UTF_8));
        if (!formToken.equals(form.get("token"))) {
            sendError(request, 403, "Reopen this page and try again.");
            return;
        }

        String nextPage = saveAction(path, form);
        if (nextPage == null) {
            sendError(request, 404, "Action not found.");
            return;
        }

        // 303 tells the browser to GET a page after the POST.
        // Refreshing that page therefore does not submit the form again.
        request.getResponseHeaders().set("Location", nextPage);
        request.sendResponseHeaders(303, -1);
    }

    // Match each form address to its action. The form fields are checked first.
    private String saveAction(String path, FormInput form) throws SQLException {
        if (path.equals("/students/add")) {
            addStudent(form);
            return successPage("/students", "Student added. Saved to MySQL.");
        } else if (path.equals("/reports/add")) {
            String type = addItem(form);
            return successPage("/", type + " report added. Saved to MySQL.");
        } else if (path.equals("/returns/add")) {
            returnItem(form);
            return successPage("/history", "Return saved. The FOUND report is now RESOLVED. "
                    + "Resolve a matching LOST report separately, if one exists.");
        } else if (path.equals("/reports/resolve")) {
            int reportId = form.number("id", "Report ID");
            database.resolveLostReport(reportId);
            return successPage("/", "LOST report resolved. Saved to MySQL.");
        } else if (path.equals("/reports/edit")) {
            int reportId = form.number("id", "Report ID");
            String description = form.text("description", "Description", 250);
            database.editDescription(reportId, description);
            return successPage("/", "Description updated. Saved to MySQL.");
        } else if (path.equals("/reports/delete")) {
            if (!form.get("confirm").equals("YES")) {
                throw new IllegalArgumentException("Tick the confirmation box before deleting.");
            }
            int reportId = form.number("id", "Report ID");
            database.deleteReport(reportId);
            return successPage("/", "Incorrect report deleted.");
        }
        return null;
    }

    // A Student object groups the three related values into one object.
    private void addStudent(FormInput form) throws SQLException {
        int studentId = form.number("id", "Student ID");
        String name = form.text("name", "Student name", 60);
        String contact = form.text("contact", "Contact", 80);

        Student student = new Student(studentId, name, contact);
        database.addStudent(student);
    }

    // LostItem and FoundItem share the Item parent class (inheritance).
    // The Item variable can hold either child object (polymorphism).
    private String addItem(FormInput form) throws SQLException {
        int reportId = form.number("id", "Report ID");
        int studentId = form.number("student", "Student");
        int categoryId = form.number("category", "Category");
        int locationId = form.number("location", "Location");
        String itemName = form.text("name", "Item name", 80);
        String description = form.text("description", "Description", 250);
        String type = form.get("type");
        Item item;

        if (type.equals("LOST")) {
            item = new LostItem(reportId, studentId, categoryId, locationId,
                    itemName, description, form.date("date"));
        } else if (type.equals("FOUND")) {
            item = new FoundItem(reportId, studentId, categoryId, locationId,
                    itemName, description, form.date("date"));
        } else {
            throw new IllegalArgumentException("Choose LOST or FOUND.");
        }

        database.addReport(item);
        return item.getReportType();
    }

    // MySQL's existing procedure saves the return and resolves the FOUND report together.
    private void returnItem(FormInput form) throws SQLException {
        int returnId = form.number("id", "Return ID");
        int reportId = form.number("report", "Found report");
        int collectorId = form.number("collector", "Collector");
        LocalDate date = form.date("date");
        database.recordReturn(returnId, reportId, collectorId, date);
    }

    // Encode spaces and special characters so a message can be part of a URL.
    private String successPage(String path, String message) {
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return path + "?notice=" + encodedMessage;
    }

    // Translate common MySQL errors into messages the student can understand.
    private String explainDatabaseError(SQLException error) {
        if (error.getErrorCode() == 1062) {
            return "That ID is already used. Choose another ID.";
        }
        if (error.getErrorCode() == 1452) {
            return "Choose an existing student, report, category and location.";
        }
        if ("45000".equals(error.getSQLState())) {
            return error.getMessage();
        }
        if (error.getSQLState() != null && error.getSQLState().startsWith("08")) {
            return "The MySQL connection was interrupted. Restart the website and check your records.";
        }
        return "MySQL could not finish this action. Check your records before trying again.";
    }

    // Rollback cancels uncommitted changes if an action fails.
    private void cancelFailedAction() {
        try {
            database.rollback();
        } catch (SQLException exception) {
            System.out.println("Connection interrupted. Restart the website.");
        }
    }

    // Main calls this when the program stops. synchronized waits for a current action.
    public synchronized void close() {
        try {
            database.close();
        } catch (SQLException exception) {
            System.out.println("Could not close the database connection cleanly.");
        }
    }

    // An error is still an HTML page, so the user sees a readable explanation.
    private void sendError(HttpExchange request, int status, String message) throws IOException {
        sendText(request, status, "text/html", pages.error(message));
    }

    // UTF-8 turns Java text into bytes the browser can read, including non-English text.
    private void sendText(HttpExchange request, int status, String type, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        sendBytes(request, status, type, bytes);
    }

    // These HTTP headers describe the response and keep the local forms/pages restricted.
    private void sendBytes(HttpExchange request, int status, String type, byte[] bytes) throws IOException {
        request.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        request.getResponseHeaders().set("Cache-Control", "no-store");
        request.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        request.getResponseHeaders().set("Content-Security-Policy", "default-src 'none'; style-src 'self'; "
                + "form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
        request.sendResponseHeaders(status, bytes.length);
        request.getResponseBody().write(bytes);
    }
}
