package com.example.grocerydelivery.config;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Loads agent configuration from a JSON file
 */
public class ConfigLoader {
    
    private JSONObject config;
    
    /**
     * Load the config from a JSON file
     * @param configPath Path to the config file
     * @throws IOException If file can't be read
     * @throws ParseException If JSON is invalid
     */
    public ConfigLoader(String configPath) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        this.config = (JSONObject) parser.parse(new FileReader(configPath));
    }
    
    /**
     * Get all market configurations
     * @return List of market config maps
     */
    public List<Map<String, Object>> getMarkets() {
        List<Map<String, Object>> markets = new ArrayList<>();
        
        JSONArray marketsArray = (JSONArray) config.get("markets");
        for (Object marketObj : marketsArray) {
            JSONObject market = (JSONObject) marketObj;
            Map<String, Object> marketConfig = new HashMap<>();
            
            // Add name
            String name = (String) market.get("name");
            marketConfig.put("name", name);
            
            // Add inventory
            JSONArray inventoryArray = (JSONArray) market.get("inventory");
            String[] inventory = new String[inventoryArray.size()];
            for (int i = 0; i < inventoryArray.size(); i++) {
                inventory[i] = (String) inventoryArray.get(i);
            }
            marketConfig.put("inventory", inventory);
            
            // Add prices
            JSONObject pricesObj = (JSONObject) market.get("prices");
            Object[][] prices = new Object[pricesObj.size()][2];
            int i = 0;
            for (Object key : pricesObj.keySet()) {
                String item = (String) key;
                Double price = ((Number) pricesObj.get(item)).doubleValue();
                prices[i][0] = item;
                prices[i][1] = price;
                i++;
            }
            marketConfig.put("prices", prices);
            
            markets.add(marketConfig);
        }
        
        return markets;
    }
    
    /**
     * Get all delivery service configurations
     * @return List of delivery service config maps
     */
    public List<Map<String, Object>> getDeliveryServices() {
        List<Map<String, Object>> deliveries = new ArrayList<>();
        
        JSONArray servicesArray = (JSONArray) config.get("deliveryServices");
        for (Object serviceObj : servicesArray) {
            JSONObject service = (JSONObject) serviceObj;
            Map<String, Object> serviceConfig = new HashMap<>();
            
            // Add name
            String name = (String) service.get("name");
            serviceConfig.put("name", name);
            
            // Add fee
            double fee = ((Number) service.get("fee")).doubleValue();
            serviceConfig.put("fee", fee);
            
            // Add connected markets
            JSONArray marketsArray = (JSONArray) service.get("connectedMarkets");
            String[] markets = new String[marketsArray.size()];
            for (int i = 0; i < marketsArray.size(); i++) {
                markets[i] = (String) marketsArray.get(i);
            }
            serviceConfig.put("connectedMarkets", markets);
            
            deliveries.add(serviceConfig);
        }
        
        return deliveries;
    }
    
    /**
     * Get all client configurations
     * @return List of client config maps
     */
    public List<Map<String, Object>> getClients() {
        List<Map<String, Object>> clients = new ArrayList<>();
        
        JSONArray clientsArray = (JSONArray) config.get("clients");
        for (Object clientObj : clientsArray) {
            JSONObject client = (JSONObject) clientObj;
            Map<String, Object> clientConfig = new HashMap<>();
            
            // Add name
            String name = (String) client.get("name");
            clientConfig.put("name", name);
            
            // Add shopping list
            JSONArray listArray = (JSONArray) client.get("shoppingList");
            String[] shoppingList = new String[listArray.size()];
            for (int i = 0; i < listArray.size(); i++) {
                shoppingList[i] = (String) listArray.get(i);
            }
            clientConfig.put("shoppingList", shoppingList);
            
            clients.add(clientConfig);
        }
        
        return clients;
    }
} 