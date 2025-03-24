package com.example.grocerydelivery.agents;

import com.example.grocerydelivery.behaviours.ClientFindDeliveryServicesBehaviour;
import com.example.grocerydelivery.behaviours.ClientWaitBehaviour;
import jade.core.Agent;

import java.util.Arrays;
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
            
            // Create the find delivery services behavior
            ClientFindDeliveryServicesBehaviour findDeliveryServicesBehaviour = 
                new ClientFindDeliveryServicesBehaviour(this, clientName, shoppingList);
            
            // Add delay to ensure other agents are registered
            addBehaviour(new ClientWaitBehaviour(this, clientName, findDeliveryServicesBehaviour));
            
        } else {
            System.out.println("ClientAgent requires parameters to start!");
            doDelete();
        }
    }
    
    @Override
    protected void takeDown() {
        System.out.println(clientName + " terminated.");
    }
} 