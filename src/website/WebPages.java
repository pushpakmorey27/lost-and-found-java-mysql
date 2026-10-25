import java.sql.SQLException;
import java.time.LocalDate;

/*
 * WHAT: Make the HTML that the browser displays. This file does not save data.
 * CONCEPTS: Objects, methods, strings, arrays, loops and if/else.
 * WHY: Keeping page appearance here makes the database and item classes easier to read.
 *
 * HOW TO READ THIS FILE:
 * - HTML is the text between triple quotes (a Java multi-line string).
 * - %s is a space for a value; formatted(...) fills those spaces left to right.
 * - escape(...) makes user/database text safe to show inside HTML.
 * - Small helpers at the bottom make repeated form fields and tables.
 * HTML and CSS provide the browser interface for this Java application.
 */
public class WebPages {
    // These private fields are only used inside this class (encapsulation).
    private final Database database;
    private final String formToken;

    // Constructor: every page uses the same database connection and form token.
    public WebPages(Database database, String formToken) {
        this.database = database;
        this.formToken = formToken;
    }

    // A description such as "Keys <blue>" must display as text, not as an HTML tag.
    // Do this to every value coming from a user or database before adding it to HTML.
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        text = text.replace("&", "&amp;");
        text = text.replace("<", "&lt;");
        text = text.replace(">", "&gt;");
        text = text.replace("\"", "&quot;");
        text = text.replace("'", "&#39;");
        return text;
    }

    // All pages share one header, navigation bar and footer.
    private String page(String title, String activePage, String content, String notice) {
        String navigation = "";
        navigation += navigationLink("/", "Reports", activePage);
        navigation += navigationLink("/students", "Students", activePage);
        navigation += navigationLink("/return", "Return an item", activePage);
        navigation += navigationLink("/history", "Return history", activePage);
        navigation += navigationLink("/lists", "Categories & places", activePage);

        String message = "";
        if (!notice.isEmpty()) {
            message = "<div class=\"notice\" role=\"status\">" + escape(notice) + "</div>";
        }

        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>%s | Lost &amp; Found</title>
                    <link rel="stylesheet" href="/style.css">
                </head>
                <body>
                    <a class="skip" href="#main">Skip to content</a>
                    <header>
                        <div class="topbar wrap">
                            <a class="brand" href="/">
                                <span class="brand-mark">LF</span>
                                <span>Lost &amp; Found<small>Campus item register</small></span>
                            </a>
                            <span class="connection"><span></span> Local MySQL</span>
                        </div>
                        <nav class="wrap" aria-label="Main navigation">%s</nav>
                    </header>
                    <main id="main" class="wrap">%s%s</main>
                    <footer class="wrap">Lost &amp; Found System
                        <span>Java &middot; MySQL &middot; Local project</span>
                    </footer>
                </body>
                </html>
                """;
        return html.formatted(escape(title), navigation, message, content);
    }

    // Mark the current navigation link so CSS can underline it.
    private String navigationLink(String address, String label, String activePage) {
        String current = "";
        if (address.equals(activePage)) {
            current = " aria-current=\"page\"";
        }
        return "<a href=\"" + address + "\"" + current + ">" + escape(label) + "</a>";
    }

    // HOME PAGE: count open items, show the search form, then display report rows.
    public String reports(FormInput query) throws SQLException {
        String keyword = query.get("q").trim();
        if (keyword.length() > 80) {
            throw new IllegalArgumentException("Search using at most 80 characters.");
        }
        boolean openOnly = query.get("open").equals("1");
        String[][] allReports = database.getReports(false, "");

        int openLost = 0;
        int openFound = 0;
        for (String[] report : allReports) {
            String type = report[1];
            String status = report[2];
            if (status.equals("OPEN")) {
                if (type.equals("LOST")) {
                    openLost++;
                } else {
                    openFound++;
                }
            }
        }

        String content = """
                <section class="hero">
                    <div><p class="eyebrow">A little help, a happy reunion.</p>
                        <h1>Let's get it back.</h1>
                        <p>Report a missing item, browse what was found,<br class="desktop-break">
                        and keep a record of every return.</p>
                    </div>
                    <div class="hero-actions">
                        <a class="button primary" href="/report?type=LOST">+ Report a lost item</a>
                        <a class="button secondary" href="/report?type=FOUND">+ Report a found item</a>
                    </div>
                </section>
                <section class="stats" aria-label="Register summary">
                """;
        content += summaryCard(openLost, "Open lost reports", "Looking for an item", "lost");
        content += summaryCard(openFound, "Open found reports", "Waiting to be collected", "found");
        content += summaryCard(database.getReturns().length, "Items returned", "Handovers recorded", "returned");
        content += """
                </section>
                <section class="panel">
                    <div class="section-title">
                        <div><h2>Item reports</h2><p>All the details in one place.</p></div>
                        <span class="subtle">%s total reports</span>
                    </div>
                """.formatted(allReports.length);
        content += searchForm(keyword, openOnly);

        // Reuse the rows already read unless the user actually requested a filter.
        String[][] reportsToShow = allReports;
        if (!keyword.isEmpty() || openOnly) {
            reportsToShow = database.getReports(openOnly, keyword);
        }
        content += reportsTable(reportsToShow);
        content += """
                <p class="panel-note">Return a FOUND item after handover. Resolve a LOST report once the owner has recovered it.</p>
                </section>
                """;
        return page("Reports", "/", content, query.get("notice"));
    }

    // A small repeated display: count, heading and explanation.
    private String summaryCard(int count, String label, String detail, String colour) {
        return """
                <div class="stat %s"><span class="stat-number">%s</span>
                    <div><h2>%s</h2><p>%s</p></div>
                </div>
                """.formatted(colour, count, label, detail);
    }

    // GET keeps the search in the URL. It reads records and changes nothing.
    private String searchForm(String keyword, boolean openOnly) {
        String checked = "";
        if (openOnly) {
            checked = " checked";
        }
        return """
                <form class="search" method="get" action="/">
                    <div><label for="q">Search item name</label>
                        <input id="q" name="q" maxlength="80"
                            placeholder="Try keys, notebook, calculator..." value="%s">
                    </div>
                    <label class="check"><input type="checkbox" name="open" value="1"%s> Open reports only</label>
                    <button class="primary" type="submit">Search</button>
                    <a href="/">Clear</a>
                </form>
                """.formatted(escape(keyword), checked);
    }

    // A loop changes a two-dimensional array of rows into an HTML table.
    private String reportsTable(String[][] reports) {
        if (reports.length == 0) {
            return """
                    <div class="empty"><h3>No matching reports</h3>
                        <p>Try another item name or clear the filter.</p>
                    </div>
                    """;
        }
        String html = """
                <div class="table-scroll"><table>
                    <caption class="sr-only">Lost and found item reports</caption>
                    <thead><tr><th>Item / report</th><th>Reported by</th><th>Category / place</th>
                        <th>Date</th><th>Status</th><th>Actions</th></tr></thead>
                    <tbody>
                """;
        for (String[] report : reports) {
            html += reportRow(report);
        }
        html += "</tbody></table></div>";
        return html;
    }

    // Give every column a name once. The column order comes from Database.getReports.
    private String reportRow(String[] row) {
        String reportId = escape(row[0]);
        String type = escape(row[1]);
        String status = escape(row[2]);
        String itemName = escape(row[3]);
        String studentName = escape(row[4]);
        String contact = escape(row[5]);
        String category = escape(row[6]);
        String location = escape(row[7]);
        String date = escape(row[8]);
        String description = escape(row[9]);

        String typeColour = "found";
        if (type.equals("LOST")) {
            typeColour = "lost";
        }
        String statusColour = "resolved";
        if (status.equals("OPEN")) {
            statusColour = "open";
        }

        String html = "<tr><td><div class=\"item-heading\">";
        html += "<span class=\"badge " + typeColour + "\">" + type + "</span>";
        html += "<strong>" + itemName + "</strong></div>";
        html += "<small>#" + reportId + " &middot; " + description + "</small></td>";
        html += "<td>" + studentName + "<small>" + contact + "</small></td>";
        html += "<td>" + category + "<small>" + location + "</small></td>";
        html += "<td class=\"date\">" + date + "</td>";
        html += "<td><span class=\"badge " + statusColour + "\">" + status + "</span></td>";
        html += "<td>" + reportButtons(reportId, type, status) + "</td></tr>";
        return html;
    }

    // Open items may be corrected. A completed item has no edit/delete buttons.
    private String reportButtons(String id, String type, String status) {
        if (!status.equals("OPEN")) {
            return "<span class=\"subtle\">Completed</span>";
        }
        String html = "<div class=\"actions\">";
        html += "<a href=\"/edit?id=" + escape(id) + "\">Edit</a>";
        html += "<a class=\"danger-link\" href=\"/delete?id=" + escape(id) + "\">Delete</a>";
        if (type.equals("FOUND")) {
            html += "<a class=\"action-main\" href=\"/return?id=" + escape(id) + "\">Return item &rarr;</a>";
        } else {
            html += startForm("/reports/resolve");
            html += hiddenField("id", id);
            html += "<button class=\"link-button\" type=\"submit\">Resolve lost &rarr;</button></form>";
        }
        return html + "</div>";
    }

    // STUDENTS PAGE: one form adds a student; the table shows existing students.
    public String students(String notice) throws SQLException {
        String[][] students = database.getStudents();
        String content = heading("Students", "Add a student before creating their first report.");
        content += "<div class=\"two-columns\"><section class=\"panel form-panel\"><h2>Add a student</h2>";
        content += startForm("/students/add");
        content += numberField("id", "Student ID", nextId(students), "Use a unique positive number.");
        content += textField("name", "Student name", 60, "e.g. Ravi Kumar");
        content += textField("contact", "Contact (email or phone)", 80, "e.g. ravi@example.com");
        content += """
                <button class="primary" type="submit">Save student</button></form></section>
                <section class="panel"><div class="section-title"><h2>Student register</h2>
                """;
        content += "<span class=\"subtle\">" + students.length + " students</span></div>";
        content += table(new String[]{"ID", "Name", "Contact"}, students);
        content += "</section></div>";
        return page("Students", "/students", content, notice);
    }

    // NEW ITEM PAGE: the same form works for LOST and FOUND, with different labels.
    public String newReport(String type) throws SQLException {
        if (!type.equals("LOST") && !type.equals("FOUND")) {
            throw new IllegalArgumentException("Choose LOST or FOUND.");
        }

        String title;
        String explanation;
        String personLabel;
        String placeLabel;
        String dateLabel;
        String colour;
        if (type.equals("LOST")) {
            title = "Report a lost item";
            explanation = "Tell us what went missing and where it was last seen.";
            personLabel = "Reported by (owner)";
            placeLabel = "Last seen at";
            dateLabel = "Date lost";
            colour = "lost";
        } else {
            title = "Report a found item";
            explanation = "Record what you found and where you found it.";
            personLabel = "Reported by (finder)";
            placeLabel = "Found at";
            dateLabel = "Date found";
            colour = "found";
        }

        String content = heading(title, explanation);
        String[][] students = database.getStudents();
        if (students.length == 0) {
            content += """
                    <section class="panel form-panel"><p>Add a student first.</p>
                        <a class="button primary" href="/students">Add a student</a>
                    </section>
                    """;
            return page("New report", "/", content, "");
        }

        content += "<section class=\"panel form-panel narrow\"><span class=\"badge " + colour + "\">" + type + " REPORT</span>";
        content += startForm("/reports/add");
        content += hiddenField("type", type);
        content += "<div class=\"field-grid\">";
        content += numberField("id", "Report ID", nextId(database.getReports(false, "")), "Suggested ID; you can change it.");
        content += dropdown("student", personLabel, students);
        content += "</div>";
        content += textField("name", "Item name", 80, "e.g. Blue notebook");
        content += descriptionField("Description", "", "Colour, brand or other details that identify the item.");
        content += "<div class=\"field-grid\">";
        content += dropdown("category", "Category", database.getCategories());
        content += dropdown("location", placeLabel, database.getLocations());
        content += "</div>";
        content += dateField(dateLabel);
        content += "<div class=\"form-actions\"><button class=\"primary\" type=\"submit\">Save " + type.toLowerCase() + " report</button>";
        content += "<a href=\"/\">Cancel</a></div></form>";
        content += "<p class=\"hint\">New reports start as OPEN. Changes are saved to MySQL.</p></section>";
        return page("New " + type.toLowerCase() + " report", "/", content, "");
    }

    // RETURN PAGE: show only OPEN FOUND reports in the item dropdown.
    public String returnItem(String selectedReportId) throws SQLException {
        String[][] reports = database.getReports(true, "");
        String choices = """
                <label for="report">Found item</label><select id="report" name="report" required>
                    <option value="">Choose an open FOUND report</option>
                """;
        int foundCount = 0;
        for (String[] report : reports) {
            String reportId = report[0];
            String type = report[1];
            String itemName = report[3];
            String finder = report[4];

            if (type.equals("FOUND")) {
                foundCount++;
                String selected = "";
                if (reportId.equals(selectedReportId)) {
                    selected = " selected";
                }
                choices += "<option value=\"" + escape(reportId) + "\"" + selected + ">#";
                choices += escape(reportId) + " - " + escape(itemName) + " (found by " + escape(finder) + ")</option>";
            }
        }
        choices += "</select>";

        String content = heading("Return an item", "Record a handover after checking who is collecting the item.");
        if (foundCount == 0) {
            content += """
                    <section class="panel form-panel"><h2>No open found items</h2>
                        <p>There is currently nothing to return.</p><a href="/">Back to reports</a>
                    </section>
                    """;
            return page("Return an item", "/return", content, "");
        }

        content += "<section class=\"panel form-panel narrow\"><h2>Handover details</h2>";
        content += startForm("/returns/add");
        content += numberField("id", "Return ID", nextId(database.getReturns()), "Use a unique number for this handover.");
        content += choices;
        content += dropdown("collector", "Collected by", database.getStudents());
        content += dateField("Return date");
        content += """
                <div class="info">Saving records the return and marks this FOUND report as RESOLVED.
                    If a matching LOST report exists, resolve it separately from Reports.</div>
                <div class="form-actions"><button class="primary" type="submit">Save return</button>
                    <a href="/">Cancel</a></div></form></section>
                """;
        return page("Return an item", "/return", content, "");
    }

    // HISTORY PAGE: this table only displays completed handovers.
    public String history(String notice) throws SQLException {
        String content = heading("Return history", "A record of items that have been handed back.");
        content += "<section class=\"panel\">";
        String[] columns = {"Return ID", "Report ID", "Item", "Found by", "Collected by", "Return date"};
        content += table(columns, database.getReturns());
        content += "<p class=\"panel-note\">Completed handovers are kept as read-only records.</p></section>";
        return page("Return history", "/history", content, notice);
    }

    // LISTS PAGE: categories and locations are preset lookup tables.
    public String lists() throws SQLException {
        String content = heading("Categories & places", "The preset choices available when reporting an item.");
        content += "<div class=\"two-columns equal\"><section class=\"panel\"><div class=\"section-title\"><h2>Categories</h2></div>";
        content += table(new String[]{"Category ID", "Name"}, database.getCategories());
        content += "</section><section class=\"panel\"><div class=\"section-title\"><h2>Places</h2></div>";
        content += table(new String[]{"Location ID", "Name"}, database.getLocations());
        content += "</section></div>";
        return page("Categories & places", "/lists", content, "");
    }

    // EDIT/DELETE PAGE: first check that the chosen report exists and is OPEN.
    public String changeReport(int id, boolean delete) throws SQLException {
        String[][] reports = database.getReport(id);
        if (reports.length != 1 || !reports[0][2].equals("OPEN")) {
            throw new IllegalArgumentException("Choose an existing OPEN report.");
        }
        String[] report = reports[0];
        String type = report[1];
        String itemName = report[3];
        String studentName = report[4];
        String location = report[7];
        String description = report[9];

        String title = "Edit a description";
        String tabTitle = "Edit report";
        String formAddress = "/reports/edit";
        String buttonText = "Save description";
        String buttonColour = "primary";
        if (delete) {
            title = "Delete an incorrect report";
            tabTitle = "Delete report";
            formAddress = "/reports/delete";
            buttonText = "Delete report";
            buttonColour = "danger";
        }

        String content = heading(title, "Report #" + id + " - " + itemName);
        content += "<section class=\"panel form-panel narrow\"><div class=\"info\"><strong>";
        content += escape(type) + " &middot; OPEN</strong><br>Reported by ";
        content += escape(studentName) + " at " + escape(location) + "<br>" + escape(description) + "</div>";
        content += startForm(formAddress);
        content += hiddenField("id", String.valueOf(id));

        if (delete) {
            content += """
                    <p>This removes the report from the database. Use it only for an incorrect entry.</p>
                    <label class="check"><input type="checkbox" name="confirm" value="YES" required>
                    """;
            content += " I want to delete report #" + id + ".</label>";
        } else {
            content += descriptionField("Corrected description", description, "Update the identifying details.");
        }
        content += "<div class=\"form-actions\"><button class=\"" + buttonColour + "\" type=\"submit\">";
        content += buttonText + "</button><a href=\"/\">Cancel</a></div></form></section>";
        return page(tabTitle, "/", content, "");
    }

    // A failed action gets a clear explanation and a route back to the project.
    public String error(String message) {
        String content = """
                <section class="panel form-panel narrow error"><p class="eyebrow">Please check</p>
                    <h1>We couldn't finish that.</h1><p>%s</p>
                    <p class="hint">Use your browser's Back button to review the form, or return to the reports.</p>
                    <a class="button secondary" href="/">Back to reports</a>
                </section>
                """.formatted(escape(message));
        return page("Please check", "", content, "");
    }

    // REUSABLE HTML HELPERS: each method below makes one small repeated element.

    // Reuse one heading layout so page titles have the same spacing.
    private String heading(String title, String description) {
        return "<div class=\"page-heading\"><h1>" + escape(title) + "</h1><p>" + escape(description) + "</p></div>";
    }

    // POST submits a change. The hidden token lets Website check it came from our form.
    private String startForm(String address) {
        return "<form method=\"post\" action=\"" + address + "\">" + hiddenField("token", formToken);
    }

    // Hidden fields carry values such as the report ID without showing an editable box.
    private String hiddenField(String name, String value) {
        return "<input type=\"hidden\" name=\"" + name + "\" value=\"" + escape(value) + "\">";
    }

    // Suggest the largest existing ID plus one. MySQL still checks uniqueness.
    private int nextId(String[][] rows) {
        int biggest = 0;
        for (String[] row : rows) {
            int id = Integer.parseInt(row[0]);
            if (id > biggest) {
                biggest = id;
            }
        }
        if (biggest == Integer.MAX_VALUE) {
            return 1;
        }
        return biggest + 1;
    }

    // A labelled number box for an ID. These are convenience checks, also checked in Java.
    private String numberField(String name, String label, int value, String hint) {
        return """
                <div><label for="%s">%s</label>
                    <input type="number" id="%s" name="%s" min="1" max="2147483647" value="%s" required>
                    <p class="hint">%s</p>
                </div>
                """.formatted(name, label, name, name, value, hint);
    }

    // A labelled text box. Its name is the key used by FormInput.get(...).
    private String textField(String name, String label, int maximumLength, String placeholder) {
        return """
                <label for="%s">%s</label>
                <input id="%s" name="%s" maxlength="%s" placeholder="%s" required>
                """.formatted(name, label, name, name, maximumLength, placeholder);
    }

    // Descriptions use a bigger text area instead of a single-line input.
    private String descriptionField(String label, String value, String hint) {
        return """
                <label for="description">%s</label>
                <textarea id="description" name="description" maxlength="250" rows="3" required>%s</textarea>
                <p class="hint">%s</p>
                """.formatted(label, escape(value), hint);
    }

    // Start dates at today, and let the browser offer its usual calendar control.
    private String dateField(String label) {
        String today = LocalDate.now().toString();
        return """
                <label for="date">%s</label>
                <input id="date" name="date" type="date" value="%s" max="%s" required>
                """.formatted(label, today, today);
    }

    // The displayed name helps the user; the submitted value is its database ID.
    private String dropdown(String name, String label, String[][] rows) {
        String html = "<div><label for=\"" + name + "\">" + label + "</label>";
        html += "<select id=\"" + name + "\" name=\"" + name + "\" required>";
        html += "<option value=\"\">Choose " + label.toLowerCase() + "</option>";
        for (String[] row : rows) {
            String id = escape(row[0]);
            String displayName = escape(row[1]);
            html += "<option value=\"" + id + "\">" + displayName + " (#" + id + ")</option>";
        }
        html += "</select></div>";
        return html;
    }

    // Nested loops: one loop for rows, another for the cells in each row.
    private String table(String[] headings, String[][] rows) {
        if (rows.length == 0) {
            return "<div class=\"empty\"><p>No records yet.</p></div>";
        }
        String html = "<div class=\"table-scroll\"><table><thead><tr>";
        for (String heading : headings) {
            html += "<th>" + escape(heading) + "</th>";
        }
        html += "</tr></thead><tbody>";
        for (String[] row : rows) {
            html += "<tr>";
            for (String cell : row) {
                html += "<td>" + escape(cell) + "</td>";
            }
            html += "</tr>";
        }
        html += "</tbody></table></div>";
        return html;
    }
}
