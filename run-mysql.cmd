@echo off
setlocal
cd /d "%~dp0"
if not exist bin mkdir bin
rem Compile every source folder so both website and console share the same classes.
javac -encoding UTF-8 -d bin src\start\*.java src\items\*.java src\database\*.java src\website\*.java src\console\*.java
if errorlevel 1 (
    echo Compilation failed. Check that the Java JDK is installed.
    pause
    exit /b 1
)
java -cp "bin;lib\*" ConsoleMain --mysql
pause
