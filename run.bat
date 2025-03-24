@echo off

REM Compile the project
call mvn clean compile

REM Run JADE with the specified main class
java -cp target\classes;lib\jade.jar com.example.grocerydelivery.GroceryDeliveryApplication

echo Application terminated
pause 