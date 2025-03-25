package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.agents.DeliveryAgent;
import com.example.grocerydelivery.utils.LoggerUtil;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.logging.log4j.Logger;

/**
 * Behavior to handle requests from ClientAgents for the DeliveryAgent.
 * Processes order requests and returns appropriate responses.
 */
public class DeliveryClientRequestsServerBehaviour extends CyclicBehaviour {
    
    private final Logger logger;
    
    public DeliveryClientRequestsServerBehaviour(DeliveryAgent agent, String serviceName) {
        super(agent);
        this.logger = LoggerUtil.getLogger(
            "DeliveryClientRequests_" + serviceName, "Behaviour");
        logger.info("DeliveryClientRequestsServerBehaviour initialized for {}", serviceName);
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
                String clientName = msg.getSender().getLocalName();
                logger.info("Received order request from {}: {}", clientName, content);
                
                // For demonstration, just acknowledge the order
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("Order-Received");
                
                myAgent.send(reply);
                logger.debug("Sent acknowledgment to client: {}", clientName);
                
                // Forward the message to DeliveryOrderProcessingBehaviour by passing it back to agent
                // Create a copy of the message with the same content and conversation ID
                ACLMessage forwardMsg = new ACLMessage(ACLMessage.REQUEST);
                forwardMsg.addReceiver(myAgent.getAID()); // Send to myself
                forwardMsg.setContent(content);
                forwardMsg.setSender(msg.getSender());
                
                // Create a special conversation ID to differentiate forwarded messages
                String originalConvId = msg.getConversationId() != null ? msg.getConversationId() : "unknown";
                forwardMsg.setConversationId("forwarded-" + originalConvId);
                
                // Send the message back to the agent for processing by DeliveryOrderProcessingBehaviour
                myAgent.send(forwardMsg);
                logger.debug("Forwarded order request to DeliveryOrderProcessingBehaviour");
                
                // Detailed order processing will be implemented in future tasks
                
            } catch (Exception e) {
                logger.error("Error processing message", e);
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