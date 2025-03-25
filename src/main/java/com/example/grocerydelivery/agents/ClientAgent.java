package com.example.grocerydelivery.agents;

import com.example.grocerydelivery.behaviours.ClientFindDeliveryServicesBehaviour;
import com.example.grocerydelivery.behaviours.ClientOrderBehaviour;
import com.example.grocerydelivery.behaviours.ClientWaitBehaviour;
import com.example.grocerydelivery.utils.LoggerUtil;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ClientAgent represents a client who wants to order groceries 
 * and have them delivered to their home.
 */
public class ClientAgent extends Agent {
    private String clientName;
    private String[] shoppingList;
    private Logger logger;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        
        if (args != null && args.length > 0 && args[0] instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) args[0];
            
            // Extract parameters
            this.clientName = (String) params.get("name");
            this.shoppingList = (String[]) params.get("shoppingList");
            
            // Initialize logger
            this.logger = LoggerUtil.getLogger(clientName, "Agent");
            
            logger.info("{} started with shopping list: {}", clientName, Arrays.toString(shoppingList));
            
            // Add delay to ensure other agents are registered
            addBehaviour(new ClientWaitBehaviour(this, clientName, 
                // Find available delivery services and start the ordering process
                new ClientFindDeliveryServicesBehaviour(this, clientName, shoppingList) {
                    @Override
                    public int onEnd() {
                        // After finding delivery services, start the order process
                        findAndStartOrder();
                        return super.onEnd();
                    }
                }));
            
        } else {
            logger = LoggerUtil.getLogger("Unknown", "Agent");
            logger.error("ClientAgent requires parameters to start!");
            doDelete();
        }
    }
    
    /**
     * Finds delivery services and starts the order process
     */
    private void findAndStartOrder() {
        try {
            logger.info("{}: Finding delivery services for order...", clientName);
            
            // Search for delivery services
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("grocery-delivery");
            template.addServices(sd);
            
            DFAgentDescription[] result = DFService.search(this, template);
            
            if (result.length > 0) {
                // Convert DFAgentDescription to AID
                List<AID> deliveryAIDs = new ArrayList<>();
                for (DFAgentDescription dfd : result) {
                    deliveryAIDs.add(dfd.getName());
                }
                
                logger.info("{}: Found {} delivery services", clientName, deliveryAIDs.size());
                
                // Start the order process
                AID[] deliveryArray = deliveryAIDs.toArray(new AID[0]);
                addBehaviour(new ClientOrderBehaviour(this, clientName, shoppingList, deliveryArray));
                
            } else {
                logger.warn("{}: No delivery services found!", clientName);
            }
            
        } catch (FIPAException fe) {
            logger.error("Error searching for delivery services", fe);
        }
    }
    
    /**
     * Gets the logger for this agent.
     */
    public Logger getLogger() {
        return logger;
    }
    
    @Override
    protected void takeDown() {
        logger.info("{} terminated.", clientName);
    }
} 