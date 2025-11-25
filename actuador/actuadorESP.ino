#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <Servo.h>

// ========== CONFIGURACIÓN WiFi y MQTT ==========
const char* ssid = "juanma";                    // Cambia esto
const char* password = "janet123";             // Cambia esto
const char* mqtt_server = "10.62.69.61";        // IP de tu PC

// ========== PIN DEL SERVO ==========
#define SERVO_PIN 13   // GPIO13

// ========== CONFIGURACIÓN DEL SERVO ==========
const int ANGULO_REPOSO = 0;      // Posición cerrada
const int ANGULO_DISPENSAR = 180;  // Posición abierta (ajusta según tu mecanismo)
const int TIEMPO_DISPENSAR = 3000; // Milisegundos que permanece abierto

// ========== VARIABLES GLOBALES ==========
Servo servoMotor;
WiFiClient espClient;
PubSubClient client(espClient);

bool alimentando = false;  // Flag para evitar comandos simultáneos

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

// ========== FUNCIÓN: Callback MQTT ==========
void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Mensaje recibido en tópico: ");
  Serial.println(topic);
  
  // Convertir payload a String
  String mensaje = "";
  for (unsigned int i = 0; i < length; i++) {
    mensaje += (char)payload[i];
  }
  
  Serial.print("Contenido: ");
  Serial.println(mensaje);
  
  // Verificar que es el tópico correcto
  if (String(topic) == "petfeeder/commands/feed") {
    // Verificar el comando
    if (mensaje == "FEED" || mensaje == "feed") {
      if (!alimentando) {
        Serial.println(">>> COMANDO DE ALIMENTACIÓN RECIBIDO <<<");
        dispensarAlimento();
      } else {
        Serial.println(">>> Ya hay una alimentación en proceso, ignorando comando <<<");
      }
    } else {
      Serial.print("Comando no reconocido: ");
      Serial.println(mensaje);
    }
  }
}

// ========== FUNCIÓN: Reconectar MQTT ==========
void reconnect() {
  while (!client.connected()) {
    Serial.print("Conectando a MQTT...");
    
    String clientId = "ESP8266_Actuador";
    
    if (client.connect(clientId.c_str())) {
      Serial.println("conectado!");
      
      // Suscribirse al tópico de comandos
      client.subscribe("petfeeder/commands/feed");
      Serial.println("Suscrito a: petfeeder/commands/feed");
      
    } else {
      Serial.print("falló, rc=");
      Serial.print(client.state());
      Serial.println(" reintento en 5 seg");
      delay(5000);
    }
  }
}

// ========== FUNCIÓN: Dispensar Alimento ==========
void dispensarAlimento() {
  alimentando = true;
  
  Serial.println("--- INICIANDO DISPENSACIÓN ---");
  
  // 1. Abrir compuerta (o rotar mecanismo)
  Serial.print("Moviendo servo a ");
  Serial.print(ANGULO_DISPENSAR);
  Serial.println(" grados");
  servoMotor.write(ANGULO_DISPENSAR);
  
  // 2. Esperar tiempo de dispensación
  Serial.print("Dispensando durante ");
  Serial.print(TIEMPO_DISPENSAR);
  Serial.println(" ms");
  delay(TIEMPO_DISPENSAR);
  
  // 3. Cerrar compuerta
  Serial.print("Regresando servo a ");
  Serial.print(ANGULO_REPOSO);
  Serial.println(" grados");
  servoMotor.write(ANGULO_REPOSO);
  
  delay(500);  // Esperar a que el servo llegue a posición
  
  Serial.println("--- DISPENSACIÓN COMPLETADA ---");
  
  // 4. Publicar estado de confirmación
  String mensaje = "{";
  mensaje += "\"device\":\"actuador\",";
  mensaje += "\"action\":\"feed_completed\",";
  mensaje += "\"timestamp\":" + String(millis());
  mensaje += "}";
  
  bool publicado = client.publish("petfeeder/actuator/status", mensaje.c_str());
  
  Serial.print("Estado publicado a MQTT: ");
  Serial.println(publicado ? "SI" : "NO");
  
  alimentando = false;
}

// ========== SETUP ==========
void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("\n\n=== DEVICE 2: ACTUADOR ===");
  
  // Configurar servo
  servoMotor.attach(SERVO_PIN);
  servoMotor.write(ANGULO_REPOSO);  // Posición inicial cerrada
  
  Serial.print("Servo configurado en pin D7 (GPIO");
  Serial.print(SERVO_PIN);
  Serial.println(")");
  Serial.print("Posición inicial: ");
  Serial.print(ANGULO_REPOSO);
  Serial.println(" grados");
  
  // Conectar WiFi
  setup_wifi();
  
  // Configurar MQTT
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);  // Configurar función callback
  
  Serial.println("=== Sistema iniciado - Esperando comandos ===\n");
}

// ========== LOOP ==========
void loop() {
  // Mantener conexión MQTT
  if (!client.connected()) {
    reconnect();
  }
  client.loop();  // Procesar mensajes entrantes
  
  // Este loop solo mantiene la conexión y espera comandos
  // No hace nada más
}