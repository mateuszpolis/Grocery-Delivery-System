#!/bin/bash
cd "$(dirname "$0")"

# Default config file
CONFIG_FILE="config.json"

# If an argument is provided, use it as the config file
if [ $# -gt 0 ]; then
    CONFIG_FILE="$1"
fi

echo "Using configuration file: $CONFIG_FILE"

# Compile the project
mvn clean compile

# Run JADE with the specified main class and config file
java -cp target/classes:lib/jade.jar:target/dependency/* com.example.grocerydelivery.GroceryDeliveryApplication "$CONFIG_FILE"

echo "Application terminated" 