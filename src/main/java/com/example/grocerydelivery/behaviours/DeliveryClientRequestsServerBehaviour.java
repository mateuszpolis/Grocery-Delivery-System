package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.agents.DeliveryAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Behavior to handle requests from ClientAgents for the DeliveryAgent.
 * Processes order requests and returns appropriate responses.
 */
public class DeliveryClientRequestsServerBehaviour extends CyclicBehaviour {
    
    private final DeliveryAgent deliveryAgent;
    private final String deliveryServiceName;
    
    public DeliveryClientRequestsServerBehaviour(DeliveryAgent agent, String serviceName) {
        this.deliveryAgent = agent;
        this.deliveryServiceName = serviceName;
    }
    
    @Override
    public void action() {
        // Listen for order requests
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        ACLMessage msg = myAgent.receive(mt);
        
        if (msg != null) {
            try {
                // Process the request
                String content = msg.getContent();
                System.out.println(deliveryServiceName + " received order request: " + content);
                
                // For demonstration, just acknowledge the order
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("Order-Received");
                
                myAgent.send(reply);
                
                // Detailed order processing will be implemented in future tasks
                
            } catch (Exception e) {
                System.out.println("Error processing message: " + e.getMessage());
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("Error-processing-request");
                myAgent.send(reply);
            }
        } else {
            block();
        }
    }
} 