package com.example.grocerydelivery.agents;

import com.example.grocerydelivery.behaviours.ClientFindDeliveryServicesBehaviour;
import com.example.grocerydelivery.behaviours.ClientOrderBehaviour;
import com.example.grocerydelivery.behaviours.ClientWaitBehaviour;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ClientAgent represents a client who wants to order groceries 
 * and have them delivered to their home.
 */
public class ClientAgent extends Agent {
    private String clientName;
    private String[] shoppingList;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        
        if (args != null && args.length > 0 && args[0] instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) args[0];
            
            // Extract parameters
            this.clientName = (String) params.get("name");
            this.shoppingList = (String[]) params.get("shoppingList");
            
            System.out.println(clientName + " started with shopping list: " + Arrays.toString(shoppingList));
            
            // Add delay to ensure other agents are registered
            addBehaviour(new ClientWaitBehaviour(this, clientName, 
                // Find available delivery services and start the ordering process
                new ClientFindDeliveryServicesBehaviour(this, clientName, shoppingList) {
                    @Override
                    public int onEnd() {
                        // After finding delivery services, start the order process
                        findAndStartOrder();
                        return super.onEnd();
                    }
                }));
            
        } else {
            System.out.println("ClientAgent requires parameters to start!");
            doDelete();
        }
    }
    
    /**
     * Finds delivery services and starts the order process
     */
    private void findAndStartOrder() {
        try {
            System.out.println(clientName + ": Finding delivery services for order...");
            
            // Search for delivery services
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("grocery-delivery");
            template.addServices(sd);
            
            DFAgentDescription[] result = DFService.search(this, template);
            
            if (result.length > 0) {
                // Convert DFAgentDescription to AID
                List<AID> deliveryAIDs = new ArrayList<>();
                for (DFAgentDescription dfd : result) {
                    deliveryAIDs.add(dfd.getName());
                }
                
                System.out.println(clientName + ": Found " + deliveryAIDs.size() + " delivery services");
                
                // Start the order process
                AID[] deliveryArray = deliveryAIDs.toArray(new AID[0]);
                addBehaviour(new ClientOrderBehaviour(this, clientName, shoppingList, deliveryArray));
                
            } else {
                System.out.println(clientName + ": No delivery services found!");
            }
            
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    
    @Override
    protected void takeDown() {
        System.out.println(clientName + " terminated.");
    }
} 