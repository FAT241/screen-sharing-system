@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

title Screen Sharing - BUILD

rem ---- Detect JAVA_HOME if not set ----------------------------------
if defined JAVA_HOME goto :java_ok
if exist "D:\Java\bin\javac.exe" set "JAVA_HOME=D:\Java" & goto :java_ok
rem Cac thu muc cai dat JDK pho bien tren Windows (Ten thu muc doc dat
rem theo tung distro, vi du: jdk-23, Java-23, openjdk-23, zulu-23, jbr).
for %%P in ("%ProgramFiles%\Java" "%ProgramFiles%\Eclipse Adoptium" "%ProgramFiles%\Microsoft" "%ProgramFiles%\Amazon Corretto" "%ProgramFiles%\Zulu" "%ProgramFiles%\BellSoft" "%ProgramFiles%\Semeru" "%ProgramFiles%\AdoptOpenJDK" "%ProgramFiles%\JetBrains") do call :tryjdk "%%~P"
if defined JAVA_HOME goto :java_ok

echo Khong tim thay JDK 17 tro len trong may.
echo Hay cai Java JDK 21/23/25 va tro bien JAVA_HOME toi thu muc JDK.
echo Vi du: setx JAVA_HOME "C:\Program Files\Java\jdk-23"
echo (buoc sau chi doc bien moi, dung lai cua so moi de no co hieu luc)
pause
exit /b 1

:java_ok
rem bao dam JAVA_HOME tro toi JDK that, khong phai JRE
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo JAVA_HOME khong phai JDK - thieu bin\javac.exe: %JAVA_HOME%
    echo Hay tro JAVA_HOME toi thu muc JDK, khong phai JRE.
    pause
    exit /b 1
)
rem Doc phien ban tu file release cua JDK, tranh escape kho dong lenh phuc tap
set "JDKVER=khong ro"
if exist "%JAVA_HOME%\release" for /f "tokens=2 delims==" %%v in ('findstr /b /c:"JAVA_VERSION=" "%JAVA_HOME%\release" 2^>nul') do set "JDKVER=%%~v"
echo JAVA_HOME = %JAVA_HOME%
echo Java      = %JDKVER%
echo [1/2] Build + cai dat thu vien dung chung (lib)...
call "%~dp0mvnw.cmd" -q clean install -pl lib -DskipTests
if errorlevel 1 (
    echo Build that bai o buoc 1 - lib!
    pause
    exit /b 1
)

echo [2/2] Build host + client...
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

rem ---- Tim mot JDK trong thu muc %~1 -------------------------------
:tryjdk
if exist "%~1\bin\javac.exe" set "JAVA_HOME=%~1" & exit /b 0
for /d %%d in ("%~1\jdk-2*" "%~1\Java-2*" "%~1\openjdk-2*" "%~1\jdk2*" "%~1\corretto-2*" "%~1\zulu2*" "%~1\temurin-2*" "%~1\jdk-1*" "%~1\jdk1*") do if exist "%%~d\bin\javac.exe" set "JAVA_HOME=%%~d" & exit /b 0
exit /b 0