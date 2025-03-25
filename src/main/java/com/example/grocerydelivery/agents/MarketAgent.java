package com.example.grocerydelivery.agents;

import com.example.grocerydelivery.behaviours.MarketContractNetResponderBehaviour;
import com.example.grocerydelivery.behaviours.MarketDeliveryRequestsServerBehaviour;
import com.example.grocerydelivery.utils.LoggerUtil;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.MessageTemplate;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MarketAgent represents a grocery store in the system that offers products at specific prices.
 * It registers its services in the DF and handles requests from DeliveryAgents.
 */
public class MarketAgent extends Agent {
    private String marketName;
    private Map<String, Double> inventory = new HashMap<>();
    private Logger logger;

    @Override
    protected void setup() {
        // Extract agent parameters
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) args[0];
            
            marketName = (String) params.get("name");
            // Initialize logger
            logger = LoggerUtil.getLogger(marketName, "Agent");
            
            logger.info("Market agent starting: {}", marketName);
            
            // Process inventory and prices
            String[] inventoryArray = (String[]) params.get("inventory");
            
            // Convert prices from Object[][] to Map
            Object[][] pricesArray = (Object[][]) params.get("prices");
            Map<String, Double> prices = new HashMap<>();
            if (pricesArray != null) {
                for (Object[] pair : pricesArray) {
                    String item = (String) pair[0];
                    Double price = (Double) pair[1];
                    prices.put(item, price);
                }
            }
            
            if (inventoryArray != null && prices != null) {
                for (String item : inventoryArray) {
                    Double price = prices.get(item);
                    if (price != null) {
                        inventory.put(item, price);
                        logger.debug("Added to inventory: {} at price {}", item, price);
                    }
                }
            }
            
            logger.info("Market has {} items in inventory", inventory.size());
        } else {
            // Default values if no args provided
            marketName = "DefaultMarket";
            logger = LoggerUtil.getLogger(marketName, "Agent");
            logger.warn("No parameters provided, using defaults: {}", marketName);
        }
        
        // Register in the DF
        registerInDF();
        
        // Create a template to match Contract Net Protocol messages
        MessageTemplate template = MarketContractNetResponderBehaviour.createMessageTemplate();
        
        // Add behavior to respond to contract net requests
        addBehaviour(new MarketContractNetResponderBehaviour(this, template));
        logger.debug("Added MarketContractNetResponderBehaviour");
        
        logger.info("Market agent {} setup completed", marketName);
    }
    
    /**
     * Register the market service in the Directory Facilitator (DF)
     */
    private void registerInDF() {
        try {
            // Create agent description
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            
            // Market service description
            ServiceDescription sd = new ServiceDescription();
            sd.setType("grocery-market");
            sd.setName(marketName);
            
            dfd.addServices(sd);
            
            // Register the service in the DF
            DFService.register(this, dfd);
            
            logger.info("Registered in DF as a grocery market");
            
        } catch (FIPAException e) {
            logger.error("Failed to register in DF", e);
        }
    }
    
    @Override
    protected void takeDown() {
        // Deregister from the DF
        try {
            DFService.deregister(this);
            logger.info("Deregistered from the DF");
        } catch (FIPAException e) {
            logger.error("Failed to deregister from DF", e);
        }
        
        logger.info("Market agent {} terminating", marketName);
    }
    
    /**
     * Log a message from the market agent.
     */
    public void log(String message) {
        logger.info(message);
    }
    
    /**
     * Gets the price of a specific item.
     * 
     * @param item The item to check
     * @return The price, or null if not available
     */
    public Double getPrice(String item) {
        return inventory.get(item);
    }
    
    /**
     * Gets the market name.
     */
    public String getMarketName() {
        return marketName;
    }
    
    /**
     * Gets the logger for this agent.
     */
    public Logger getLogger() {
        return logger;
    }
} 