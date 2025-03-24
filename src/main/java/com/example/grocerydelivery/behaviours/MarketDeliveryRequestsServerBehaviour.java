package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.agents.MarketAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Behavior to handle requests from DeliveryAgents for the MarketAgent.
 * Responds to price inquiries and provides available items with their prices.
 */
public class MarketDeliveryRequestsServerBehaviour extends CyclicBehaviour {
    
    private final MarketAgent marketAgent;
    
    public MarketDeliveryRequestsServerBehaviour(MarketAgent agent) {
        this.marketAgent = agent;
    }
    
    @Override
    public void action() {
        // Listen for price inquiry messages
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
        ACLMessage msg = myAgent.receive(mt);
        
        if (msg != null) {
            try {
                // Process the request
                String content = msg.getContent();
                String conversationId = msg.getConversationId();
                
                marketAgent.log("received inquiry for: " + content + " (conversation: " + conversationId + ")");
                
                // Prepare response with available items and prices
                ACLMessage reply = msg.createReply();
                
                // Make sure to preserve the conversation ID
                if (conversationId != null) {
                    reply.setConversationId(conversationId);
                }
                
                // For demonstration, parse a list of items requested
                String[] requestedItems = content.split(",");
                
                // Build response with available items and their prices
                StringBuilder responseBuilder = new StringBuilder();
                int availableItemCount = 0;
                
                for (String requestedItem : requestedItems) {
                    requestedItem = requestedItem.trim();
                    Double price = marketAgent.getPrice(requestedItem);
                    if (price != null) {
                        responseBuilder.append(requestedItem).append(":").append(price).append(",");
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
                marketAgent.log("Error processing message: " + e.getMessage());
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