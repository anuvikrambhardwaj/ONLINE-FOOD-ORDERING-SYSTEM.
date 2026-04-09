@echo off
title Swiggy Mega Server
color 0A
echo ========================================
echo    SWIGGY MEGA - Food Delivery Server
echo ========================================
echo.
echo Compiling...
javac -cp "lib/sqlite-jdbc.jar" src/DatabaseManager.java src/Server.java
echo.
echo Starting Server...
echo.
echo ========================================
echo   Server is LIVE at: http://localhost:8000
echo   Browser me ye link kholo!
echo   Band karne ke liye ye window band karo.
echo ========================================
echo.
java -cp ".;lib/sqlite-jdbc.jar" src.Server
pause
