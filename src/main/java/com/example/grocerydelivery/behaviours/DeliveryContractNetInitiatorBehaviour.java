package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.agents.DeliveryAgent;
import com.example.grocerydelivery.utils.LoggerUtil;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Behavior for DeliveryAgent to initiate Contract Net Protocol with MarketAgents
 * to find the best prices for a grocery order.
 */
public class DeliveryContractNetInitiatorBehaviour extends ContractNetInitiator {

    private final String[] shoppingList;
    private final double deliveryFee;
    private final AID clientAID;
    private final String clientConversationId;
    private final Logger logger;
    
    // Price calculation results
    private double bestTotalPrice = Double.MAX_VALUE;
    private List<Map.Entry<AID, String>> selectedMarkets = new ArrayList<>();
    private Map<String, Double> finalItemPrices = new HashMap<>();
    private Set<String> unavailableItems = new HashSet<>();
    
    public DeliveryContractNetInitiatorBehaviour(Agent agent, ACLMessage cfp, 
                                                String[] shoppingList, 
                                                double deliveryFee, 
                                                AID clientAID,
                                                String conversationId) {
        super(agent, cfp);
        this.shoppingList = shoppingList;
        this.deliveryFee = deliveryFee;
        this.clientAID = clientAID;
        this.clientConversationId = conversationId;
        
        String deliveryName = ((DeliveryAgent)myAgent).getDeliveryServiceName();
        this.logger = LoggerUtil.getLogger(
            "DeliveryContractNet_" + deliveryName, "Behaviour");
        
        logger.info("Starting contract negotiation for client conversation {}", clientConversationId);
    }
    
    /**
     * Creates a CFP message to send to all MarketAgents
     */
    public static ACLMessage createCFP(Agent agent, AID[] marketAgents, String[] shoppingList, String conversationId) {
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        
        // Set the interaction protocol
        cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        
        // Add all markets as receivers
        for (AID market : marketAgents) {
            cfp.addReceiver(market);
        }
        
        // Set the content (comma-separated list of items)
        cfp.setContent(String.join(",", shoppingList));
        
        // Always generate a new unique conversation ID for market communications
        // Store the original client conversation ID in the reply-with field if needed for correlation
        String marketConversationId = "market-" + UUID.randomUUID().toString();
        cfp.setConversationId(marketConversationId);
        
        // Store the original client conversation ID for correlation
        if (conversationId != null && !conversationId.isEmpty()) {
            cfp.setReplyWith("client-conversation-" + conversationId);
        }
        
        // Set a reply deadline (10 seconds)
        cfp.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        
        // Get a logger for this static method
        Logger staticLogger = LoggerUtil.getLogger("DeliveryContractNet_Static", "Behaviour");
        staticLogger.debug("Created CFP message for {} markets with new conversation ID: {}, linked to client conversation: {}, protocol: {}", 
                          marketAgents.length, marketConversationId, conversationId, cfp.getProtocol());
        
        return cfp;
    }

    @Override
    @SuppressWarnings("rawtypes") // Required to match parent class signature
    protected void handlePropose(ACLMessage propose, Vector v) {
        String marketName = propose.getSender().getLocalName();
        String clientRef = propose.getInReplyTo(); // Get the original client reference if available
        
        logger.debug("Received proposal from {} (market conversation: {}, client ref: {})", 
                marketName, propose.getConversationId(), clientRef);
    }

    @Override
    protected void handleRefuse(ACLMessage refuse) {
        String marketName = refuse.getSender().getLocalName();
        String clientRef = refuse.getInReplyTo(); // Get the original client reference if available
        
        logger.debug("Received refusal from {} (market conversation: {}, client ref: {})", 
                marketName, refuse.getConversationId(), clientRef);
    }

    @Override
    protected void handleFailure(ACLMessage failure) {
        String marketName = failure.getSender().getLocalName();
        String clientRef = failure.getInReplyTo(); // Get the original client reference if available
        
        logger.warn("Transaction failure from {} (market conversation: {}, client ref: {})", 
                marketName, failure.getConversationId(), clientRef);
    }

    @Override
    protected void handleInform(ACLMessage inform) {
        String marketName = inform.getSender().getLocalName();
        String clientRef = inform.getInReplyTo(); // Get the original client reference if available
        
        logger.info("Order confirmed by {} (market conversation: {}, client ref: {})", 
                marketName, inform.getConversationId(), clientRef);
    }

