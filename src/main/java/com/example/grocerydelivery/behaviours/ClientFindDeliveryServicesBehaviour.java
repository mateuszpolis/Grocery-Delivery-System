package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.agents.ClientAgent;
import com.example.grocerydelivery.utils.LoggerUtil;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.UUID;

/**
 * Behavior for a ClientAgent to find delivery services using DF
 */
public class ClientFindDeliveryServicesBehaviour extends OneShotBehaviour {
    
    private final String clientName;
    private final String[] shoppingList;
    private final Logger logger;
    
    public ClientFindDeliveryServicesBehaviour(ClientAgent agent, String clientName, String[] shoppingList) {
        super(agent);
        this.clientName = clientName;
        this.shoppingList = shoppingList;
        this.logger = LoggerUtil.getLogger(
            "ClientFindServices_" + clientName, "Behaviour");
    }
    
    @Override
    public void action() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("grocery-delivery");
        template.addServices(sd);
        
        try {
            logger.info("{} searching for delivery services...", clientName);
            
            DFAgentDescription[] result = DFService.search(myAgent, template);
            
            if (result.length > 0) {
                logger.info("{} found {} delivery services:", clientName, result.length);
                
                for (DFAgentDescription dfd : result) {
                    ServiceDescription deliverySD = (ServiceDescription) dfd.getAllServices().next();
                    String deliveryName = deliverySD.getName();
                    
                    // Get the delivery fee if available
                    String deliveryFee = "unknown";
                    @SuppressWarnings("unchecked")
                    Iterator<Property> it = deliverySD.getAllProperties();
                    while (it.hasNext()) {
                        Property prop = it.next();
                        if ("delivery-fee".equals(prop.getName())) {
                            deliveryFee = prop.getValue().toString();
                            break;
                        }
                    }
                    
                    logger.info("  - {} (Fee: {} zł)", deliveryName, deliveryFee);
                    
                    // No longer sending requests here - will be handled by ClientOrderBehaviour
                }
            } else {
                logger.warn("{} couldn't find any delivery services.", clientName);
            }
            
        } catch (FIPAException fe) {
            logger.error("Error searching for delivery services", fe);
        }
    }
} 