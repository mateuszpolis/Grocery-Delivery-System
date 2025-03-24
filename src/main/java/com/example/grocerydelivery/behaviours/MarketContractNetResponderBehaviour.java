package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.agents.MarketAgent;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;

import java.util.HashMap;
import java.util.Map;

/**
 * Behavior to handle Contract Net Protocol requests from DeliveryAgents.
 * The Market responds with prices for the requested items.
 */
public class MarketContractNetResponderBehaviour extends ContractNetResponder {

    private final MarketAgent marketAgent;
    
    public MarketContractNetResponderBehaviour(Agent agent, MessageTemplate template) {
        super(agent, template);
        this.marketAgent = (MarketAgent) agent;
    }
    
    /**
     * Creates a standard template for Contract Net Protocol
     */
    public static MessageTemplate createMessageTemplate() {
        return MessageTemplate.and(
            MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
            MessageTemplate.MatchPerformative(ACLMessage.CFP)
        );
    }

    @Override
    protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
        marketAgent.log("Received CFP from " + cfp.getSender().getLocalName());
        
        // Extract order from the message
        String content = cfp.getContent();
        String[] requestedItems = content.split(",");
        
        // Calculate available items and total price
        Map<String, Double> availableItems = new HashMap<>();
        double totalPrice = 0.0;
        int availableCount = 0;
        
        for (String item : requestedItems) {
            String trimmedItem = item.trim();
            Double price = marketAgent.getPrice(trimmedItem);
            if (price != null) {
                availableItems.put(trimmedItem, price);
                totalPrice += price;
                availableCount++;
            }
        }
        
        // Prepare response
        ACLMessage reply = cfp.createReply();
        
        if (availableCount == 0) {
            // If no items are available, refuse the proposal
            marketAgent.log("Refusing proposal - no items available");
            reply.setPerformative(ACLMessage.REFUSE);
            reply.setContent("no-items-available");
        } else {
            // Propose the available items and their total price
            marketAgent.log("Proposing " + availableCount + " items, total price: " + totalPrice);
            reply.setPerformative(ACLMessage.PROPOSE);
            
            // Format: availableCount|totalPrice|item1:price1,item2:price2,...
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(availableCount).append("|");
            contentBuilder.append(totalPrice).append("|");
            
            for (Map.Entry<String, Double> entry : availableItems.entrySet()) {
                contentBuilder.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
            }
            
            // Remove trailing comma
            if (!availableItems.isEmpty()) {
                contentBuilder.deleteCharAt(contentBuilder.length() - 1);
            }
            
            reply.setContent(contentBuilder.toString());
        }
        
        return reply;
    }

    @Override
    protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
        marketAgent.log("Proposal accepted by " + accept.getSender().getLocalName());
        
        // Extract items to be delivered from the acceptance message
        String itemList = accept.getContent();
        
        // Process the order - simulate successful processing for this example
        marketAgent.log("Processing order: " + itemList);
        
        // Send confirmation that items are ready for delivery
        ACLMessage inform = accept.createReply();
        inform.setPerformative(ACLMessage.INFORM);
        inform.setContent("items-ready");
        
        return inform;
    }

    @Override
    protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
        marketAgent.log("Proposal rejected by " + reject.getSender().getLocalName());
    }
} 