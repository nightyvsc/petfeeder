package com.petfeeder;

import java.util.Scanner;

public class PetFeederApp {
    
    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("  Pet Feeder Control System");
        System.out.println("=================================\n");
        
        // Configuración
        String brokerUrl = "tcp://10.62.69.61:1883";  // Tu IP
        String clientId = "PetFeederJavaApp";
        String n8nIp = "10.62.69.61";
        
        try {
            // Inicializar componentes
            N8nNotifier n8n = new N8nNotifier(n8nIp);
            DataStorage storage = new DataStorage("petfeeder_data.json");
            FeedingData scheduler = new FeedingData(storage);
            MQTTManager mqttManager = new MQTTManager(brokerUrl, clientId, storage, scheduler, n8n);

            // Conectar a MQTT
            mqttManager.connect();
            
            // Suscribirse a tópicos de sensores
            mqttManager.subscribeToSensors();
            
            System.out.println("\n✓ Sistema iniciado correctamente");
            System.out.println("✓ Escuchando mensajes MQTT...");
            System.out.println("\nComandos disponibles:");
            System.out.println("  'feed'   - Alimentar manualmente");
            System.out.println("  'status' - Ver estado del sistema");
            System.out.println("  'exit'   - Salir\n");
            
            // Loop de comandos del usuario
            Scanner scanner = new Scanner(System.in);
            boolean running = true;
            
            while (running) {
                System.out.print("> ");
                String command = scanner.nextLine().trim().toLowerCase();
                
                switch (command) {
                    case "feed":
                        System.out.println("Enviando comando de alimentación manual...");
                        mqttManager.sendFeedCommand();
                        break;
                        
                    case "status":
                        System.out.println("\n--- Estado del Sistema ---");
                        System.out.println("Bowl: " + storage.getLastBowlStatus());
                        System.out.println("Hopper: " + storage.getLastHopperLevel() + "%");
                        System.out.println("Última alimentación: " + scheduler.getLastFeedingTime());
                        System.out.println("Próxima alimentación: " + scheduler.getNextFeedingTime());
                        System.out.println("--------------------------\n");
                        break;
                        
                    case "exit":
                        System.out.println("Cerrando sistema...");
                        mqttManager.disconnect();
                        running = false;
                        break;
                        
                    default:
                        System.out.println("Comando no reconocido. Usa: feed, status, exit");
                }
            }
            
            scanner.close();
            
        } catch (Exception e) {
            System.err.println("Error fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
