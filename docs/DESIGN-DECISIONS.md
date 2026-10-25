# Design decisions and limits

These decisions explain the current implementation. They are small-project tradeoffs, not claims that the application is ready for a public campus deployment.

## Keep the Java application small

**Choice:** Java's built-in `HttpServer`, server-generated HTML, plain JDBC and a small number of classes.

**Reason:** The project began as an OOP and RDBMS mini project. A student should be able to trace a form from the browser to a Java method and then to the SQL statement that saves it.

**Cost:** Routing and HTML generation are written by hand. A larger application would benefit from packages, templates, structured configuration and a framework after the underlying concepts are understood.

## Confirm matches manually

**Choice:** A person compares reports and confirms recovery. Recording a return resolves its FOUND report; a matching LOST report is resolved separately.

**Reason:** Similar names and descriptions do not prove ownership. Two students can lose identical black umbrellas. Automatically closing both lost reports would be incorrect.

**Cost:** The user completes an extra confirmation step. Suggested matches could later use category, location and date, but final ownership verification would still need evidence. The current database does not link lost and found reports.

## Keep a handover atomic

**Choice:** Call `lf_record_return` instead of performing a standalone return insertion from Java.

**Reason:** Saving the return and resolving its FOUND report describe one operation. A half-completed operation would mislead the next user.

The procedure owns its transaction. Its `SQLEXCEPTION` handler rolls back and rethrows the error. Successful changes are committed together. Ordinary Java writes commit on success; the website and console roll back failed actions.

**Boundary:** Use the supported procedure for handovers. A database account with direct table-write permissions can bypass the application's sequence. The current grants simplify a local demo; they do not make all arbitrary administrative SQL safe. Procedure-only write access and tighter ownership rules would be appropriate for a hosted version.

## Put rules in more than one place

**Choice:** Validate forms in Java, and use keys, constraints and triggers in MySQL.

**Reason:** Java validation gives a useful message before a bad request reaches storage. Database rules still apply when the application is not the writer.

Examples include existing student/category/location references, allowed report types, a unique return for each found report, date checks and protection against editing completed returns.

**Boundary:** These rules check record consistency. They cannot determine whether someone is the rightful owner of a physical item. An authentication and authorization design is also outside the current scope.

## Reuse joined queries through views

**Choice:** `lf_report_details` and `lf_return_details` provide readable joined results.

**Reason:** Pages need names such as the category and collector, rather than only numeric foreign keys. Saving the join as a view reduces repeated SQL and keeps those queries easy to inspect.

**Cost:** A normal view is not a cached result or an automatic performance optimization. Performance depends on its underlying query and indexes.

## Use one connection and serialize requests

**Choice:** The application creates one JDBC `Connection`. Its HTTP handler is synchronized.

**Reason:** Requests must not accidentally share an in-progress transaction on that connection. Serial handling is easy to reason about for a local demonstration.

**Cost:** A slow request holds up other requests. This is not a design for many simultaneous users. A hosted version should use a connection pool, one connection per operation and carefully defined transaction boundaries.

## Use a local account and remember the login

**Choice:** The application can remember database credentials outside the repository so later launches are simple. The setup guide recommends a dedicated `lf_user` with access to this database rather than the administrator account.

**Cost:** The saved password is plain text. File privacy matters. The account has no permission to create/drop tables or manage database users, but it can modify project records. An operating-system credential store or deployment secret manager would improve this for a wider audience.

The MySQL connection requests TLS with `sslMode=REQUIRED`. This requires encryption, but it does not verify the server certificate's identity. That choice is limited to the local demonstration setup.

## Keep the website local

**Choice:** Bind to loopback, check the Host header, accept state changes through POST, check a per-run form token, limit form size and escape HTML output.

**Reason:** These are modest safeguards around a local form application.

**Boundary:** A form token is not a login or permission check. Any user with access to the local application can perform its actions. Do not expose this application to the public internet. Public use would need authentication, roles, CSRF/session design, secure credential handling, HTTPS and operational monitoring.

## Keep IDs explicit

**Choice:** The user enters positive student, report and return IDs.

**Reason:** This makes primary keys and relationships visible during a college demonstration.

**Cost:** Users can choose an existing ID and receive a duplicate-ID error. Auto-generated IDs would reduce that friction in a real deployment.

## Improvements worth doing next

1. Generate IDs and add an explicit link between verified lost/found matches.
2. Introduce authentication and role-based permissions before serving multiple users.
3. Replace the shared connection with a pool and test concurrent handovers.
4. Replace plain-text saved credentials with safer configuration.
5. Add pagination and measure query plans before choosing further indexes.

These are future changes. They are not features of the current project.
