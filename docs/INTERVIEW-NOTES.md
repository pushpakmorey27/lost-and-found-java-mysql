# Explaining this project in an interview

Use these notes to understand the implementation and practice explaining it in your own words. Describe only work you understand and features that exist. Avoid claims about production users, scale or performance that you have not measured.

## A 30-second introduction

> I built a local campus Lost and Found application with Java, JDBC and MySQL. It lets a user register students, report lost or found items, search reports and record a confirmed return. I kept the implementation small enough to trace from an HTML form to Java and SQL. The most useful database feature is an atomic handover: recording the return and resolving its found report either succeed together or roll back.

Be ready to replace “I built” with an accurate explanation of your contribution if you worked with a teammate or used external assistance. Credit and technical understanding are more useful than invented experience.

## A short demonstration

1. Show an OPEN found report and its description.
2. Record its return to an existing student.
3. Show the return history and the report's RESOLVED status.
4. Show `Database.recordReturn()` and the `lf_record_return` SQL procedure.
5. Explain what happens if one statement fails, then show the relevant automated test.

Use fictional demonstration records and unused IDs. Do not expose your saved database password while sharing your screen.

## Where the important code is

Line numbers can change. Search for these class and method names in VS Code with **Ctrl+Shift+F**, or open a file with **Ctrl+P**.

| Concept | Where to look | How to explain it |
| --- | --- | --- |
| Entry point | `src/start/Main.java`: `main` | Creates the database and website objects, registers the handler and starts the server. |
| JDBC connection | `src/database/Database.java`: `DriverManager.getConnection` | Opens the Java-to-MySQL connection using the URL and login. |
| Parameterized SQL | `Database.addReport` | Uses placeholders and typed bindings instead of putting user text into SQL strings. |
| Stored procedure call | `Database.recordReturn` | A `CallableStatement` calls `lf_record_return`. |
| Inheritance | `src/items/LostItem.java`, `FoundItem.java` | Both extend the abstract `Item` class. |
| Overriding | Both child classes: `getReportType` | Each implements the same parent method with LOST or FOUND. |
| Runtime polymorphism | `Website.addItem` and `Database.addReport` | An `Item` reference holds either child; calling `getReportType()` runs that child's implementation. |
| Encapsulation | `src/items/Student.java` | Private fields hide state; public getters provide access. |
| Constructor, `this`, `super` | `Student` constructor; `LostItem` constructor | `this` distinguishes object fields from parameters; `super` calls the parent constructor. Constructors are not inherited. |
| Abstraction | `Item` and its abstract `getReportType` | Defines common data and an operation whose specific result comes from a child. |
| Interface | `Actions`, `Database`, `PracticeData` | The optional console uses a common contract for persistent or temporary data. The website uses `Database` directly. |
| Overloading | `KeyboardInput.readNumber` | The optional console has one version taking a prompt and another taking a prompt plus minimum/maximum. |
| Exceptions | `FormInput.number`, `Website.handle` | Invalid data becomes a readable error; failed database actions are rolled back. |
| File I/O | `Database` settings load/save | Reads and writes saved connection settings outside the repository. |
| Arrays | `PracticeData`; `Database.readRows` | Practice mode stores temporary records in arrays; database results are also represented as arrays for rendering. |

The project has **14 classes and one project-defined interface**. One of the classes, `Item`, is abstract. The built-in `HttpHandler` interface comes from Java, not from this project's own source files.

## Questions worth practicing

### Why JDBC rather than an ORM?

> The project was intended to demonstrate SQL and database concepts directly. JDBC makes the query, bound values and transaction boundaries visible. For a larger application I would compare that control with the reduced mapping work an ORM could offer.

### What exactly is atomic here?

> The return record and the FOUND report's RESOLVED status belong to one handover. The procedure starts a transaction and commits after both statements. Its exception handler rolls back and rethrows the error if either fails. I call the procedure without unrelated pending changes because it owns the transaction.

Do not say that every possible database modification is automatically safe. A direct SQL writer can bypass the supported procedure workflow.

### Why use views?

> The pages need readable joined details such as the category name and collector's name. Views save those SELECT definitions so I do not repeat the joins. They are not a second stored copy of the rows or an automatic speed improvement.

### Why use triggers as well as Java validation?

> Java catches errors near the form and produces useful messages. Foreign keys, constraints and triggers also check rules at the database boundary, including when data is written from another client. Neither layer can verify physical ownership of an item.

### Why is resolving a lost report manual?

> A similar description is only a possible match. Two people can lose similar umbrellas. A person confirms recovery before resolving the lost report. Recording a return automatically resolves its particular FOUND report, but the matching LOST report is separate.

### How do you prevent SQL injection?

> User values are bound as parameters in prepared or callable statements rather than concatenated into SQL. That separates data from SQL syntax. It does not replace input validation or authorization.

### Can multiple people use it at once?

> It is designed as a local demonstration. Requests use one JDBC connection and the handler is synchronized so operations do not interleave on it. That limits throughput. A multi-user deployment would need a connection pool and request-level transactions, plus concurrency tests.

### What security limitations remain?

> It has no application login or roles, and its remembered database password is plain text outside the repository. It binds to loopback and uses form safeguards, but those do not make it a public website. I would add authentication, authorization, safe secret storage and deployment controls before hosting it.

### Why are IDs entered manually?

> It makes primary keys and foreign-key references visible for a college demonstration. Auto-generated IDs would be a better user experience because users would not need to pick unused numbers.

### What do the tests prove?

Read `tests/README.md`, run the checks and explain their actual assertions. Distinguish tests of Java input/model behavior from integration tests that need MySQL. A passing test run is evidence for those cases, not proof that every possible input and deployment is correct.

### What would you improve first?

> I would generate IDs, introduce authentication and explicitly link confirmed matches. For more users I would add a connection pool and test concurrent returns. I would choose changes based on a real requirement rather than adding tools only to increase the stack size.

## A small example to understand

```java
Item item = new FoundItem(/* report details */);
String type = item.getReportType(); // "FOUND"
```

This is an explanatory excerpt, not a complete constructor call to paste into the program. The variable's declared type is `Item`; the actual object's class is `FoundItem`. That object's overridden method runs. Overloading is different: a compiler chooses between methods with different parameter lists.

## A truthful résumé bullet

> Built a local campus Lost and Found application using Java, JDBC and MySQL, with searchable reports, database validation and transactional item returns.

Add testing or CI details only after you have run them and can explain them. Use measured numbers only if you actually measured them.
