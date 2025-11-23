package com.petfeeder;

import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class N8nNotifier {
    
    private String n8nBaseUrl;
    private HttpClient httpClient;
    
    public N8nNotifier(String n8nIp) {
        this.n8nBaseUrl = "http://" + n8nIp + ":5678/webhook";
        this.httpClient = HttpClient.newHttpClient();
        System.out.println("✓ N8n Notifier configurado: " + n8nBaseUrl);
    }
    
    private void enviarWebhook(String path, JSONObject data) {
        try {
            String url = n8nBaseUrl + "/" + path;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                .build();
            
            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() == 200) {
                System.out.println("  ✓ Notificación n8n enviada: " + path);
            } else {
                System.err.println("  ✗ Error n8n: " + response.statusCode());
            }
            
        } catch (Exception e) {
            System.err.println("  ✗ Error enviando a n8n: " + e.getMessage());
        }
    }
    
    private String obtenerTimestamp() {
        LocalDateTime ahora = LocalDateTime.now();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return ahora.format(formato);
    }
    
    public void notificarAlimentacion(String estadoBowl, int nivelHopper) {
        JSONObject data = new JSONObject();
        data.put("estado_bowl", estadoBowl);
        data.put("nivel_hopper", nivelHopper);
        data.put("timestamp", obtenerTimestamp());
        
        enviarWebhook("alimentacion", data);
    }
    
    public void alertaHopperBajo(int nivel) {
        JSONObject data = new JSONObject();
        data.put("nivel", nivel);
        data.put("timestamp", obtenerTimestamp());
        
        enviarWebhook("alerta-hopper", data);
    }
    
    public void notificarMascotaComio() {
        JSONObject data = new JSONObject();
        data.put("timestamp", obtenerTimestamp());
        
        enviarWebhook("mascota-comio", data);
    }
}