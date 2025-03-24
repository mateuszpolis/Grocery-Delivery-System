package com.example.grocerydelivery.behaviours;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

/**
 * Behavior for a ClientAgent to wait before searching for delivery services.
 * This ensures all other agents are properly registered in the DF.
 */
public class ClientWaitBehaviour extends OneShotBehaviour {
    
    private final String clientName;
    private final Agent agent;
    private final ClientFindDeliveryServicesBehaviour nextBehaviour;
    
    public ClientWaitBehaviour(Agent agent, String clientName, ClientFindDeliveryServicesBehaviour nextBehaviour) {
        this.agent = agent;
        this.clientName = clientName;
        this.nextBehaviour = nextBehaviour;
    }
    
    @Override
    public void action() {
        try {
            System.out.println(clientName + " waiting for delivery services to register...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public int onEnd() {
        // Search for delivery services after delay
        agent.addBehaviour(nextBehaviour);
        return super.onEnd();
    }
} 