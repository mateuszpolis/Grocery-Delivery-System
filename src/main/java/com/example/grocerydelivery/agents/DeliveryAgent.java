package com.example.grocerydelivery.agents;

import com.example.grocerydelivery.behaviours.DeliveryClientRequestsServerBehaviour;
import com.example.grocerydelivery.behaviours.DeliveryOrderProcessingBehaviour;
import com.example.grocerydelivery.utils.LoggerUtil;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Delivery agent that connects clients with markets and handles order processing.
 */
public class DeliveryAgent extends Agent {
    
    private String deliveryServiceName;
    private double deliveryFee;
    private List<AID> connectedMarkets = new ArrayList<>();
    private Logger logger;
    
    @Override
    protected void setup() {
        // Extract agent parameters
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) args[0];
            
            deliveryServiceName = (String) params.get("name");
            deliveryFee = ((Number) params.get("fee")).doubleValue();
            
            // Initialize logger
            logger = LoggerUtil.getLogger(deliveryServiceName, "Agent");
            
            logger.info("Delivery agent starting: {} with fee {}", deliveryServiceName, deliveryFee);
            
            // Process connected markets
            String[] marketNamesArray = (String[]) params.get("connectedMarkets");
            if (marketNamesArray != null) {
                logger.info("Connected to {} markets: {}", marketNamesArray.length, String.join(", ", marketNamesArray));
                for (String marketName : marketNamesArray) {
                    connectedMarkets.add(new AID(marketName, AID.ISLOCALNAME));
                }
            }
        } else {
            // Default values if no args provided
            deliveryServiceName = "DefaultDelivery";
            deliveryFee = 10.0;
            logger = LoggerUtil.getLogger(deliveryServiceName, "Agent");
            logger.warn("No parameters provided, using defaults: {} with fee {}", 
                       deliveryServiceName, deliveryFee);
        }
        
        // Register the delivery service in the DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        
        ServiceDescription sd = new ServiceDescription();
        sd.setType("grocery-delivery");
        sd.setName(deliveryServiceName);
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
            logger.info("Registered with DF as grocery-delivery service: {}", deliveryServiceName);
        } catch (FIPAException e) {
            logger.error("Failed to register with DF", e);
        }
        
        // Add behavior to handle client requests
        addBehaviour(new DeliveryClientRequestsServerBehaviour(this, deliveryServiceName));
        logger.debug("Added DeliveryClientRequestsServerBehaviour");
        
        // Add behavior to process orders
        addBehaviour(new DeliveryOrderProcessingBehaviour(this, deliveryServiceName, deliveryFee));
        logger.debug("Added DeliveryOrderProcessingBehaviour");
        
        logger.info("Delivery agent {} setup completed", deliveryServiceName);
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
        
        logger.info("Delivery agent {} terminating", deliveryServiceName);
    }
    
    /**
     * Gets the name of this delivery service.
     */
    public String getDeliveryServiceName() {
        return deliveryServiceName;
    }
    
    /**
     * Gets the delivery fee charged by this service.
     */
    public double getDeliveryFee() {
        return deliveryFee;
    }
    
    /**
     * Gets the list of markets this delivery service is connected to.
     */
    public List<AID> getConnectedMarkets() {
        return connectedMarkets;
    }
    
    /**
     * Gets the logger for this agent.
     */
    public Logger getLogger() {
        return logger;
    }
} 