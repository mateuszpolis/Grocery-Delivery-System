package com.example.grocerydelivery;

import com.example.grocerydelivery.config.ConfigLoader;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.io.File;
import java.util.List;
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
            
            // Load configuration from config.json
            String configPath = "config.json";
            if (args.length > 0) {
                configPath = args[0];
            }
            
            File configFile = new File(configPath);
            if (!configFile.exists()) {
                System.err.println("Configuration file not found: " + configPath);
                System.exit(1);
            }
            
            ConfigLoader config = new ConfigLoader(configPath);
            
            // Create market agents
            List<Map<String, Object>> markets = config.getMarkets();
            for (Map<String, Object> marketParams : markets) {
                String marketName = (String) marketParams.get("name");
                
                Object[] marketArgs = new Object[]{marketParams};
                
                AgentController marketAgent = mainContainer.createNewAgent(
                        marketName, 
                        "com.example.grocerydelivery.agents.MarketAgent", 
                        marketArgs);
                marketAgent.start();
            }
            
            // Create delivery agents
            List<Map<String, Object>> deliveryServices = config.getDeliveryServices();
            for (Map<String, Object> deliveryParams : deliveryServices) {
                String deliveryName = (String) deliveryParams.get("name");
                
                Object[] deliveryArgs = new Object[]{deliveryParams};
                
                AgentController deliveryAgent = mainContainer.createNewAgent(
                        deliveryName, 
                        "com.example.grocerydelivery.agents.DeliveryAgent", 
                        deliveryArgs);
                deliveryAgent.start();
            }
            
            // Create client agents
            List<Map<String, Object>> clients = config.getClients();
            for (Map<String, Object> clientParams : clients) {
                String clientName = (String) clientParams.get("name");
                
                Object[] clientArgs = new Object[]{clientParams};
                
                AgentController clientAgent = mainContainer.createNewAgent(
                        clientName, 
                        "com.example.grocerydelivery.agents.ClientAgent", 
                        clientArgs);
                clientAgent.start();
            }
            
            System.out.println("All agents created and started!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 