# Grocery Delivery System

This project implements a multi-agent system for a grocery delivery scenario using JADE (Java Agent DEvelopment Framework). 

## Scenario

The system simulates a grocery delivery service with three types of agents:
1. **ClientAgent**: Represents a client who wants to order groceries
2. **DeliveryAgent**: Represents a delivery service that connects clients with markets
3. **MarketAgent**: Represents grocery stores with different product inventories and pricing

The workflow is as follows:
- Client searches for delivery services
- Delivery services connect with different markets to fulfill the order
- Markets provide product information and pricing
- Delivery services calculate total costs and provide offers to clients
- Client selects a delivery service and places an order

## Project Structure

- `src/main/java/com/example/grocerydelivery/`: Main package
  - `GroceryDeliveryApplication.java`: Main application that sets up the JADE platform and agents
  - `agents/`: Contains agent implementations
    - `ClientAgent.java`: Client agent implementation
    - `DeliveryAgent.java`: Delivery service agent implementation
    - `MarketAgent.java`: Market agent implementation
  - `behaviours/`: Contains agent behaviors
  - `config/`: Contains configuration utilities
    - `ConfigLoader.java`: Loads agent configurations from JSON
- `config.json`: Configuration file for agents and their relationships

## Agent Configuration

The system now uses a JSON configuration file (`config.json`) to define all agents and their relationships:

1. **Markets**: Define each market with:
   - `name`: Market identifier
   - `inventory`: Array of available products
   - `prices`: Map of products to prices

2. **Delivery Services**: Define each delivery service with:
   - `name`: Service identifier
   - `fee`: Delivery fee
   - `connectedMarkets`: Array of market names this service is connected to

3. **Clients**: Define each client with:
   - `name`: Client identifier
   - `shoppingList`: Array of products the client wants to order

Example configuration:
```json
{
  "markets": [
    {
      "name": "Market1",
      "inventory": ["milk", "coffee", "bread"],
      "prices": {
        "milk": 5.0,
        "coffee": 30.0,
        "bread": 4.5
      }
    }
  ],
  "deliveryServices": [
    {
      "name": "BoltFood",
      "fee": 10.0,
      "connectedMarkets": ["Market1", "Market2"]
    }
  ],
  "clients": [
    {
      "name": "Alice",
      "shoppingList": ["milk", "coffee"]
    }
  ]
}
```

## Prerequisites

- Java 17 or higher
- Maven

## Running the Application

### Using the Run Scripts (Recommended)

For macOS/Linux:
```bash
./run.sh
```

For Windows:
```
run.bat
```

These scripts will:
1. Compile the project
2. Copy all dependencies to the target directory
3. Run the application with the JADE platform and the specified configuration file

### Using a Custom Config File

To use a custom configuration file:

```bash
java -cp target/classes:lib/jade.jar:target/dependency/* com.example.grocerydelivery.GroceryDeliveryApplication /path/to/your/config.json
```

## Implementation Details

### Task 1 & 2: Implementation

The project now implements:

1. **Agent types with behaviors**:
   - `ClientAgent`: Searches for delivery services and places orders
   - `DeliveryAgent`: Registers its service in DF and handles client requests
   - `MarketAgent`: Registers its products in DF and connects with delivery agents

2. **Market Selection Algorithm**:
   - Delivery agents initially try to select all items from the market with the largest number available
   - If multiple markets have the same items, they choose the one with the lowest price
   - If not all items can be selected from one place, they repeat for missing items

3. **JSON Configuration**:
   - All agents and their relationships are defined in a JSON configuration file
   - Delivery services have specified connections to markets, limiting which markets they can interact with
   - Multiple clients can be defined with different shopping lists

4. **Contract Net Protocol**:
   - Implementation of the FIPA Contract Net Protocol for negotiation between agents
   - Proper handling of proposals, acceptances, and rejections

## License

This project is for educational purposes. 