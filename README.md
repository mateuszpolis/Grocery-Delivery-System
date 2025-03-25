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
- `config.json`: Default configuration file for agents and their relationships
- `config_1.json`, `config_2.json`, `config_3.json`: Test configuration files

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

#### With Default Configuration

For macOS/Linux:
```bash
./run.sh
```

For Windows:
```
run.bat
```

#### With Custom Configuration

You can specify a custom configuration file as a command-line argument:

For macOS/Linux:
```bash
./run.sh path/to/config.json
```

For Windows:
```
run.bat path\to\config.json
```

These scripts will:
1. Compile the project
2. Copy all dependencies to the target directory
3. Run the application with the JADE platform and the specified configuration file

### Running Test Scenarios

The project includes three test configuration files to validate the market selection algorithm:

For macOS/Linux:
```bash
./run_tests.sh
```

For Windows:
```
run_tests.bat
```

This will run all three test scenarios sequentially.

To run an individual test scenario:

```bash
./run.sh config_1.json  # Scenario 1: Most Items Available First
./run.sh config_2.json  # Scenario 2: Price Tie-Breaker
./run.sh config_3.json  # Scenario 3: Complex Multi-Market Selection
```

### Running Manually

If you prefer to run the application manually:

```bash
java -cp target/classes:lib/jade.jar:target/dependency/* com.example.grocerydelivery.GroceryDeliveryApplication [config-file]
```

Where `[config-file]` is optional and defaults to `config.json` if not specified.

## Test Scenarios

The project includes three pre-configured test scenarios to validate the market selection algorithm:

### Scenario 1: Most Items Available First (config_1.json)

Tests that the delivery agent selects the market with the most available items first.

- **Order**: milk, coffee, rice
- **Market1**: milk, coffee
- **Market2**: coffee
- **Market3**: rice

**Expected Outcome**: Deliverer first selects Market1 (2 items), then Market3 (1 item).

### Scenario 2: Price Tie-Breaker (config_2.json)

Tests that when multiple markets have the same number of items, the delivery agent selects the one with the lowest total price.

- **Order**: milk, coffee, rice
- **Market1**: milk (5zl), coffee (30zl) - total 35zl
- **Market2**: coffee (25zl), rice (3zl) - total 28zl
- **Market3**: rice (4zl) - total 4zl

**Expected Outcome**: Deliverer first selects Market2 (2 items, cheaper), then Market1 (1 item).

### Scenario 3: Complex Multi-Market Selection (config_3.json)

Tests a complex scenario where the algorithm needs to make decisions based on both item count and price.

- **Order**: milk, coffee, rice
- **Market1**: milk (5zl), coffee (30zl)
- **Market2**: coffee (25zl), rice (3zl)
- **Market3**: rice (4zl)
- **Market4**: milk (2zl)

**Expected Outcome**: Deliverer first selects Market2 (2 items, cheaper), then Market4 (milk at 2zl instead of Market1's 5zl).

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