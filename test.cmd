@echo off
setlocal
cd /d "%~dp0"
if "%~1"=="" goto compile
if not "%~1"=="--mysql" goto usage
if not "%~2"=="--reset-test-database" goto usage
if not "%~3"=="" goto usage

:compile
where javac >nul 2>&1
if errorlevel 1 (
    echo Java JDK 21 or newer is required. Install a JDK and reopen the terminal.
    exit /b 1
)
if not exist "build\tests" mkdir "build\tests"
javac --release 21 -encoding UTF-8 -cp "lib\*" -d "build\tests" src\start\*.java src\items\*.java src\database\*.java src\console\*.java src\website\*.java tests\*.java
if errorlevel 1 exit /b 1
java -cp "build\tests;lib\*" CoreChecks
if errorlevel 1 exit /b 1
if "%~1"=="" exit /b 0
java -cp "build\tests;lib\*" IntegrationChecks --reset-test-database
exit /b %errorlevel%

:usage
echo Usage: test.cmd
echo        test.cmd --mysql --reset-test-database
echo The optional MySQL checks delete and recreate ONLY lost_and_found_test.
echo Set LF_TEST_MYSQL_URL, LF_TEST_MYSQL_USER and LF_TEST_MYSQL_PASSWORD first.
exit /b 2
