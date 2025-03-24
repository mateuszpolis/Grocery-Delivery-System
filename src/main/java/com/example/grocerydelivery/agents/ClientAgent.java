package com.example.grocerydelivery.agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * ClientAgent represents a client who wants to order groceries 
 * and have them delivered to their home.
 */
public class ClientAgent extends Agent {
    private String clientName;
    private String[] shoppingList;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        
        if (args != null && args.length > 0 && args[0] instanceof Map) {
            Map<String, Object> params = (Map<String, Object>) args[0];
            
            // Extract parameters
            this.clientName = (String) params.get("name");
            this.shoppingList = (String[]) params.get("shoppingList");
            
            System.out.println(clientName + " started with shopping list: " + Arrays.toString(shoppingList));
            
            // Add delay to ensure other agents are registered
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    try {
                        System.out.println(clientName + " waiting for delivery services to register...");
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                
                @Override
                public int onEnd() {
                    // Search for delivery services after delay
                    myAgent.addBehaviour(new FindDeliveryServices());
                    return super.onEnd();
                }
            });
            
        } else {
            System.out.println("ClientAgent requires parameters to start!");
            doDelete();
        }
    }
    
    /**
     * Behavior to find delivery services using DF
     */
    private class FindDeliveryServices extends OneShotBehaviour {
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
                        Iterator it = deliverySD.getAllProperties();
                        while (it.hasNext()) {
                            Property prop = (Property) it.next();
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
    
    @Override
    protected void takeDown() {
        System.out.println(clientName + " terminated.");
    }
} 