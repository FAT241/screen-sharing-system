@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
chcp 65001 >nul

title Screen Sharing Launcher

echo ===============================================
echo            SCREEN SHARING LAUNCHER
echo ===============================================
echo  [1] Start Host        (server, chia se man hinh)
echo  [2] Start Client      (xem man hinh tu xa)
echo  [3] Rebuild project
echo  [4] Exit
echo ===============================================
set /p "choice=Chon (1-4): "

if "%choice%"=="1" goto host
if "%choice%"=="2" goto client
if "%choice%"=="3" goto build
if "%choice%"=="4" exit /b 0
echo Lua chon khong hop le.
ping 127.0.0.1 -n 2 >nul
goto done

:host
start "" "%~dp0start-host.cmd"
goto done

:client
start "" "%~dp0start-client.cmd"
goto done

:build
call "%~dp0build.cmd"
goto done

:done
exit /b 0