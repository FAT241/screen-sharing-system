@echo off
cd /d "%~dp0..\bin"
start java -Xms1g -Xmx1g -jar client.jar
