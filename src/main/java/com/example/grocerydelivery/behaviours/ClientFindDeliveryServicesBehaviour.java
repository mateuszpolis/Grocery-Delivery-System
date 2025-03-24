package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.agents.ClientAgent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Iterator;

/**
 * Behavior for a ClientAgent to find delivery services using DF
 */
public class ClientFindDeliveryServicesBehaviour extends OneShotBehaviour {
    
    private final String clientName;
    private final String[] shoppingList;
    
    public ClientFindDeliveryServicesBehaviour(ClientAgent agent, String clientName, String[] shoppingList) {
        super(agent);
        this.clientName = clientName;
        this.shoppingList = shoppingList;
    }
    
    @Override
    public void action() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("grocery-delivery");
        template.addServices(sd);
        
        try {
            System.out.println(clientName + " searching for delivery services...");
            
            DFAgentDescription[] result = DFService.search(myAgent, template);
            
            if (result.length > 0) {
                System.out.println(clientName + " found " + result.length + " delivery services:");
                
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
                    
                    System.out.println("  - " + deliveryName + " (Fee: " + deliveryFee + " zł)");
                    
                    // For demonstration, send a request to the first delivery service
                    if (dfd == result[0]) {
                        // Send a request to the delivery service
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                        request.addReceiver(dfd.getName());
                        request.setContent(String.join(",", shoppingList));
                        request.setConversationId("grocery-order");
                        myAgent.send(request);
                        
                        System.out.println(clientName + " sent order request to " + deliveryName);
                    }
                }
            } else {
                System.out.println(clientName + " couldn't find any delivery services.");
            }
            
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
} 