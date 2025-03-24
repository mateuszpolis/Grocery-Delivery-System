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

import java.util.Map;

/**
 * DeliveryAgent represents a delivery service that connects clients with markets
 * and delivers groceries to clients.
 */
public class DeliveryAgent extends Agent {
    private String deliveryServiceName;
    private double deliveryFee;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        
        if (args != null && args.length > 0 && args[0] instanceof Map) {
            Map<String, Object> params = (Map<String, Object>) args[0];
            
            // Extract parameters
            this.deliveryServiceName = (String) params.get("name");
            this.deliveryFee = (Double) params.get("fee");
            
            System.out.println(deliveryServiceName + " started with delivery fee: " + deliveryFee);
            
            // Register in the DF
            registerInDF();
            
            // Add behavior to handle messages from clients
            addBehaviour(new ClientRequestsServer());
            
        } else {
            System.out.println("DeliveryAgent requires parameters to start!");
            doDelete();
        }
    }
    
    /**
     * Register the delivery service in the Directory Facilitator (DF)
     */
    private void registerInDF() {
        try {
            // Create agent description
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            
            // Delivery service description
            ServiceDescription sd = new ServiceDescription();
            sd.setType("grocery-delivery");
            sd.setName(deliveryServiceName);
            
            // Add delivery fee as a property
            Property feeProp = new Property("delivery-fee", String.valueOf(deliveryFee));
            sd.addProperties(feeProp);
            
            dfd.addServices(sd);
            
            // Register the service in the DF
            DFService.register(this, dfd);
            
            System.out.println(deliveryServiceName + " registered in DF as a grocery delivery service");
            
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    /**
     * Behavior to handle requests from ClientAgents
     */
    private class ClientRequestsServer extends CyclicBehaviour {
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
    
    @Override
    protected void takeDown() {
        // Deregister from the DF
        try {
            DFService.deregister(this);
            System.out.println(deliveryServiceName + " deregistered from DF");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        System.out.println(deliveryServiceName + " terminated.");
    }
} 