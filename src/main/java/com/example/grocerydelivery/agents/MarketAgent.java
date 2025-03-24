package com.example.grocerydelivery.agents;

import com.example.grocerydelivery.behaviours.MarketContractNetResponderBehaviour;
import com.example.grocerydelivery.behaviours.MarketDeliveryRequestsServerBehaviour;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * MarketAgent represents a grocery store in the system that offers products at specific prices.
 * It registers its services in the DF and handles requests from DeliveryAgents.
 */
public class MarketAgent extends Agent {
    private String marketName;
    private String[] inventory;
    private Map<String, Double> prices = new HashMap<>();

    @Override
    protected void setup() {
        Object[] args = getArguments();
        
        if (args != null && args.length > 0 && args[0] instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) args[0];
            
            // Extract parameters
            this.marketName = (String) params.get("name");
            this.inventory = (String[]) params.get("inventory");
            
            // Extract prices
            Object[][] priceArray = (Object[][]) params.get("prices");
            for (Object[] price : priceArray) {
                String item = (String) price[0];
                Double value = (Double) price[1];
                prices.put(item, value);
            }
            
            log("started with inventory: " + Arrays.toString(inventory));
            
            // Register in the DF
            registerInDF();
            
            // Add behavior to handle messages from delivery agents using the old protocol
            addBehaviour(new MarketDeliveryRequestsServerBehaviour(this));
            
            // Add behavior to handle Contract Net Protocol requests
            addBehaviour(new MarketContractNetResponderBehaviour(
                this, MarketContractNetResponderBehaviour.createMessageTemplate()));
            
        } else {
            System.out.println("MarketAgent requires parameters to start!");
            doDelete();
        }
    }
    
    /**
     * Register the market services in the Directory Facilitator (DF)
     */
    private void registerInDF() {
        try {
            // Create agent description
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            
            // Main market service description
            ServiceDescription mainSd = new ServiceDescription();
            mainSd.setType("grocery-market");
            mainSd.setName(marketName);
            dfd.addServices(mainSd);
            
            // Register individual items as services
            for (String item : inventory) {
                ServiceDescription itemSd = new ServiceDescription();
                itemSd.setType("grocery-item");
                itemSd.setName(item);
                // Add the price as a property
                Property priceProp = new Property("price", prices.get(item).toString());
                itemSd.addProperties(priceProp);
                dfd.addServices(itemSd);
            }
            
            // Register the services in the DF
            DFService.register(this, dfd);
            
            log("registered in DF with services: main market and " + inventory.length + " items");
            
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    /**
     * Gets the price of an item
     * @param item The item name
     * @return The price of the item, or null if not available
     */
    public Double getPrice(String item) {
        return prices.get(item);
    }
    
    /**
     * Logs a message with the market name prefix
     * @param message The message to log
     */
    public void log(String message) {
        System.out.println(marketName + " " + message);
    }
    
    @Override
    protected void takeDown() {
        // Deregister from the DF
        try {
            DFService.deregister(this);
            log("deregistered from DF");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        log("terminated.");
    }
} 