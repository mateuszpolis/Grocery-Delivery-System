package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.utils.LoggerUtil;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Behavior for ClientAgent to handle the entire order process:
 * 1. Send order requests to delivery services
 * 2. Receive and evaluate proposals
 * 3. Select the best delivery service
 * 4. Send payment and await delivery confirmation
 */
public class ClientOrderBehaviour extends Behaviour {
    
    private static final int STATE_SEND_REQUESTS = 0;
    private static final int STATE_COLLECT_PROPOSALS = 1;
    private static final int STATE_SELECT_DELIVERY = 2;
    private static final int STATE_WAIT_CONFIRMATION = 3;
    private static final int STATE_DONE = 4;
    
    private final String clientName;
    private final String[] shoppingList;
    private final AID[] deliveryServices;
    private final String conversationId;
    private final Logger logger;
    
    private int state = STATE_SEND_REQUESTS;
    private int numResponses = 0;
    private boolean done = false;
    
    // Store delivery service proposals
    private final Map<AID, DeliveryProposal> proposals = new HashMap<>();
    private AID selectedDeliveryService = null;
    
    public ClientOrderBehaviour(Agent agent, String clientName, String[] shoppingList, AID[] deliveryServices) {
        super(agent);
        this.clientName = clientName;
        this.shoppingList = shoppingList;
        this.deliveryServices = deliveryServices;
        this.conversationId = "order-" + UUID.randomUUID().toString();
        this.logger = LoggerUtil.getLogger(
            "ClientOrder_" + clientName, "Behaviour");
    }
    
    @Override
    public void action() {
        switch (state) {
            case STATE_SEND_REQUESTS:
                // Send order requests to all delivery services
                sendOrderRequests();
                state = STATE_COLLECT_PROPOSALS;
                break;
                
            case STATE_COLLECT_PROPOSALS:
                // Collect proposals from delivery services
                collectProposals();
                if (numResponses >= deliveryServices.length) {
                    state = STATE_SELECT_DELIVERY;
                }
                break;
                
            case STATE_SELECT_DELIVERY:
                // Select the best delivery service
                selectDeliveryService();
                break;
                
            case STATE_WAIT_CONFIRMATION:
                // Wait for order confirmation
                waitForConfirmation();
                break;
                
            case STATE_DONE:
                done = true;
                break;
        }
    }
    
    private void sendOrderRequests() {
        logger.info("{}: Sending order requests to {} delivery services", 
                   clientName, deliveryServices.length);
        
        // Create request message
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        
        // Add all delivery services as receivers
        for (AID deliveryService : deliveryServices) {
            request.addReceiver(deliveryService);
        }
        
        // Set the content to the shopping list
        request.setContent(String.join(",", shoppingList));
        
        // Set conversation ID
        request.setConversationId(conversationId);
        
        // Send the message
        myAgent.send(request);
    }
    
    private void collectProposals() {
        // Template to match proposals for our conversation
        MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchConversationId(conversationId),
            MessageTemplate.MatchPerformative(ACLMessage.PROPOSE)
        );
        
        // Check for responses
        ACLMessage response = myAgent.receive(mt);
        
