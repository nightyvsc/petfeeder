package com.petfeeder;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

public class MQTTManager {
    
    private MqttClient client;
    private String brokerUrl;
    private String clientId;
    private DataStorage storage;
    private FeedingScheduler scheduler;
    
    // Tópicos
    private static final String TOPIC_BOWL = "petfeeder/sensors/bowl";
    private static final String TOPIC_HOPPER = "petfeeder/sensors/hopper";
    private static final String TOPIC_ACTUATOR_STATUS = "petfeeder/actuator/status";
    private static final String TOPIC_FEED_COMMAND = "petfeeder/commands/feed";
    
    public MQTTManager(String brokerUrl, String clientId, DataStorage storage, FeedingScheduler scheduler) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.storage = storage;
        this.scheduler = scheduler;
    }
    
    public void connect() throws MqttException {
        client = new MqttClient(brokerUrl, clientId);
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        
        // Configurar callback para mensajes
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("⚠ Conexión MQTT perdida: " + cause.getMessage());
            }
            
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                handleMessage(topic, new String(message.getPayload()));
            }
            
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // No necesario para suscriptor
            }
        });
        
        System.out.println("Conectando a MQTT broker: " + brokerUrl);
        client.connect(options);
        System.out.println("✓ Conectado a MQTT");
    }
    
    public void subscribeToSensors() throws MqttException {
        client.subscribe(TOPIC_BOWL);
        System.out.println("✓ Suscrito a: " + TOPIC_BOWL);
        
        client.subscribe(TOPIC_HOPPER);
        System.out.println("✓ Suscrito a: " + TOPIC_HOPPER);
        
        client.subscribe(TOPIC_ACTUATOR_STATUS);
        System.out.println("✓ Suscrito a: " + TOPIC_ACTUATOR_STATUS);
    }
    
    private void handleMessage(String topic, String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            
            System.out.println("\n[MQTT] " + topic);
            System.out.println("  " + payload);
            
            // Almacenar datos
            storage.saveData(topic, json);
            
            // Procesar según el tópico
            switch (topic) {
                case TOPIC_BOWL:
                    processBowlData(json);
                    break;
                    
                case TOPIC_HOPPER:
                    processHopperData(json);
                    break;
                    
                case TOPIC_ACTUATOR_STATUS:
                    processActuatorStatus(json);
                    break;
            }
            
            // Verificar si es hora de alimentar
            checkFeedingSchedule();
            
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
        }
    }
    
    private void processBowlData(JSONObject data) {
        String status = data.getString("status");
        double distance = data.getDouble("distance");
        
        storage.setLastBowlStatus(status);
        
        // Detectar si la mascota comió
        if (status.equals("empty") && storage.wasPreviouslyFull()) {
            System.out.println("  🐕 La mascota ha comido!");
            scheduler.recordEatingEvent();
        }
        
        storage.updateBowlHistory(status);
    }
    
    private void processHopperData(JSONObject data) {
        int level = data.getInt("level");
        String status = data.getString("status");
        
        storage.setLastHopperLevel(level);
        
        // Alerta si nivel bajo
        if (status.equals("low")) {
            System.out.println("  ⚠ ALERTA: Nivel de hopper bajo (" + level + "%)");
        }
    }
    
    private void processActuatorStatus(JSONObject data) {
        String action = data.getString("action");
        
        if (action.equals("feed_completed")) {
            System.out.println("  ✓ Alimentación completada");
            scheduler.recordFeedingEvent();
        }
    }
    
    private boolean canFeedSafely() {
        // Verificar condiciones de seguridad
        String bowlStatus = storage.getLastBowlStatus();
        int hopperLevel = storage.getLastHopperLevel();
        
        if (hopperLevel < 10) {
            System.out.println("  ✗ No se puede alimentar: hopper vacío");
            return false;
        }
        
        if (bowlStatus.equals("full")) {
            System.out.println("  ✗ No se puede alimentar: plato ya tiene comida");
            return false;
        }
        
        return true;
    }
    
    public void sendFeedCommand() {
        try {
            MqttMessage message = new MqttMessage("FEED".getBytes());
            message.setQos(1);
            client.publish(TOPIC_FEED_COMMAND, message);
            System.out.println("  → Comando FEED enviado");
        } catch (MqttException e) {
            System.err.println("Error enviando comando: " + e.getMessage());
        }
    }
    
    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            client.close();
            System.out.println("✓ Desconectado de MQTT");
        }
    }
}