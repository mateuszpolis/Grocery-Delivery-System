package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.agents.MarketAgent;
import com.example.grocerydelivery.utils.LoggerUtil;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Behavior to handle Contract Net Protocol requests from DeliveryAgents.
 * The Market responds with prices for the requested items.
 */
public class MarketContractNetResponderBehaviour extends ContractNetResponder {

    private final MarketAgent marketAgent;
    private final Logger logger;
    
    public MarketContractNetResponderBehaviour(Agent agent, MessageTemplate template) {
        super(agent, template);
        this.marketAgent = (MarketAgent) agent;
        this.logger = LoggerUtil.getLogger(
            "MarketContractNet_" + marketAgent.getMarketName(), "Behaviour");
        logger.info("MarketContractNetResponderBehaviour initialized for {}", 
                   marketAgent.getMarketName());
    }
    
    /**
     * Creates a standard template for Contract Net Protocol
     */
    public static MessageTemplate createMessageTemplate() {
        return MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
    }

    @Override
    protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
        // First check if this is indeed a CFP message
        if (cfp.getPerformative() != ACLMessage.CFP) {
            logger.warn("Received non-CFP message in handleCfp: {} from {}", 
                       ACLMessage.getPerformative(cfp.getPerformative()), cfp.getSender().getLocalName());
            // Instead of throwing an exception, just return a not-understood message
            ACLMessage notUnderstood = cfp.createReply();
            notUnderstood.setPerformative(ACLMessage.NOT_UNDERSTOOD);
            return notUnderstood;
        }
        
        String conversationId = cfp.getConversationId();
        String clientReference = cfp.getReplyWith(); // Get the original client conversation ID if available
        
        logger.info("Received CFP from {} (conversation: {}, client reference: {})", 
                   cfp.getSender().getLocalName(), conversationId, clientReference);
        
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
                logger.debug("Item available: {} at price {}", trimmedItem, price);
            } else {
                logger.debug("Item not available: {}", trimmedItem);
            }
        }
        
        // Prepare response
        ACLMessage reply = cfp.createReply();
        
        // Preserve the client reference if available
        if (clientReference != null && !clientReference.isEmpty()) {
            reply.setInReplyTo(clientReference);
        }
        
        if (availableCount == 0) {
            // If no items are available, refuse the proposal
            logger.info("Refusing proposal - no items available (conversation: {})", 
                       conversationId);
            reply.setPerformative(ACLMessage.REFUSE);
            reply.setContent("no-items-available");
        } else {
            // Propose the available items and their total price
            logger.info("Proposing {} items, total price: {} (conversation: {})", 
                       availableCount, totalPrice, conversationId);
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
        String conversationId = accept.getConversationId();
        String clientReference = accept.getInReplyTo(); // Get the client reference if available
        
        logger.info("Proposal accepted by {} (conversation: {}, client reference: {})", 
                   accept.getSender().getLocalName(), conversationId, clientReference);
        
        // Extract items to be delivered from the acceptance message
        String itemList = accept.getContent();
        
        // Process the order - simulate successful processing for this example
        logger.info("Processing order: {} (conversation: {})", 
                   itemList, conversationId);
        
        // Send confirmation that items are ready for delivery
        ACLMessage inform = accept.createReply();
        inform.setPerformative(ACLMessage.INFORM);
        inform.setContent("items-ready");
        
        // Preserve the client reference if available
        if (clientReference != null && !clientReference.isEmpty()) {
            inform.setInReplyTo(clientReference);
        }
        
        return inform;
    }

    @Override
    protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
        String conversationId = reject.getConversationId();
        String clientReference = reject.getInReplyTo(); // Get the client reference if available
        
        logger.info("Proposal rejected by {} (conversation: {}, client reference: {})", 
                   reject.getSender().getLocalName(), conversationId, clientReference);
    }
} 