package com.petfeeder;

import org.json.JSONObject;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DataStorage {
    
    private Connection connection;
    
    // Estado actual en memoria
    private String lastBowlStatus = "unknown";
    private String previousBowlStatus = "unknown";
    private int lastHopperLevel = 0;
    private boolean hopperAlertSent = false;  // AGREGAR
    
    // Configuración de MySQL
    private static final String DB_URL = "jdbc:mysql://localhost:3306/petfeeder";
    private static final String DB_USER = "petfeeder";  // Cambia esto
    private static final String DB_PASSWORD = "petfeeder";  // Cambia esto
    
    public DataStorage(String filename) {  // filename ya no se usa, pero mantenemos firma
        connectToDatabase();
    }
    
    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("✓ Conectado a MySQL");
        } catch (Exception e) {
            System.err.println("✗ Error conectando a MySQL: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void saveData(String topic, JSONObject data) {
        try {
            String sql = "INSERT INTO sensor_data (timestamp, topic, device, distance, status, level, data_json) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            
            // Timestamp actual
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, topic);
            
            // Extraer campos del JSON
            stmt.setString(3, data.optString("device", null));
            stmt.setDouble(4, data.optDouble("distance", -1));
            stmt.setString(5, data.optString("status", null));
            stmt.setInt(6, data.optInt("level", -1));
            stmt.setString(7, data.toString());
            
            stmt.executeUpdate();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error guardando datos: " + e.getMessage());
        }
    }
    
    public void recordFeedingEvent(String eventType) {
        try {
            String sql = "INSERT INTO feeding_events (event_type, timestamp) VALUES (?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, eventType);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error guardando evento: " + e.getMessage());
        }
    }
    
    // Métodos de estado (sin cambios)
    public String getLastBowlStatus() {
        return lastBowlStatus;
    }
    
    public void setLastBowlStatus(String status) {
        this.previousBowlStatus = this.lastBowlStatus;
        this.lastBowlStatus = status;
    }
    
    public boolean wasPreviouslyFull() {
        return previousBowlStatus.equals("full");
    }
    
    public void updateBowlHistory(String status) {
        // Ya no necesario
    }
    
    public int getLastHopperLevel() {
        return lastHopperLevel;
    }
    
    public void setLastHopperLevel(int level) {
        this.lastHopperLevel = level;
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✓ Conexión MySQL cerrada");
            }
        } catch (SQLException e) {
            System.err.println("Error cerrando conexión: " + e.getMessage());
        }
    }
    public boolean shouldSendHopperAlert(int level) {
    	if (level < 20 && !hopperAlertSent) {
           hopperAlertSent = true;
           return true;  // Enviar alerta
	 }
	else if (level >= 20) {
       	   hopperAlertSent = false;  // Reset cuando sube de nivel
    	}
    	return false;  // No enviar
    }
}
