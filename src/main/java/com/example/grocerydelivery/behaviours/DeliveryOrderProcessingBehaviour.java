package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.agents.DeliveryAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Behavior for DeliveryAgent to process order requests from clients and 
 * handle the complete order processing workflow.
 */
public class DeliveryOrderProcessingBehaviour extends CyclicBehaviour {
    
    private final DeliveryAgent deliveryAgent;
    private final String deliveryServiceName;
    private final double deliveryFee;
    
    // Track active conversation IDs to avoid duplicate processing
    private final Map<String, Boolean> activeConversations = new HashMap<>();
    
    public DeliveryOrderProcessingBehaviour(Agent agent, String serviceName, double fee) {
        super(agent);
        this.deliveryAgent = (DeliveryAgent) agent;
        this.deliveryServiceName = serviceName;
        this.deliveryFee = fee;
    }
    
    @Override
    public void action() {
        // Create templates for different message types to handle each separately
        
        // 1. Template for new order requests
        MessageTemplate orderRequestTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        processOrderRequests(orderRequestTemplate);
        
        // 2. Template for payment messages
        MessageTemplate paymentTemplate = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
        processPayments(paymentTemplate);
        
        // 3. Template for rejection messages
        MessageTemplate rejectTemplate = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
        processRejections(rejectTemplate);
        
        // Block if no messages
        block();
    }
    
    private void processOrderRequests(MessageTemplate mt) {
        ACLMessage msg = myAgent.receive(mt);
        
        if (msg != null) {
            // Get the conversation ID to track this request
            String conversationId = msg.getConversationId();
            
            // Skip if we've already processed this conversation
            if (activeConversations.containsKey(conversationId)) {
                System.out.println(deliveryServiceName + ": Ignoring duplicate request with ID: " + conversationId);
                return;
            }
            
            // Mark this conversation as active
            activeConversations.put(conversationId, true);
            
            // Process the request
            String content = msg.getContent();
            AID clientAID = msg.getSender();
            
            System.out.println(deliveryServiceName + ": Received order request from " + 
                               clientAID.getLocalName() + ": " + content);
            
            // Parse shopping list
            String[] shoppingList = content.split(",");
            
            // If connectedMarkets is specified, use only those markets
            DeliveryAgent deliveryAgent = (DeliveryAgent) myAgent;
            List<AID> connectedMarkets = deliveryAgent.getConnectedMarkets();
            
            if (!connectedMarkets.isEmpty()) {
                // Use only connected markets
                System.out.println(deliveryServiceName + ": Using " + connectedMarkets.size() + 
                                  " connected markets for order processing");
                
                AID[] marketAIDs = connectedMarkets.toArray(new AID[0]);
                
                // Create contract net initiator to negotiate with markets
                ACLMessage cfp = DeliveryContractNetInitiatorBehaviour.createCFP(
                    myAgent, marketAIDs, shoppingList, conversationId);
                
                myAgent.addBehaviour(new DeliveryContractNetInitiatorBehaviour(
                    myAgent, cfp, shoppingList, deliveryFee, clientAID, conversationId));
                
            } else {
                // Use DF to find all markets (legacy behavior)
                try {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("grocery-market");
                    template.addServices(sd);
                    
                    DFAgentDescription[] marketAgents = DFService.search(myAgent, template);
                    
                    if (marketAgents.length > 0) {
                        System.out.println(deliveryServiceName + ": Found " + marketAgents.length + 
                                          " markets for order processing");
                        
                        // Convert DFAgentDescription array to AID array
                        AID[] marketAIDs = new AID[marketAgents.length];
                        for (int i = 0; i < marketAgents.length; i++) {
                            marketAIDs[i] = marketAgents[i].getName();
                        }
                        
                        // Create contract net initiator to negotiate with markets
                        ACLMessage cfp = DeliveryContractNetInitiatorBehaviour.createCFP(
                            myAgent, marketAIDs, shoppingList, conversationId);
                        
                        myAgent.addBehaviour(new DeliveryContractNetInitiatorBehaviour(
                            myAgent, cfp, shoppingList, deliveryFee, clientAID, conversationId));
                        
                    } else {
                        // No markets found, send failure response to client
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent("FAILURE|0||");
                        myAgent.send(reply);
                        
                        System.out.println(deliveryServiceName + ": No markets found, sent failure reply to " + clientAID.getLocalName());
                        
                        // Clean up tracking for this conversation
                        activeConversations.remove(conversationId);
                    }
                    
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                    
                    // Send failure response to client
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Error-searching-markets");
                    myAgent.send(reply);
                    
                    // Clean up tracking for this conversation
                    activeConversations.remove(conversationId);
                }
            }
        }
    }
    
    private void processPayments(MessageTemplate mt) {
        ACLMessage paymentMsg = myAgent.receive(mt);
        
        if (paymentMsg != null) {
            // Process payment
            AID clientAID = paymentMsg.getSender();
            String content = paymentMsg.getContent();
            String conversationId = paymentMsg.getConversationId();
            
            if (content.startsWith("PAYMENT:")) {
                // Extract payment amount
                double paymentAmount = Double.parseDouble(content.substring(8));
                
                System.out.println(deliveryServiceName + ": Received payment of " + 
                                   paymentAmount + " from " + clientAID.getLocalName());
                
                // Send confirmation of delivery
                ACLMessage confirmation = paymentMsg.createReply();
                confirmation.setPerformative(ACLMessage.INFORM);
                confirmation.setContent("ORDER-DELIVERED");
                myAgent.send(confirmation);
                
                System.out.println(deliveryServiceName + ": Order delivered to " + clientAID.getLocalName());
                
                // Clean up tracking for this conversation
                if (conversationId != null) {
                    activeConversations.remove(conversationId);
                }
            }
        }
    }
    
    private void processRejections(MessageTemplate mt) {
        ACLMessage rejectMsg = myAgent.receive(mt);
        
        if (rejectMsg != null) {
            // Process rejection
            AID clientAID = rejectMsg.getSender();
            String conversationId = rejectMsg.getConversationId();
            
            System.out.println(deliveryServiceName + ": Proposal rejected by " + clientAID.getLocalName());
            
            // Clean up tracking for this conversation
            if (conversationId != null) {
                activeConversations.remove(conversationId);
            }
        }
    }
} 