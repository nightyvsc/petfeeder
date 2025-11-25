#include <ESP8266WiFi.h>
#include <PubSubClient.h>

// ========== CONFIGURACIÓN WiFi y MQTT ==========
const char* ssid = "juanma";
const char* password = "janet123";
const char* mqtt_server = "10.62.69.61";

// ========== PINES DE LOS SENSORES ==========
// Sensor 1: Plato
#define TRIG_PLATO 14   // D5
#define ECHO_PLATO 12   // D6

// Sensor 2: Hopper
#define TRIG_HOPPER 5  // D1
#define ECHO_HOPPER 4  // D2

// ========== CONFIGURACIÓN SENSOR PLATO ==========
const float PLATO_UMBRAL_LLENO = 16;    // < 5cm = hay comida
const float PLATO_UMBRAL_VACIO = 21;   // > 10cm = plato vacío

// ========== CONFIGURACIÓN SENSOR HOPPER ==========
const float HOPPER_ALTURA_TOTAL = 7;  // Altura total del contenedor en cm (AJUSTA ESTE VALOR)
const float HOPPER_NIVEL_BAJO = 5;    // % bajo el cual se considera nivel crítico

// ========== VARIABLES GLOBALES ==========
WiFiClient espClient;
PubSubClient client(espClient);
unsigned long lastMsg = 0;
const long interval = 5000;  // Publicar cada 5 segundos

// ========== FUNCIÓN: Conectar WiFi ==========
void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Conectando a: ");
  Serial.println(ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi conectado!");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());
}

// ========== FUNCIÓN: Reconectar MQTT ==========
void reconnect() {
  while (!client.connected()) {
    Serial.print("Conectando a MQTT...");
    
    String clientId = "ESP8266_Sensores";
    
    if (client.connect(clientId.c_str())) {
      Serial.println("conectado!");
    } else {
      Serial.print("falló, rc=");
      Serial.print(client.state());
      Serial.println(" reintento en 5 seg");
      delay(5000);
    }
  }
}

// ========== FUNCIÓN: Leer Distancia Genérica ==========
float leerDistancia(int trigPin, int echoPin, String nombreSensor) {
  // Limpiar
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  
  // Enviar pulso
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  
  // Leer echo
  long duracion = pulseIn(echoPin, HIGH, 30000);
  
  // Debug
  Serial.print("[");
  Serial.print(nombreSensor);
  Serial.print("] Duracion: ");
  Serial.print(duracion);
  Serial.print(" us | ");
  
  if (duracion == 0) {
    Serial.println("ERROR: timeout");
    return -1;
  }
  
  // Calcular distancia
  float distancia = duracion * 0.034 / 2;
  
  if (distancia < 2 || distancia > 400) {
    Serial.print("ERROR: fuera de rango (");
    Serial.print(distancia);
    Serial.println(" cm)");
    return -1;
  }
  
  return distancia;
}

// ========== FUNCIÓN: Estado del Plato ==========
String obtenerEstadoPlato(float distancia) {
  if (distancia < 0) return "error";
  
  if (distancia < PLATO_UMBRAL_LLENO) {
    return "full";
  } else if (distancia > PLATO_UMBRAL_VACIO) {
    return "empty";
  } else {
    return "partial";
  }
}

// ========== FUNCIÓN: Nivel del Hopper (Porcentaje) ==========
int calcularNivelHopper(float distancia) {
  if (distancia < 0) return -1;  // Error
  
  // Calcular nivel: cuanto más cerca está el sensor del fondo, menos alimento hay
  // Nivel = (altura_total - distancia_medida) / altura_total * 100
  float nivelAlimento = HOPPER_ALTURA_TOTAL - distancia;
  float porcentaje = (nivelAlimento / HOPPER_ALTURA_TOTAL) * 100;
  
  // Limitar entre 0 y 100
  if (porcentaje < 0) porcentaje = 0;
  if (porcentaje > 100) porcentaje = 100;
  
  return (int)porcentaje;
}

