@echo off

ECHO ======================================
ECHO Running Test Scenario 1: Most Items Available First
ECHO ======================================
call run.bat config_1.json

ECHO.
ECHO ======================================
ECHO Running Test Scenario 2: Price Tie-Breaker
ECHO ======================================
call run.bat config_2.json

ECHO.
ECHO ======================================
ECHO Running Test Scenario 3: Complex Multi-Market Selection
ECHO ======================================
call run.bat config_3.json

ECHO.
ECHO All tests completed
pause 