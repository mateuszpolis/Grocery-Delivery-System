#!/bin/bash
cd "$(dirname "$0")"

# Compile the project
mvn clean compile

# Run JADE with the specified main class
java -cp target/classes:lib/jade.jar:target/dependency/* com.example.grocerydelivery.GroceryDeliveryApplication config.json

echo "Application terminated" 