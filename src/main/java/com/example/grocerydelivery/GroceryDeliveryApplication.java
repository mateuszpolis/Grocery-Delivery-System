package com.example.grocerydelivery;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.util.HashMap;
import java.util.Map;

/**
 * Main application class for the Grocery Delivery System.
 * This class starts the JADE platform and creates the necessary agents.
 */
public class GroceryDeliveryApplication {

    public static void main(String[] args) {
        try {
            // Get a hold on JADE runtime
            Runtime rt = Runtime.instance();

            // Create a default profile
            Profile profile = new ProfileImpl(true);
            profile.setParameter(Profile.GUI, "true");

            // Create a main container
            AgentContainer mainContainer = rt.createMainContainer(profile);
            
            System.out.println("JADE platform started successfully!");
            
            // Create market agents with inventory and prices
            for (int i = 1; i <= 3; i++) {
                Map<String, Object> marketParams = new HashMap<>();
                
                // Different markets have different inventory and prices
                if (i == 1) {
                    marketParams.put("inventory", new String[]{"milk", "coffee", "bread", "sugar"});
                    marketParams.put("prices", new Object[][]{
                        {"milk", 5.0}, {"coffee", 30.0}, {"bread", 4.5}, {"sugar", 3.2}
                    });
                } else if (i == 2) {
                    marketParams.put("inventory", new String[]{"coffee", "rice", "eggs", "flour"});
                    marketParams.put("prices", new Object[][]{
                        {"coffee", 25.0}, {"rice", 3.0}, {"eggs", 10.0}, {"flour", 2.8}
                    });
                } else {
                    marketParams.put("inventory", new String[]{"rice", "tea", "tomatoes", "potatoes"});
                    marketParams.put("prices", new Object[][]{
                        {"rice", 4.0}, {"tea", 12.0}, {"tomatoes", 8.0}, {"potatoes", 6.5}
                    });
                }
                
                marketParams.put("name", "Market" + i);
                
                Object[] marketArgs = new Object[]{marketParams};
                
                AgentController marketAgent = mainContainer.createNewAgent(
                        "Market" + i, 
                        "com.example.grocerydelivery.agents.MarketAgent", 
                        marketArgs);
                marketAgent.start();
            }
            
            // Create delivery agents with delivery fees
            for (int i = 1; i <= 2; i++) {
                Map<String, Object> deliveryParams = new HashMap<>();
                
                if (i == 1) {
                    deliveryParams.put("name", "BoltFood");
                    deliveryParams.put("fee", 10.0);
                } else {
                    deliveryParams.put("name", "UberEats");
                    deliveryParams.put("fee", 12.5);
                }
                
                Object[] deliveryArgs = new Object[]{deliveryParams};
                
                AgentController deliveryAgent = mainContainer.createNewAgent(
                        "Delivery" + i, 
                        "com.example.grocerydelivery.agents.DeliveryAgent", 
                        deliveryArgs);
                deliveryAgent.start();
            }
            
            // Create client agent with shopping list
            Map<String, Object> clientParams = new HashMap<>();
            clientParams.put("shoppingList", new String[]{"milk", "coffee", "rice"});
            clientParams.put("name", "Alice");
            
            Object[] clientArgs = new Object[]{clientParams};
            
            AgentController clientAgent = mainContainer.createNewAgent(
                    "Client1", 
                    "com.example.grocerydelivery.agents.ClientAgent", 
                    clientArgs);
            clientAgent.start();
            
            System.out.println("All agents created and started!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 