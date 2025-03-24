package com.example.grocerydelivery.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

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
            
            System.out.println(marketName + " started with inventory: " + Arrays.toString(inventory));
            
            // Register in the DF
            registerInDF();
            
            // Add behavior to handle messages from delivery agents
            addBehaviour(new DeliveryRequestsServer());
            
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
            
            System.out.println(marketName + " registered in DF with services: main market and " + inventory.length + " items");
            
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    /**
     * Behavior to handle requests from DeliveryAgents
     */
    private class DeliveryRequestsServer extends CyclicBehaviour {
        @Override
        public void action() {
            // Listen for price inquiry messages
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                try {
                    // Process the request
                    String content = msg.getContent();
                    System.out.println(marketName + " received inquiry for: " + content);
                    
                    // Prepare response with available items and prices
                    ACLMessage reply = msg.createReply();
                    
                    // For demonstration, parse a list of items requested
                    String[] requestedItems = content.split(",");
                    
                    // Build response with available items and their prices
                    StringBuilder responseBuilder = new StringBuilder();
                    int availableItemCount = 0;
                    
                    for (String requestedItem : requestedItems) {
                        requestedItem = requestedItem.trim();
                        if (prices.containsKey(requestedItem)) {
                            responseBuilder.append(requestedItem).append(":").append(prices.get(requestedItem)).append(",");
                            availableItemCount++;
                        }
                    }
                    
                    if (availableItemCount > 0) {
                        // Remove trailing comma
                        String response = responseBuilder.substring(0, responseBuilder.length() - 1);
                        
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent(response);
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("No-items-available");
                    }
                    
                    myAgent.send(reply);
                    
                } catch (Exception e) {
                    System.out.println("Error processing message: " + e.getMessage());
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Error-processing-request");
                    myAgent.send(reply);
                }
            } else {
                block();
            }
        }
    }
    
    @Override
    protected void takeDown() {
        // Deregister from the DF
        try {
            DFService.deregister(this);
            System.out.println(marketName + " deregistered from DF");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        System.out.println(marketName + " terminated.");
    }
} 