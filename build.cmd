@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

title Screen Sharing - BUILD

rem ---- Detect JAVA_HOME if not set ----------------------------------
if defined JAVA_HOME goto :java_ok
if exist "D:\Java\bin\javac.exe" set "JAVA_HOME=D:\Java" & goto :java_ok
if exist "%ProgramFiles%\Java\jdk-21\bin\javac.exe" set "JAVA_HOME=%ProgramFiles%\Java\jdk-21" & goto :java_ok
if exist "%ProgramFiles%\Java\jdk-17\bin\javac.exe" set "JAVA_HOME=%ProgramFiles%\Java\jdk-17" & goto :java_ok
for /d %%d in ("%ProgramFiles%\Java\jdk*") do if exist "%%d\bin\javac.exe" set "JAVA_HOME=%%d" & goto :java_ok
for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk*") do if exist "%%d\bin\javac.exe" set "JAVA_HOME=%%d" & goto :java_ok

echo Khong tim thay JDK. Hay cai Java JDK (17 hoac 21) hoac dat bien JAVA_HOME.
pause
exit /b 1

:java_ok
echo JAVA_HOME = %JAVA_HOME%
echo Build project, xin cho giay lat...
call "%~dp0mvnw.cmd" -q clean package -pl client,host -DskipTests
if errorlevel 1 (
    echo Build that bai!
    pause
    exit /b 1
)
echo.
echo Build thanh cong. JAR da san sang:
echo   .bin\host.jar   (server)
echo   .bin\client.jar (client)
echo.
exit /b 0