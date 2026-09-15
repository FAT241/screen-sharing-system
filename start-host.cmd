@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul

title Screen Sharing - HOST

if not exist ".bin\host.jar" (
    echo host.jar khong tim thay. Dang build project...
    call "%~dp0build.cmd"
    if errorlevel 1 (
        echo Build that bai. Khong the chay Host.
        pause
        exit /b 1
    )
)

echo Dang khoi dong Host (server)...
echo Dong cua so nay se dong Host.
start "Host" java -Xms512m -Xmx2g -jar ".bin\host.jar"
exit /b 0