        if (response != null) {
            // Process the proposal
            AID sender = response.getSender();
            String content = response.getContent();
            
            // Parse the proposal content (format: STATUS|totalPrice|item1:price1,item2:price2,...|unavailable1,unavailable2,...)
            DeliveryProposal proposal = parseProposal(content);
            proposals.put(sender, proposal);
            
            logger.info("{}: Received proposal from {} - Status: {}, Total price: {}", 
                       clientName, sender.getLocalName(), proposal.status, proposal.totalPrice);
            
            numResponses++;
        } else {
            block();
        }
    }
    
    private DeliveryProposal parseProposal(String content) {
        DeliveryProposal proposal = new DeliveryProposal();
        
        String[] parts = content.split("\\|", 4);
        
        // Parse status
        proposal.status = parts[0];
        
        // Parse total price
        if (parts.length > 1 && !parts[1].isEmpty()) {
            proposal.totalPrice = Double.parseDouble(parts[1]);
        }
        
        // Parse available items and prices
        if (parts.length > 2 && !parts[2].isEmpty()) {
            for (String itemPrice : parts[2].split(",")) {
                String[] itemParts = itemPrice.split(":");
                if (itemParts.length == 2) {
                    String item = itemParts[0];
                    double price = Double.parseDouble(itemParts[1]);
                    proposal.availableItems.put(item, price);
                }
            }
        }
        
        // Parse unavailable items
        if (parts.length > 3 && !parts[3].isEmpty()) {
            proposal.unavailableItems.addAll(Arrays.asList(parts[3].split(",")));
        }
        
        return proposal;
    }
    
    private void selectDeliveryService() {
        logger.info("{}: Selecting best delivery service from {} proposals", 
                   clientName, proposals.size());
        
        // Log all proposals for debugging
        for (Map.Entry<AID, DeliveryProposal> entry : proposals.entrySet()) {
            AID deliveryService = entry.getKey();
            DeliveryProposal proposal = entry.getValue();
            
            logger.info("{}: Proposal from {} - Status: {}, Total price: {}, Available items: {}, Unavailable items: {}", 
                       clientName, deliveryService.getLocalName(), proposal.status, proposal.totalPrice, 
                       proposal.availableItems.keySet(), proposal.unavailableItems);
        }
        
        // Select the best delivery service based on completeness and price
        AID bestDeliveryService = null;
        String bestStatus = "FAILURE";
        double bestPrice = Double.MAX_VALUE;
        
        for (Map.Entry<AID, DeliveryProposal> entry : proposals.entrySet()) {
            AID deliveryService = entry.getKey();
            DeliveryProposal proposal = entry.getValue();
            
            // First priority: Status (SUCCESS > FAILURE)
            if ("SUCCESS".equals(proposal.status) && !"SUCCESS".equals(bestStatus)) {
                logger.info("{}: Found better status - {} with status {}", 
                           clientName, deliveryService.getLocalName(), proposal.status);
                bestDeliveryService = deliveryService;
                bestStatus = proposal.status;
                bestPrice = proposal.totalPrice;
            }
            // If same status, select based on price
            else if (proposal.status.equals(bestStatus) && proposal.totalPrice < bestPrice) {
                logger.info("{}: Found better price - {} with price {} (previous best: {})", 
                           clientName, deliveryService.getLocalName(), proposal.totalPrice, bestPrice);
                bestDeliveryService = deliveryService;
                bestPrice = proposal.totalPrice;
            }
        }
        
        if (bestDeliveryService != null && "SUCCESS".equals(bestStatus)) {
            // Store the selected delivery service
            selectedDeliveryService = bestDeliveryService;
            
            // Send acceptance to the selected delivery service
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.addReceiver(selectedDeliveryService);
            accept.setConversationId(conversationId);
            accept.setContent("PAYMENT:" + bestPrice); // Simulating payment
            
            myAgent.send(accept);
            
            logger.info("{}: Selected {} for delivery with price {}", 
                       clientName, selectedDeliveryService.getLocalName(), bestPrice);
            
            // Send rejection to other delivery services
            for (AID deliveryService : proposals.keySet()) {
                if (!deliveryService.equals(selectedDeliveryService)) {
                    ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    reject.addReceiver(deliveryService);
                    reject.setConversationId(conversationId);
                    
                    myAgent.send(reject);
                }
            }
            
            state = STATE_WAIT_CONFIRMATION;
        } else {
            logger.warn("{}: No suitable delivery service found!", clientName);
            state = STATE_DONE;
        }
    }
    
    private void waitForConfirmation() {
        // Template to match confirmation for our conversation from the selected delivery service
        MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.and(
                MessageTemplate.MatchConversationId(conversationId),
                MessageTemplate.MatchSender(selectedDeliveryService)
            ),
            MessageTemplate.MatchPerformative(ACLMessage.INFORM)
        );
        
        // Check for confirmation
        ACLMessage confirmation = myAgent.receive(mt);
        
        if (confirmation != null) {
            // Process the confirmation
            String content = confirmation.getContent();
            
            logger.info("{}: Received confirmation from {}: {}", 
                       clientName, selectedDeliveryService.getLocalName(), content);
            
            // Order process complete
            state = STATE_DONE;
        } else {
            block();
        }
    }
    
    @Override
    public boolean done() {
        return done;
    }
    
    /**
     * Internal class to store delivery service proposals
     */
    private static class DeliveryProposal {
        String status = "FAILURE"; // SUCCESS or FAILURE
        double totalPrice = 0.0;
        Map<String, Double> availableItems = new HashMap<>();
        Set<String> unavailableItems = new HashSet<>();
    }
} 