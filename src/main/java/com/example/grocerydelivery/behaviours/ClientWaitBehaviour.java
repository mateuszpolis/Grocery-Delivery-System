package com.example.grocerydelivery.behaviours;

import com.example.grocerydelivery.agents.ClientAgent;
import com.example.grocerydelivery.utils.LoggerUtil;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import org.apache.logging.log4j.Logger;

/**
 * Behavior for a ClientAgent to wait before searching for delivery services.
 * This ensures all other agents are properly registered in the DF.
 */
public class ClientWaitBehaviour extends OneShotBehaviour {
    
    private final String clientName;
    private final ClientAgent agent;
    private final Behaviour nextBehaviour;
    private final Logger logger;
    
    public ClientWaitBehaviour(Agent agent, String clientName, Behaviour nextBehaviour) {
        this.agent = (ClientAgent) agent;
        this.clientName = clientName;
        this.nextBehaviour = nextBehaviour;
        this.logger = LoggerUtil.getLogger(
            "ClientWait_" + clientName, "Behaviour");
    }
    
    @Override
    public void action() {
        try {
            logger.info("{} waiting for delivery services to register...", clientName);
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            logger.error("Wait interrupted", e);
        }
    }
    
    @Override
    public int onEnd() {
        // Search for delivery services after delay
        agent.addBehaviour(nextBehaviour);
        return super.onEnd();
    }
} 