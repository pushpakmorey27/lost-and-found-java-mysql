# Local setup

This guide starts with an extracted repository folder. Download it from GitHub with **Code → Download ZIP**, then choose **Extract All**. Do not run files while they are still inside the ZIP.

## 1. Install the two requirements

Skip a program if it is already installed.

| Requirement | Official download | Choose |
| --- | --- | --- |
| Java JDK | [Eclipse Temurin releases](https://adoptium.net/temurin/releases/) | JDK 21 or newer; your operating system and architecture. For a normal Windows laptop, choose Windows x64 MSI. Enable adding Java to PATH if offered. |
| MySQL Server | [MySQL Community Server 8.4](https://dev.mysql.com/downloads/mysql/8.4.html) | For Windows x64, choose the MSI installer. Install the server, then finish its configuration. |

For MySQL's local configuration, keep port **3306**, configure it as a Windows service and remember the **root password you choose**. You do not need the Sakila or World sample databases. MySQL Workbench is an optional SQL editor; installing Workbench alone does not install the database server. See [MySQL's Windows installation instructions](https://dev.mysql.com/doc/refman/8.4/en/windows-installation.html) if installation itself fails.

After installing Java, close and reopen your terminal. Check:

```text
java -version
javac -version
```

Both commands should print version 21 or newer. The JDK includes `javac`; a JRE alone is insufficient.

## 2. Create the database once

Start MySQL first. On Windows, the **Services** application should show your MySQL service as **Running**.

In the extracted project folder, double-click **`1-Setup-Database.cmd`** and enter the MySQL **root password from installation**. Password typing may show nothing at all; type it and press Enter once.

The setup imports `database/setup_mysql.sql`. It creates a new database named `lost_and_found`, including:

- 5 tables, 2 views, 2 procedures and 6 triggers.
- Fictional seed data: 4 students, 4 categories, 4 locations, 6 reports and 1 return.

**Run setup once.** A message that the database already exists means you should inspect the existing database before doing anything else. This script does not replace it. Do not delete a database containing work you need to keep.

### Alternative: import in MySQL Workbench

1. Connect to your local server as `root`.
2. Choose **File → Open SQL Script** and open `database/setup_mysql.sql` from the project.
3. Clear any text selection and run the **whole script** using **Query → Execute (All or Selection)**. Keep **Stop Script Execution On Errors** enabled.
4. Refresh the Schemas list and expand `lost_and_found`.

The script contains `DELIMITER` commands for procedures and triggers. Use a script-capable MySQL client or Workbench's full-script execution; do not paste isolated fragments into an unrelated single-statement runner.

## 3. Create a database account for the application

Use root only for administration. In a SQL editor connected as root, run these two statements **after replacing `CHOOSE_YOUR_OWN_PASSWORD` with a password you choose**:

```sql
CREATE USER 'lf_user'@'localhost'
IDENTIFIED BY 'CHOOSE_YOUR_OWN_PASSWORD';

GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE
ON lost_and_found.* TO 'lf_user'@'localhost';
```

The capitalized text is a placeholder, not the intended password. Keep your chosen password private. If `lf_user` already exists, keep using its existing password, or deliberately change it as root with:

```sql
ALTER USER 'lf_user'@'localhost'
IDENTIFIED BY 'CHOOSE_YOUR_OWN_PASSWORD';
```

Do not run `ALTER USER` unless you intend to change that account's password. The application account can work with project data but cannot create/drop tables or manage users.

If using the MySQL command line instead of Workbench, open the client first:

```text
mysql -u root -p
```

Enter the root password, then paste the two account-creation statements. If Windows cannot find `mysql`, use its usual full path in Command Prompt:

```bat
"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -p
```

Change the path only if you installed MySQL elsewhere.

## 4. Start the website

Double-click **`run-website.cmd`**. On the first successful launch, answer:

| Prompt | What to enter |
| --- | --- |
| `JDBC URL (blank = local database):` | Press Enter. |
| `Database username (blank = lf_user):` | Press Enter. |
| Password | Enter the password you chose for `lf_user`, then press Enter. |

The default URL is `jdbc:mysql://127.0.0.1:3306/lost_and_found`. The default username is `lf_user`.

Wait for **`LOST AND FOUND WEBSITE IS READY`**, then open **http://127.0.0.1:8080/**. The browser may open automatically. Keep the Java window open. Press **Ctrl+C** in it to stop the website.

The MySQL port is **3306**; the website port is **8080**. They are different services.

### Subsequent launches

1. Make sure MySQL is running.
2. Run `run-website.cmd`.
3. Open http://127.0.0.1:8080/.

Do not import the database again. Successful changes remain in MySQL after Java closes.

## 5. Try a complete workflow

The sample data already contains some resolved records. Use new IDs for your own demonstration:

1. Open Students and add student **10**, name **Demo Student**, contact **demo@example.com**.
2. Create FOUND report **110**, finder **Asha**, category **Keys**, location **Library**, today's date and a clear description.
3. Find that report and record its handover with return ID **510** and collector **10**.
4. Check Return History. Report **110** should be RESOLVED.
5. If demonstrating a LOST report too, create a separate one and resolve it only after confirming recovery.

Do not reuse these IDs if you already performed this demonstration. Choose different unused positive numbers.

To inspect the database directly, run `database/ViewDatabase.sql` or:

```sql
USE lost_and_found;
SHOW FULL TABLES;
SELECT * FROM lf_report_details;
SELECT * FROM lf_return_details;
```

## Saved login

On Windows, the application normally saves the login in:

```text
%LOCALAPPDATA%\LostAndFound\database.properties
```

Open File Explorer, paste `%LOCALAPPDATA%\LostAndFound` in its address bar and press Enter. The settings file contains a plain-text password; do not upload or share it.

To switch accounts or correct a saved password, stop Java and delete **only `database.properties`** from that folder. Start the website again and enter the correct details. This does not delete database records.

On Linux/macOS, the normal settings file is `$HOME/LostAndFound/database.properties`, unless `LOCALAPPDATA` is set. Developers can choose a separate settings file with the JVM property `lostfound.settings`.

## Linux and macOS

Install JDK 21+ and MySQL 8.4 using your operating system's supported installation method. Start MySQL, then open a terminal in the repository root:

```sh
mysql -u root -p < database/setup_mysql.sql
```

Create `lf_user` using the SQL in step 3, then start the application:

```sh
bash launch.sh
```

Enter the same first-launch answers as Windows. The website address is still http://127.0.0.1:8080/. Use `bash test.sh` for the no-database checks.

## Common problems

| Message or symptom | What to check |
| --- | --- |
| `javac` is not recognized | Install a JDK, enable its PATH option and reopen the terminal. Check `javac -version`. |
| `Access denied` | Use the correct database username and that account's password. The root and `lf_user` passwords may differ. Reset the saved login if necessary. |
| Password typing appears blank | This is normal for a terminal password prompt. Type once and press Enter. |
| `Unknown database 'lost_and_found'` | Complete step 2 against the same server and port. |
| Connection refused / communications failure | Start the MySQL service and check port 3306. |
| Website port already in use | An existing copy may already be running. Try the website address or stop the previous Java window before starting again. |
| Page cannot be reached | Wait for the ready message and keep the Java window open. Use `http`, not `https`. |
| Driver missing | Keep the Connector/J `.jar` in `lib/` and run the supplied launcher from the extracted folder. |
| Duplicate ID | Choose an unused positive ID. Existing data is preserved between launches. |
| A resolved record cannot be edited | Completed records are intentionally protected. Use an OPEN report to demonstrate corrections. |

## Opening the source in VS Code

Open the **whole extracted project folder**, then open `src/start/Main.java`. That file starts the website. `src/database/Database.java` contains the JDBC connection and SQL calls. See the [interview notes](INTERVIEW-NOTES.md) for the other classes.
