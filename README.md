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
  - `behaviours/`: Contains agent behaviors (to be implemented)
  - `models/`: Contains data models (to be implemented)

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

These scripts will compile the project and run the application with the correct classpath including the JADE library.

### Using Maven (Not Working Due to Classpath Issues)

```bash
mvn compile exec:java
```
Note: This method might not work correctly due to issues with the system-scoped JADE dependency.

### Using an IDE

1. Import the project into your IDE (IntelliJ, Eclipse, etc.)
2. Add the `lib/jade.jar` to your project classpath
3. Run the `GroceryDeliveryApplication` class

## Implementation Details

### Task 1 Implementation

The first task of the project has been implemented:

#### 1. Created three agent types:
- `ClientAgent`: Searches for delivery services and places orders
- `DeliveryAgent`: Registers its service in DF and handles client requests
- `MarketAgent`: Registers its products in DF and connects with delivery agents

#### 2. Parameter passing:
- All agents accept parameters via a Map in their constructor arguments

#### 3. DF Registration:
- DeliveryAgent registers its service with "grocery-delivery" type
- MarketAgent registers each product as a separate service with "grocery-item" type

#### 4. Market and Delivery agent connection:
- MarketAgent can receive queries from DeliveryAgent and provide product information
- DeliveryAgent can find markets by searching the DF for specific products

#### 5. Client-Delivery interaction:
- ClientAgent can search for delivery services in the DF
- ClientAgent can send order requests to delivery services

## License

This project is for educational purposes. 