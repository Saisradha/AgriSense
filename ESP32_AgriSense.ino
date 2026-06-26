#include <Arduino.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <DHT.h>
#include <time.h>

// Provide Helper functions for Firebase token generation details
#include <addons/TokenHelper.h>
// Provide Helper functions for RTDB formatting details
#include <addons/RTDBHelper.h>

// 1. WIFI Credentials
#define WIFI_SSID "YOUR_WIFI_SSID"
#define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"

// 2. Firebase Credentials
#define API_KEY "YOUR_FIREBASE_API_KEY"
#define DATABASE_URL "YOUR_FIREBASE_DATABASE_URL"

// 3. Pin Allocations
#define DHTPIN 4
#define DHTTYPE DHT11 // Set to DHT22 if using DHT22
#define SOIL_MOISTURE_PIN 34 // Analog Pin VP/A0
#define WATER_LEVEL_PIN 35   // Analog Pin

// Sensor calibration bounds
// Change these depending on raw readings:
#define SOIL_MOISTURE_AIR 3200   // Raw reading in dry air
#define SOIL_MOISTURE_WATER 1500 // Raw reading in water
#define WATER_LEVEL_MIN 0        // Raw reading in empty cup
#define WATER_LEVEL_MAX 2500     // Raw reading in full water

// 4. Global Instances
DHT dht(DHTPIN, DHTTYPE);
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

unsigned long sendDataPrevMillis = 0;
const long sendInterval = 5000; // Upload values every 5 seconds

// Time configuration
const char* ntpServer = "pool.ntp.org";
const long  gmtOffset_sec = 19800; // GMT+5:30 (India Standard Time)
const int   daylightOffset_sec = 0;

void setupWiFi() {
  Serial.println("Connecting to WiFi...");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.print("WiFi Connected. IP address: ");
  Serial.println(WiFi.localIP());
}

void setup() {
  Serial.begin(115200);
  dht.begin();
  
  // Set Analog Resolution to 12-bit (0-4095) for ESP32
  analogReadResolution(12);

  setupWiFi();

  // Configure Time (NTP)
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);

  // Configure Firebase Config Details
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;

  // Sign in anonymously
  if (Firebase.signUp(&config, &auth, "", "")) {
    Serial.println("Firebase Auth Sign Up Success.");
  } else {
    Serial.printf("Firebase Auth Sign Up Fail: %s\n", config.signer.signupError.message.c_str());
  }

  // Set callback for Token status
  config.token_status_callback = tokenStatusCallback;
  
  // Initialize library
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

unsigned long getEpochTime() {
  time_t now;
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) {
    return 0;
  }
  time(&now);
  return now;
}

void loop() {
  // Auto-reconnect WiFi if disconnected
  if (WiFi.status() != WL_CONNECTED) {
    setupWiFi();
  }

  // Run database upload every 5 seconds
  if (Firebase.ready() && (millis() - sendDataPrevMillis > sendInterval || sendDataPrevMillis == 0)) {
    sendDataPrevMillis = millis();

    // Read Sensors
    float t = dht.readTemperature();
    float h = dht.readHumidity();

    // Verify DHT readings
    if (isnan(t) || isnan(h)) {
      Serial.println("Failed to read from DHT sensor!");
      t = 28.0; // Fail-safes
      h = 65.0;
    }

    // Read Soil Moisture
    int rawMoisture = analogRead(SOIL_MOISTURE_PIN);
    // Map raw ADC to moisture scale (e.g. 0 to 1000)
    // Dry soil yields higher resistance (higher raw value)
    int mappedMoisture = map(rawMoisture, SOIL_MOISTURE_AIR, SOIL_MOISTURE_WATER, 0, 1000);
    // Constrain to logical range 0 - 1000
    mappedMoisture = constrain(mappedMoisture, 0, 1000);

    // Read Farm Water Level
    int rawWater = analogRead(WATER_LEVEL_PIN);
    // Map raw ADC to water level percentage (0 to 100%)
    int mappedWaterPercent = map(rawWater, WATER_LEVEL_MIN, WATER_LEVEL_MAX, 0, 100);
    mappedWaterPercent = constrain(mappedWaterPercent, 0, 100);

    // Fetch timestamp
    unsigned long timestamp = getEpochTime();

    // Serial monitor debugging output
    Serial.println("==============================");
    Serial.print("Temperature: "); Serial.print(t); Serial.println(" °C");
    Serial.print("Humidity: "); Serial.print(h); Serial.println(" %");
    Serial.print("Soil Moisture (raw): "); Serial.print(rawMoisture);
    Serial.print(" -> Mapped: "); Serial.println(mappedMoisture);
    Serial.print("Water Level (raw): "); Serial.print(rawWater);
    Serial.print(" -> Percentage: "); Serial.print(mappedWaterPercent); Serial.println(" %");
    Serial.print("Timestamp: "); Serial.println(timestamp);

    // Upload to Firebase
    FirebaseJson json;
    json.set("temperature", t);
    json.set("humidity", h);
    json.set("soilMoisture", mappedMoisture);
    json.set("farmWaterLevel", mappedWaterPercent);
    json.set("timestamp", timestamp);

    Serial.println("Uploading sensor data to Firebase...");
    if (Firebase.RTDB.setJSON(&fbdo, "/sensorData", &json)) {
      Serial.println("Upload SUCCESS!");
    } else {
      Serial.print("Upload FAILED: ");
      Serial.println(fbdo.errorReason());
    }
  }
}