// ========== FUNCIÓN: Estado del Hopper ==========
String obtenerEstadoHopper(int nivel) {
  if (nivel < 0) return "error";
  if (nivel < HOPPER_NIVEL_BAJO) return "low";
  if (nivel < 50) return "medium";
  return "high";
}

// ========== SETUP ==========
void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("\n\n=== DEVICE 1: SENSORES ===");
  
  // Configurar pines - Sensor Plato
  pinMode(TRIG_PLATO, OUTPUT);
  pinMode(ECHO_PLATO, INPUT);
  Serial.println("Sensor Plato: D5 (TRIG), D6 (ECHO)");
  
  // Configurar pines - Sensor Hopper
  pinMode(TRIG_HOPPER, OUTPUT);
  pinMode(ECHO_HOPPER, INPUT);
  Serial.println("Sensor Hopper: D1 (TRIG), D2 (ECHO)");
  
  // Conectar WiFi
  setup_wifi();
  
  // Configurar MQTT
  client.setServer(mqtt_server, 1883);
  
  Serial.println("=== Sistema iniciado ===\n");
}

// ========== LOOP ==========
void loop() {
  // Mantener conexión MQTT
  if (!client.connected()) {
    reconnect();
  }
  client.loop();
  
  unsigned long now = millis();
  
  // Publicar cada 5 segundos
  if (now - lastMsg > interval) {
    lastMsg = now;
    
    Serial.println("========== NUEVA LECTURA ==========");
    
    // ===== LEER SENSOR PLATO =====
    float distanciaPlato = leerDistancia(TRIG_PLATO, ECHO_PLATO, "PLATO");
    String estadoPlato = obtenerEstadoPlato(distanciaPlato);
    
    if (distanciaPlato > 0) {
      Serial.print("Plato: ");
      Serial.print(distanciaPlato);
      Serial.print(" cm | Estado: ");
      Serial.println(estadoPlato);
      
      // Crear y publicar mensaje JSON del plato
      String mensajePlato = "{";
      mensajePlato += "\"device\":\"sensor_plato\",";
      mensajePlato += "\"distance\":" + String(distanciaPlato, 2) + ",";
      mensajePlato += "\"status\":\"" + estadoPlato + "\",";
      mensajePlato += "\"timestamp\":" + String(millis());
      mensajePlato += "}";
      
      bool pub1 = client.publish("petfeeder/sensors/bowl", mensajePlato.c_str());
      Serial.print("Publicado a bowl: ");
      Serial.println(pub1 ? "OK" : "FALLO");
    }
    
    delay(100);  // Pequeña pausa entre sensores
    
    // ===== LEER SENSOR HOPPER =====
    float distanciaHopper = leerDistancia(TRIG_HOPPER, ECHO_HOPPER, "HOPPER");
    int nivelHopper = calcularNivelHopper(distanciaHopper);
    String estadoHopper = obtenerEstadoHopper(nivelHopper);
    
    if (distanciaHopper > 0) {
      Serial.print("Hopper: ");
      Serial.print(distanciaHopper);
      Serial.print(" cm | Nivel: ");
      Serial.print(nivelHopper);
      Serial.print("% | Estado: ");
      Serial.println(estadoHopper);
      
      // Crear y publicar mensaje JSON del hopper
      String mensajeHopper = "{";
      mensajeHopper += "\"device\":\"sensor_hopper\",";
      mensajeHopper += "\"distance\":" + String(distanciaHopper, 2) + ",";
      mensajeHopper += "\"level\":" + String(nivelHopper) + ",";
      mensajeHopper += "\"status\":\"" + estadoHopper + "\",";
      mensajeHopper += "\"timestamp\":" + String(millis());
      mensajeHopper += "}";
      
      bool pub2 = client.publish("petfeeder/sensors/hopper", mensajeHopper.c_str());
      Serial.print("Publicado a hopper: ");
      Serial.println(pub2 ? "OK" : "FALLO");
    }
    
    Serial.println("===================================\n");
  }
}
