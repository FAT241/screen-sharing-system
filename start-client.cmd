@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul

title Screen Sharing - CLIENT

if not exist ".bin\client.jar" (
    echo client.jar khong tim thay. Dang build project...
    call "%~dp0build.cmd"
    if errorlevel 1 (
        echo Build that bai. Khong the chay Client.
        pause
        exit /b 1
    )
)

echo Dang khoi dong Client...
echo Dong cua so nay se dong Client.
start "Client" java -Xms512m -Xmx1g -jar ".bin\client.jar"
exit /b 0