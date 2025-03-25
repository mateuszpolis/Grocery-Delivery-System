package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.agents.DeliveryAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.util.*;

/**
 * Behavior for DeliveryAgent to initiate Contract Net Protocol with MarketAgents
 * to find the best prices for a grocery order.
 */
public class DeliveryContractNetInitiatorBehaviour extends ContractNetInitiator {

    private final String[] shoppingList;
    private final double deliveryFee;
    private final AID clientAID;
    private final String conversationId;
    
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
        this.conversationId = conversationId;
        
        String deliveryName = ((DeliveryAgent)myAgent).getDeliveryServiceName();
        System.out.println(deliveryName + ": Starting contract negotiation for conversation " + conversationId);
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
        
        // Use the provided conversation ID to track this specific negotiation
        // If none provided, generate a new one
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = "market-" + UUID.randomUUID().toString();
        }
        cfp.setConversationId(conversationId);
        
        // Set a reply deadline (5 seconds)
        cfp.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
        
        return cfp;
    }

    @Override
    @SuppressWarnings("rawtypes") // Required to match parent class signature
    protected void handlePropose(ACLMessage propose, Vector v) {
        System.out.println(myAgent.getLocalName() + " (" + ((DeliveryAgent)myAgent).getDeliveryServiceName() + "): Received proposal from " + propose.getSender().getLocalName());
    }

    @Override
    protected void handleRefuse(ACLMessage refuse) {
        System.out.println(myAgent.getLocalName() + " (" + ((DeliveryAgent)myAgent).getDeliveryServiceName() + "): Received refusal from " + refuse.getSender().getLocalName());
    }

    @Override
    protected void handleFailure(ACLMessage failure) {
        String deliveryName = ((DeliveryAgent)myAgent).getDeliveryServiceName();
        System.out.println(deliveryName + ": Transaction failure from " + failure.getSender().getLocalName());
    }

    @Override
    protected void handleInform(ACLMessage inform) {
        String deliveryName = ((DeliveryAgent)myAgent).getDeliveryServiceName();
        System.out.println(deliveryName + ": Order confirmed by " + inform.getSender().getLocalName());
    }

    @Override
    @SuppressWarnings("rawtypes") // Required to match parent class signature
    protected void handleAllResponses(Vector responses, Vector acceptances) {
        // Process market responses according to the algorithm in the task
        // 1. Initially try to select all items from the market with the largest number available
        // 2. If multiple markets have the same count, choose the one with lowest price
        // 3. If not all items can be selected from one place, repeat for missing items
        
        String deliveryName = ((DeliveryAgent)myAgent).getDeliveryServiceName();
        System.out.println(deliveryName + ": Processing " + responses.size() + " market responses (conversation: " + conversationId + ")");
        System.out.println(deliveryName + ": Starting with algorithm: 1) Select market with most items, 2) Tie-break by price, 3) Repeat for remaining items");
        
        // Extract all available items from all markets
        Map<AID, Map<String, Double>> marketItemPrices = new HashMap<>();
        
        // Parse all proposals
        for (Object obj : responses) {
            ACLMessage response = (ACLMessage) obj;
            
            if (response.getPerformative() == ACLMessage.PROPOSE) {
                AID marketAID = response.getSender();
                String content = response.getContent();
                
                System.out.println(deliveryName + ": Parsing proposal from " + marketAID.getLocalName() + ": " + content);
                
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
                            System.out.println(deliveryName + ": Market " + marketAID.getLocalName() + " has " + item + " for " + price);
                        }
                    }
                    
                    marketItemPrices.put(marketAID, itemPrices);
                }
            }
        }
        
        // Create a list of all items needed
        Set<String> remainingItems = new HashSet<>(Arrays.asList(shoppingList));
        
        System.out.println(deliveryName + ": Need to find " + remainingItems.size() + " items: " + remainingItems);
        
        // Clear results from previous runs
        selectedMarkets.clear();
        finalItemPrices.clear();
        unavailableItems.clear();
        
        // Keep selecting markets until all items are found or no more markets are available
        int iterationCount = 0;
        while (!remainingItems.isEmpty() && !marketItemPrices.isEmpty()) {
            iterationCount++;
            System.out.println(deliveryName + ": Iteration " + iterationCount + " - Finding best market for remaining items: " + remainingItems);
            
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
                
                System.out.println(deliveryName + ": Market " + marketAID.getLocalName() + 
                                  " has " + availableCount + " of the needed items for total price " + totalPrice);
                
                // Rule 1: Choose market with most items
                if (availableCount > maxAvailableItems) {
                    maxAvailableItems = availableCount;
                    lowestTotalPrice = totalPrice;
                    bestMarketAID = marketAID;
                    bestMarketItems = marketItems;
                    bestMarketAvailableItems = new HashSet<>(availableItems);
                    System.out.println(deliveryName + ": New best market: " + marketAID.getLocalName() + 
                                      " with " + availableCount + " items (most items rule)");
                }
                // Rule 2: If same number of items, choose cheapest
                else if (availableCount == maxAvailableItems && availableCount > 0 && totalPrice < lowestTotalPrice) {
                    lowestTotalPrice = totalPrice;
                    bestMarketAID = marketAID;
                    bestMarketItems = marketItems;
                    bestMarketAvailableItems = new HashSet<>(availableItems);
                    System.out.println(deliveryName + ": New best market: " + marketAID.getLocalName() + 
                                      " with lower price " + totalPrice + " (price tie-breaker rule)");
                }
            }
            
            // If no market has any of the remaining items, break
            if (bestMarketAID == null || maxAvailableItems == 0) {
                System.out.println(deliveryName + ": No markets have the remaining items: " + remainingItems);
                unavailableItems.addAll(remainingItems);
                break;
            }
            
            // Process the selected market
            System.out.println(deliveryName + ": SELECTING market " + bestMarketAID.getLocalName() + 
                              " for " + bestMarketAvailableItems.size() + " items with total price " + lowestTotalPrice);
            
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
                
                System.out.println(deliveryName + ": Adding " + item + " from " + 
                                  bestMarketAID.getLocalName() + " at price " + price);
            }
            
            // Add this market to selected markets
            selectedMarkets.add(new AbstractMap.SimpleEntry<>(bestMarketAID, selectedItems.toString()));
            
            // Remove this market from consideration for next iterations
            marketItemPrices.remove(bestMarketAID);
            
            System.out.println(deliveryName + ": Remaining items to find: " + remainingItems);
        }
        
        // Any items still in remaining items are unavailable
        unavailableItems.addAll(remainingItems);
        
        // Calculate the total price plus delivery fee
        bestTotalPrice = finalItemPrices.values().stream().mapToDouble(Double::doubleValue).sum() + deliveryFee;
        
        // Now we'll provide feedback on partial vs. complete fulfillment
        boolean canFulfillOrder = unavailableItems.isEmpty();
        String fulfillmentStatus = canFulfillOrder ? "COMPLETE" : "PARTIAL";
        
        System.out.println("====== " + deliveryName + ": ORDER SUMMARY ======");
        System.out.println("Items found: " + finalItemPrices.keySet());
        System.out.println("Items unavailable: " + unavailableItems);
        System.out.println("Total price (incl. delivery fee): " + bestTotalPrice);
        System.out.println("Order fulfillment: " + fulfillmentStatus);
        System.out.println("Selected markets: ");
        for (Map.Entry<AID, String> market : selectedMarkets) {
            System.out.println("  - " + market.getKey().getLocalName() + ": " + market.getValue());
        }
        System.out.println("===============================");
        
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
                System.out.println(deliveryName + ": Accepting proposal from " + 
                                   response.getSender().getLocalName() + 
                                   " for items: " + selectedMarket.get().getValue());
            } else if (response.getPerformative() == ACLMessage.PROPOSE) {
                // Reject this proposal
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                System.out.println(deliveryName + ": Rejecting proposal from " + 
                                   response.getSender().getLocalName());
            }
            
            acceptances.add(reply);
        }
        
        // After processing all responses, prepare to respond to the client
        myAgent.addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage clientReply = new ACLMessage(ACLMessage.PROPOSE);
                clientReply.addReceiver(clientAID);
                clientReply.setConversationId(conversationId);
                
                // Format the message to the client
                StringBuilder content = new StringBuilder();
                
                // We'll mark SUCCESS even for partial orders, as long as we found some items
                if (!finalItemPrices.isEmpty()) {
                    content.append("SUCCESS");
                    String orderType = canFulfillOrder ? "complete" : "partial";
                    System.out.println(deliveryName + ": Sending SUCCESS proposal with " + 
                                      finalItemPrices.size() + " items (" + orderType + " order, conversation: " + conversationId + ")");
                } else {
                    content.append("FAILURE");
                    System.out.println(deliveryName + ": Sending FAILURE proposal, couldn't find any items (conversation: " + conversationId + ")");
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
                clientReply.setContent(content.toString());
                
                // Send reply to client
                myAgent.send(clientReply);
                System.out.println(deliveryName + ": Sent proposal to client " + 
                                   clientAID.getLocalName() + " with total price: " + bestTotalPrice + 
                                   " (conversation: " + conversationId + ")");
            }
        });
    }

    @Override
    @SuppressWarnings("rawtypes") // Required to match parent class signature
    protected void handleAllResultNotifications(Vector resultNotifications) {
        String deliveryName = ((DeliveryAgent)myAgent).getDeliveryServiceName();
        System.out.println(deliveryName + ": All markets have processed the order");
        
        // At this point, we've received confirmations from all selected markets
        // We could update our internal state, but for this example we'll just log it
    }
} 