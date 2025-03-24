#!/bin/bash

# Compile the project
mvn clean compile

# Run JADE with the specified main class
java -cp target/classes:lib/jade.jar com.example.grocerydelivery.GroceryDeliveryApplication

echo "Application terminated" 