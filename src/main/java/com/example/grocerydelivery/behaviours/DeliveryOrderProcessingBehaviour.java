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

import java.util.List;
import java.util.UUID;

/**
 * Behavior for DeliveryAgent to process order requests from clients and 
 * handle the complete order processing workflow.
 */
public class DeliveryOrderProcessingBehaviour extends CyclicBehaviour {
    
    private final DeliveryAgent deliveryAgent;
    private final String deliveryServiceName;
    private final double deliveryFee;
    
    public DeliveryOrderProcessingBehaviour(Agent agent, String serviceName, double fee) {
        super(agent);
        this.deliveryAgent = (DeliveryAgent) agent;
        this.deliveryServiceName = serviceName;
        this.deliveryFee = fee;
    }
    
    @Override
    public void action() {
        // Template to match order requests - fixed to correctly match REQUEST messages
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        
        // Check for messages
        ACLMessage msg = myAgent.receive(mt);
        
        if (msg != null) {
            // Process the request
            String content = msg.getContent();
            AID clientAID = msg.getSender();
            String conversationId = msg.getConversationId();
            
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
                    myAgent, marketAIDs, shoppingList, "market-" + UUID.randomUUID().toString());
                
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
                            myAgent, marketAIDs, shoppingList, "market-" + UUID.randomUUID().toString());
                        
                        myAgent.addBehaviour(new DeliveryContractNetInitiatorBehaviour(
                            myAgent, cfp, shoppingList, deliveryFee, clientAID, conversationId));
                        
                    } else {
                        // No markets found, send failure response to client
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent("FAILURE|0||");
                        myAgent.send(reply);
                        
                        System.out.println(deliveryServiceName + ": No markets found, sent failure reply to " + clientAID.getLocalName());
                    }
                    
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                    
                    // Send failure response to client
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Error-searching-markets");
                    myAgent.send(reply);
                }
            }
            
        } else {
            block();
        }
        
        // Also check for payment messages (ACCEPT_PROPOSAL)
        MessageTemplate paymentTemplate = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
        ACLMessage paymentMsg = myAgent.receive(paymentTemplate);
        
        if (paymentMsg != null) {
            // Process payment
            AID clientAID = paymentMsg.getSender();
            String content = paymentMsg.getContent();
            
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
            }
        }
        
        // Check for rejection messages
        MessageTemplate rejectTemplate = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
        ACLMessage rejectMsg = myAgent.receive(rejectTemplate);
        
        if (rejectMsg != null) {
            // Process rejection
            AID clientAID = rejectMsg.getSender();
            
            System.out.println(deliveryServiceName + ": Proposal rejected by " + clientAID.getLocalName());
        }
    }
} 