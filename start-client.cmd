@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
chcp 65001 >nul

title Screen Sharing - CLIENT

rem ---- Build neu jar thieu hoac da cu so voi source -----------------------
set "JAR=.bin\client.jar"
set "NEEDBUILD=1"
set "CHECK=NONE"

if exist "%JAR%" (
    for /f %%i in ('powershell -NoProfile -Command "$jar=(Get-Item '%JAR%').LastWriteTimeUtc; $stale=$false; foreach($f in (Get-ChildItem -Recurse -File -Path 'client\src','lib\src','pom.xml','client\pom.xml','lib\pom.xml' -EA SilentlyContinue)){ if(($f.Extension -eq '.java' -or $f.Extension -eq '.xml') -and $f.LastWriteTimeUtc -gt $jar){ $stale=$true } }; if($stale){'STALE'}else{'FRESH'}"') do set "CHECK=%%i"
    if /i "!CHECK!"=="FRESH" set "NEEDBUILD=0"
)

if "!NEEDBUILD!"=="1" (
    echo Client.jar thieu hoac da cu, dang build...
    call "%~dp0build.cmd"
    if errorlevel 1 (
        echo Build that bai. Khong the chay Client.
        pause
        exit /b 1
    )
) else (
    echo Client.jar van moi, bo qua build.
)

if not exist "%JAR%" (
    echo Khong tim thay client.jar sau khi build.
    pause
    exit /b 1
)

echo Dang khoi dong Client...
echo Dong cua so nay se dong Client.
start "Client" java -Xms512m -Xmx1g -jar "%JAR%"
exit /b 0
