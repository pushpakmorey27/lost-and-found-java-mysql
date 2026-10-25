# Running the checks

The tests use plain Java, so there is no Maven, JUnit or extra test dependency to install. Use **JDK 21 or newer**. Run commands from the project folder.

## Everyday checks: no database needed

Windows:

```bat
test.cmd
```

Linux or macOS:

```sh
bash test.sh
```

These run 27 checks against the actual `FormInput`, `WebPages`, `LostItem` and `FoundItem` classes: decoded form values, required text and length limits, positive IDs, invalid and future dates, HTML escaping, and child methods called through `Item` references. A failure stops the command with a nonzero exit code.

## Optional checks: real MySQL

These require a **local MySQL 8.4+ test server** and an account allowed to create/drop the disposable schema, tables, triggers and procedures. They delete and recreate **only `lost_and_found_test`**, including anything previously in that test schema. They never reset the normal `lost_and_found` application database. The confirmation flag is required every time.

In PowerShell, set the connection details for your own test server:

```powershell
$env:LF_TEST_MYSQL_URL = 'jdbc:mysql://127.0.0.1:3306/'
$env:LF_TEST_MYSQL_USER = 'root'
$testCredential = Get-Credential -UserName $env:LF_TEST_MYSQL_USER -Message 'Local MySQL test login'
$env:LF_TEST_MYSQL_PASSWORD = $testCredential.GetNetworkCredential().Password
.\test.cmd --mysql --reset-test-database
```

Enter the test-server password in the credential prompt. It will not appear in command history.

Change `3306` if your isolated test server uses another port. The URL must end with `/` and must not include a database name or query options. Only loopback hosts (`127.0.0.1`, `localhost`, `[::1]`) are accepted.

On Linux or macOS:

```sh
export LF_TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:3306/'
export LF_TEST_MYSQL_USER='root'
read -r -s -p 'Password for local test server: ' LF_TEST_MYSQL_PASSWORD
echo
export LF_TEST_MYSQL_PASSWORD
bash test.sh --mysql --reset-test-database
unset LF_TEST_MYSQL_PASSWORD
```

The Java test imports the actual `database/setup_mysql.sql`, substituting only its database declarations. It uses an encrypted connection and a temporary login file, then removes the schema and login file when finished. Your app's remembered login is not read or changed. If the process is forcibly stopped, the test schema may remain; the next explicitly confirmed run resets it.

The 23 database checks cover:

- The expected schema objects, fictional seed data and readable joined views.
- A successful return through the production Java `Database.recordReturn` method.
- Rejection of a second return for the same found report.
- A deliberately injected failure during the report update, after the return insert: neither change may remain committed.
- Successful retry after that rollback.
- Lost-report resolution, foreign-key rejection and an apostrophe saved through a prepared statement.

Remove the password from the PowerShell session after testing:

```powershell
Remove-Item Env:LF_TEST_MYSQL_PASSWORD
```

GitHub Actions runs the core checks on Windows and Linux with Java 21 and the database checks against a temporary MySQL 8.4 service. It uses a throwaway CI credential, with no repository secrets or real records.
