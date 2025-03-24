#!/bin/bash
cd "$(dirname "$0")"
mvn clean compile

# Create a new test scenario with a specific shopping list
run_scenario() {
  local scenario=$1
  local shopping_list=$2
  
  echo "===== Running Scenario $scenario: Shopping List = [$shopping_list] ====="
  java -cp target/classes:lib/jade.jar jade.Boot -gui -agents "Market1:com.example.grocerydelivery.agents.MarketAgent({\"name\":\"Market1\",\"inventory\":[\"milk\",\"coffee\",\"bread\",\"sugar\"],\"prices\":[[\"milk\",5.0],[\"coffee\",30.0],[\"bread\",4.5],[\"sugar\",3.2]]});Market2:com.example.grocerydelivery.agents.MarketAgent({\"name\":\"Market2\",\"inventory\":[\"coffee\",\"rice\",\"eggs\",\"flour\"],\"prices\":[[\"coffee\",25.0],[\"rice\",3.0],[\"eggs\",10.0],[\"flour\",2.8]]});Market3:com.example.grocerydelivery.agents.MarketAgent({\"name\":\"Market3\",\"inventory\":[\"rice\",\"tea\",\"tomatoes\",\"potatoes\"],\"prices\":[[\"rice\",4.0],[\"tea\",12.0],[\"tomatoes\",8.0],[\"potatoes\",6.5]]});Market4:com.example.grocerydelivery.agents.MarketAgent({\"name\":\"Market4\",\"inventory\":[\"milk\",\"tomatoes\",\"potatoes\"],\"prices\":[[\"milk\",2.0],[\"tomatoes\",7.0],[\"potatoes\",5.0]]});BoltFood:com.example.grocerydelivery.agents.DeliveryAgent({\"name\":\"BoltFood\",\"fee\":10.0});UberEats:com.example.grocerydelivery.agents.DeliveryAgent({\"name\":\"UberEats\",\"fee\":12.5});Alice:com.example.grocerydelivery.agents.ClientAgent({\"name\":\"Alice\",\"shoppingList\":[$shopping_list]})"
}

# Scenario 1: Basic ordering (should get milk from Market1, coffee and rice from Market2)
# Expected: milk from Market1, coffee and rice from Market2
if [[ "$1" == "1" || -z "$1" ]]; then
  run_scenario 1 "\"milk\",\"coffee\",\"rice\""
fi

# Scenario 2: Choose by price (should get milk from Market4 because it's cheaper)
# Expected: milk from Market4, coffee and rice from Market2
if [[ "$1" == "2" ]]; then
  run_scenario 2 "\"milk\",\"coffee\",\"rice\""
fi

# Scenario 3: Items from all markets
# Expected: milk from Market1, coffee from Market2, tea from Market3
if [[ "$1" == "3" ]]; then
  run_scenario 3 "\"milk\",\"coffee\",\"tea\""
fi

# Scenario 4: Some items unavailable
# Expected: milk and bread from Market1, eggs from Market2, tomatoes from Market3, cheese unavailable
if [[ "$1" == "4" ]]; then
  run_scenario 4 "\"milk\",\"bread\",\"eggs\",\"tomatoes\",\"cheese\""
fi 