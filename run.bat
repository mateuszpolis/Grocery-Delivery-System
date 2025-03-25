@echo off

REM Default config file
SET CONFIG_FILE=config.json

REM If an argument is provided, use it as the config file
IF NOT "%~1"=="" (
    SET CONFIG_FILE=%~1
)

ECHO Using configuration file: %CONFIG_FILE%

REM Compile the project
call mvn clean compile

REM Run JADE with the specified main class and config file
java -cp target\classes;lib\jade.jar;target\dependency\* com.example.grocerydelivery.GroceryDeliveryApplication %CONFIG_FILE%

ECHO Application terminated
pause 