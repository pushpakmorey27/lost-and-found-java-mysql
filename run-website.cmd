@echo off
setlocal
cd /d "%~dp0"
if not exist bin mkdir bin
rem Compile the Java files from the five labelled folders into bin.
javac -encoding UTF-8 -d bin src\start\*.java src\items\*.java src\database\*.java src\website\*.java src\console\*.java
if errorlevel 1 (
    echo Compilation failed. Check that the Java JDK is installed.
    pause
    exit /b 1
)
rem Main starts the website. The database login is reused automatically.
java -cp "bin;lib\*" Main
pause
