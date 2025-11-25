package com.petfeeder;

import org.json.JSONObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class N8nNotifier {
    
    private String n8nBaseUrl;
    private CloseableHttpClient httpClient;
    
    public N8nNotifier(String n8nIp) {
        this.n8nBaseUrl = "http://" + n8nIp + ":5678/webhook";
        this.httpClient = HttpClients.createDefault();
        System.out.println("✓ N8n Notifier configurado: " + n8nBaseUrl);
    }
    
    private void enviarWebhook(String path, JSONObject data) {
        new Thread(() -> {
            try {
                String url = n8nBaseUrl + "/" + path;
                
                HttpPost post = new HttpPost(url);
                post.setHeader("Content-Type", "application/json");
                post.setEntity(new StringEntity(data.toString()));
                
                CloseableHttpResponse response = httpClient.execute(post);
                int statusCode = response.getCode();
                
                if (statusCode == 200) {
                    System.out.println("  ✓ Notificación n8n enviada: " + path);
                } else {
                    System.err.println("  ✗ Error n8n: " + statusCode);
                }
                
                response.close();
                
            } catch (Exception e) {
                System.err.println("  ✗ Error enviando a n8n: " + e.getMessage());
            }
        }).start();
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
        
        System.out.println("  → Enviando notificación n8n...");
        enviarWebhook("alimentacion", data);
    }
    
    public void alertaHopperBajo(int nivel) {
        JSONObject data = new JSONObject();
        data.put("nivel", nivel);
        data.put("timestamp", obtenerTimestamp());
        
        System.out.println("  → Enviando alerta n8n...");
        enviarWebhook("alerta-hopper", data);
    }
    
    public void notificarMascotaComio() {
        JSONObject data = new JSONObject();
        data.put("timestamp", obtenerTimestamp());
        
        System.out.println("  → Enviando notificación n8n...");
        enviarWebhook("mascota-comio", data);
    }
}
