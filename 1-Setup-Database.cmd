@echo off
setlocal
cd /d "%~dp0"
if not defined LF_MYSQL_PORT set "LF_MYSQL_PORT=3306"
if not defined LF_MYSQL_CLIENT set "LF_MYSQL_CLIENT=%ProgramFiles%\MySQL\MySQL Server 8.4\bin\mysql.exe"
if not exist "%LF_MYSQL_CLIENT%" (
    for /f "delims=" %%M in ('where mysql.exe 2^>nul') do set "LF_MYSQL_CLIENT=%%M"
)
if not exist "%LF_MYSQL_CLIENT%" (
    echo MySQL command-line client was not found.
    echo Install MySQL Server 8.4, or open database\setup_mysql.sql in MySQL Workbench.
    echo Execute the whole SQL file once, connected as your local root user.
    pause
    exit /b 1
)
echo LOST AND FOUND - ONE-TIME DATABASE IMPORT
echo Start MySQL Server before continuing.
echo This creates lost_and_found with the fictional demonstration records.
echo If lost_and_found already exists, setup stops without resetting it.
echo Enter YOUR OWN MySQL root password. Password typing is invisible.
echo.
"%LF_MYSQL_CLIENT%" --no-defaults --protocol=TCP --host=127.0.0.1 --port=%LF_MYSQL_PORT% --user=root --password --ssl-mode=REQUIRED --default-character-set=utf8mb4 --table < "database\setup_mysql.sql"
if errorlevel 1 (
    echo.
    echo IMPORT STOPPED. Read the MySQL ERROR above.
    echo If the database already exists from a successful import, use run-website.cmd.
    pause
    exit /b 1
)
echo.
echo DATABASE IMPORT COMPLETE. Next, double-click run-website.cmd.
echo Do not run this setup every time. Your normal website launcher is run-website.cmd.
pause
exit /b 0