    @Override
    @SuppressWarnings("rawtypes") // Required to match parent class signature
    protected void handleAllResponses(Vector responses, Vector acceptances) {
        // Process market responses according to the algorithm in the task
        // 1. Initially try to select all items from the market with the largest number available
        // 2. If multiple markets have the same count, choose the one with lowest price
        // 3. If not all items can be selected from one place, repeat for missing items
        
        logger.info("Processing {} market responses (conversation: {})", responses.size(), clientConversationId);
        
        // Add a delay to ensure all responses are collected
        if (responses.isEmpty()) {
            logger.warn("No market responses received, waiting longer for responses (conversation: {})", clientConversationId);
            try {
                Thread.sleep(2000); // Wait an additional 2 seconds for late responses
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Re-check if any responses arrived after waiting
            if (responses.isEmpty()) {
                logger.warn("Still no market responses after waiting (conversation: {})", clientConversationId);
            }
        }
        
        logger.info("Starting with algorithm: 1) Select market with most items, 2) Tie-break by price, 3) Repeat for remaining items");
        
        // Extract all available items from all markets
        Map<AID, Map<String, Double>> marketItemPrices = new HashMap<>();
        
        // Parse all proposals
        for (Object obj : responses) {
            ACLMessage response = (ACLMessage) obj;
            
            if (response.getPerformative() == ACLMessage.PROPOSE) {
                AID marketAID = response.getSender();
                String content = response.getContent();
                
                logger.debug("Parsing proposal from {}: {}", marketAID.getLocalName(), content);
                
                // Parse the proposal content (format: count|totalPrice|item1:price1,item2:price2,...)
                String[] parts = content.split("\\|", 3);
                if (parts.length == 3) {
                    String itemsList = parts[2];
                    
                    // Parse individual items
                    Map<String, Double> itemPrices = new HashMap<>();
                    for (String itemPrice : itemsList.split(",")) {
                        String[] itemParts = itemPrice.split(":");
                        if (itemParts.length == 2) {
                            String item = itemParts[0];
                            double price = Double.parseDouble(itemParts[1]);
                            itemPrices.put(item, price);
                            logger.debug("Market {} has {} for {}", marketAID.getLocalName(), item, price);
                        }
                    }
                    
                    marketItemPrices.put(marketAID, itemPrices);
                }
            }
        }
        
        // Create a list of all items needed
        Set<String> remainingItems = new HashSet<>(Arrays.asList(shoppingList));
        
        logger.info("Need to find {} items: {}", remainingItems.size(), remainingItems);
        
        // Clear results from previous runs
        selectedMarkets.clear();
        finalItemPrices.clear();
        unavailableItems.clear();
        
        // Keep selecting markets until all items are found or no more markets are available
        int iterationCount = 0;
        while (!remainingItems.isEmpty() && !marketItemPrices.isEmpty()) {
            iterationCount++;
            logger.info("Iteration {} - Finding best market for remaining items: {}", iterationCount, remainingItems);
            
            // Find market with the most available items from the remaining items list
            AID bestMarketAID = null;
            int maxAvailableItems = 0;
            double lowestTotalPrice = Double.MAX_VALUE;
            Map<String, Double> bestMarketItems = null;
            Set<String> bestMarketAvailableItems = null;
            
            // First, evaluate each market
            for (Map.Entry<AID, Map<String, Double>> entry : marketItemPrices.entrySet()) {
                AID marketAID = entry.getKey();
                Map<String, Double> marketItems = entry.getValue();
                
                // Count how many remaining items this market has
                Set<String> availableItems = new HashSet<>();
                double totalPrice = 0.0;
                
                for (String item : remainingItems) {
                    if (marketItems.containsKey(item)) {
                        availableItems.add(item);
                        totalPrice += marketItems.get(item);
                    }
                }
                
                int availableCount = availableItems.size();
                
                logger.info("Market {} has {} of the needed items for total price {}", marketAID.getLocalName(), availableCount, totalPrice);
                
                // Rule 1: Choose market with most items
                if (availableCount > maxAvailableItems) {
                    maxAvailableItems = availableCount;
                    lowestTotalPrice = totalPrice;
                    bestMarketAID = marketAID;
                    bestMarketItems = marketItems;
                    bestMarketAvailableItems = new HashSet<>(availableItems);
                    logger.info("New best market: {} with {} items (most items rule)", marketAID.getLocalName(), availableCount);
                }
                // Rule 2: If same number of items, choose cheapest
                else if (availableCount == maxAvailableItems && availableCount > 0 && totalPrice < lowestTotalPrice) {
                    lowestTotalPrice = totalPrice;
                    bestMarketAID = marketAID;
                    bestMarketItems = marketItems;
                    bestMarketAvailableItems = new HashSet<>(availableItems);
                    logger.info("New best market: {} with lower price {} (price tie-breaker rule)", marketAID.getLocalName(), totalPrice);
                }
            }
            
            // If no market has any of the remaining items, break
            if (bestMarketAID == null || maxAvailableItems == 0) {
                logger.info("No markets have the remaining items: {}", remainingItems);
                unavailableItems.addAll(remainingItems);
                break;
            }
            
            // Process the selected market
            logger.info("SELECTING market {} for {} items with total price {}", bestMarketAID.getLocalName(), bestMarketAvailableItems.size(), lowestTotalPrice);
            
            // Build the list of items to get from this market
            StringBuilder selectedItems = new StringBuilder();
            boolean first = true;
            
            for (String item : bestMarketAvailableItems) {
                // Add this item to our final selection
                Double price = bestMarketItems.get(item);
                finalItemPrices.put(item, price);
                
                // Add to the selected items string
                if (!first) {
                    selectedItems.append(",");
                }
                selectedItems.append(item);
                first = false;
                
                // Remove from remaining items
                remainingItems.remove(item);
                
                logger.info("Adding {} from {} at price {}", item, bestMarketAID.getLocalName(), price);
            }
            
            // Add this market to selected markets
            selectedMarkets.add(new AbstractMap.SimpleEntry<>(bestMarketAID, selectedItems.toString()));
            
            // Remove this market from consideration for next iterations
            marketItemPrices.remove(bestMarketAID);
            
            logger.info("Remaining items to find: {}", remainingItems);
        }
        
        // Any items still in remaining items are unavailable
        unavailableItems.addAll(remainingItems);
        
        // Calculate the total price plus delivery fee
        bestTotalPrice = finalItemPrices.values().stream().mapToDouble(Double::doubleValue).sum() + deliveryFee;
        
        // Now we'll provide feedback on partial vs. complete fulfillment
        boolean canFulfillOrder = unavailableItems.isEmpty();
        String fulfillmentStatus = canFulfillOrder ? "COMPLETE" : "PARTIAL";
        
        logger.info("====== ORDER SUMMARY ======");
        logger.info("Items found: {}", finalItemPrices.keySet());
        logger.info("Items unavailable: {}", unavailableItems);
        logger.info("Total price (incl. delivery fee): {}", bestTotalPrice);
        logger.info("Order fulfillment: {}", fulfillmentStatus);
        logger.info("Selected markets: ");
        for (Map.Entry<AID, String> market : selectedMarkets) {
            logger.info("  - {} : {}", market.getKey().getLocalName(), market.getValue());
        }
        logger.info("===============================");
        
        // Create acceptance or rejection messages for each market
        for (Object obj : responses) {
            ACLMessage response = (ACLMessage) obj;
            ACLMessage reply = response.createReply();
            
            // Find if this market is selected
            Optional<Map.Entry<AID, String>> selectedMarket = selectedMarkets.stream()
                .filter(entry -> entry.getKey().equals(response.getSender()))
                .findFirst();
            
            if (selectedMarket.isPresent() && response.getPerformative() == ACLMessage.PROPOSE) {
                // Accept this proposal
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                reply.setContent(selectedMarket.get().getValue()); // The items we want
                logger.info("Accepting proposal from {} for items: {}", response.getSender().getLocalName(), selectedMarket.get().getValue());
            } else if (response.getPerformative() == ACLMessage.PROPOSE) {
                // Reject this proposal
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                logger.info("Rejecting proposal from {}", response.getSender().getLocalName());
            }
            
            // Add reply to acceptances Vector - using @SuppressWarnings for raw type
            @SuppressWarnings("unchecked")
            Vector<ACLMessage> typedAcceptances = acceptances;
            typedAcceptances.add(reply);
        }
        
        // After processing all responses, prepare to respond to the client
        myAgent.addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                sendProposalToClient(canFulfillOrder);
            }
        });
    }

    @Override
    @SuppressWarnings("rawtypes") // Required to match parent class signature
    protected void handleAllResultNotifications(Vector resultNotifications) {
        logger.info("All markets have processed the order");        
    }

    private void sendProposalToClient(boolean isSuccess) {
        ACLMessage proposal = new ACLMessage(ACLMessage.PROPOSE);
        proposal.addReceiver(clientAID);
        
        // Use the original client conversation ID for client communication
        proposal.setConversationId(clientConversationId);
        
        // Format the message to the client
        StringBuilder content = new StringBuilder();
        
        // Only mark SUCCESS for complete orders, FAILURE for partial orders
        if (isSuccess) {
            content.append("SUCCESS");
            logger.info("Sending SUCCESS proposal with complete order ({} items, conversation: {})", finalItemPrices.size(), clientConversationId);
        } else {
            content.append("FAILURE");
            if (!finalItemPrices.isEmpty()) {
                logger.info("Sending FAILURE proposal for partial order, found {} items but missing {} items (conversation: {})",
                        finalItemPrices.size(), unavailableItems.size(), clientConversationId);
            } else {
                logger.info("Sending FAILURE proposal, couldn't find any items (conversation: {})", clientConversationId);
            }
        }
        
        // Add total price
        content.append("|").append(bestTotalPrice);
        
        // Add available items and their prices
        content.append("|");
        if (!finalItemPrices.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Double> entry : finalItemPrices.entrySet()) {
                if (!first) {
                    content.append(",");
                }
                content.append(entry.getKey()).append(":").append(entry.getValue());
                first = false;
            }
        }
        
        // Add unavailable items
        content.append("|");
        if (!unavailableItems.isEmpty()) {
            content.append(String.join(",", unavailableItems));
        }
        
        // Set message content
        proposal.setContent(content.toString());
        
        // Send reply to client
        myAgent.send(proposal);
        logger.info("Sent proposal to client {} with total price: {} (conversation: {})", clientAID.getLocalName(), bestTotalPrice, clientConversationId);
    }
} 