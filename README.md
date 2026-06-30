# 🌱 AgriSense AI – Intelligent Smart Irrigation System

## 📌 Overview

AgriSense AI is an IoT and AI-powered smart irrigation system designed to optimize water usage in agriculture by continuously monitoring environmental conditions and automatically controlling irrigation. The system collects real-time data from multiple sensors using an ESP32 microcontroller, uploads the data to Firebase, and displays it in an Android application. Based on sensor readings, an AI decision engine predicts whether irrigation is required and controls the water pump accordingly.

The project aims to reduce water wastage, improve crop productivity, and provide farmers with an affordable, intelligent irrigation solution.

---

# 🎯 Objectives

- Monitor soil moisture in real time.
- Measure environmental conditions such as temperature, humidity, and light intensity.
- Automatically control irrigation based on AI predictions.
- Provide remote monitoring through an Android application.
- Store sensor data securely in Firebase Realtime Database.
- Reduce manual intervention and conserve water.

---

# ✨ Features

- 🌱 Real-time Soil Moisture Monitoring
- 🌡 Temperature Monitoring
- 💧 Humidity Monitoring
- ☀ Light Intensity Detection
- 🤖 AI-Based Irrigation Prediction
- 🚰 Automatic Water Pump Control
- 📱 Android Application
- ☁ Firebase Realtime Database Integration
- 📊 Historical Sensor Graphs
- 🔔 Smart Notifications
- 📡 WiFi Connectivity using ESP32
- 🔄 Real-Time Data Synchronization
- 🧪 Sensor Simulation Mode for Testing

---

# 🛠 Hardware Components

| Component | Purpose |
|------------|---------|
| ESP32 Development Board | Main Controller |
| Soil Moisture Sensor | Detects Soil Moisture |
| DHT11 Sensor | Measures Temperature & Humidity |
| LDR Sensor | Measures Light Intensity |
| Relay Module | Controls Water Pump |
| DC Water Pump | Irrigation |
| Power Supply | Powers Entire System |
| Jumper Wires | Connections |
| Breadboard | Prototyping |

---

# 💻 Software Used

- Android Studio
- Java
- Firebase Realtime Database
- Firebase Authentication
- ESP32 Arduino Framework
- Arduino IDE
- XML (Android UI)
- Git & GitHub

---

# 📱 Android Application

The Android application provides:

- User Login
- Dashboard
- Live Sensor Values
- AI Prediction
- Pump Status
- Manual Water Now Button
- Automatic Irrigation Mode
- Historical Graphs
- Insights Page
- Notifications
- Settings

---

# ☁ Firebase Database Structure

```
Firebase
│
├── sensorData
│      ├── temperature
│      ├── humidity
│      ├── soilMoisture
│      ├── light
│
├── pumpStatus
│
├── prediction
│
└── timestamp
```

---

# 🤖 AI Decision Logic

The AI engine analyses:

- Soil Moisture
- Temperature
- Humidity
- Light Intensity

Example Logic:

```
If

Soil Moisture > Threshold

OR

Temperature > Threshold

↓

Prediction = Water Required

↓

Pump = ON

Else

Prediction = Soil Moisture Good

↓

Pump = OFF
```

This enables intelligent irrigation decisions without manual intervention.

---

# 🔄 Working Principle

1. Sensors collect environmental data.
2. ESP32 reads sensor values.
3. Data is uploaded to Firebase.
4. Android application receives live updates.
5. AI analyzes the sensor readings.
6. Prediction is generated.
7. Relay switches the pump ON/OFF.
8. User can also manually control irrigation.

---

# 📊 Parameters Monitored

- Temperature (°C)
- Humidity (%)
- Soil Moisture
- Light Intensity
- Pump Status
- AI Prediction

---

# 📡 Communication Flow

```
Sensors
     │
     ▼
ESP32
     │
 WiFi
     │
     ▼
Firebase Realtime Database
     │
     ▼
Android Application
     │
     ▼
AI Decision Engine
     │
     ▼
Relay Module
     │
     ▼
Water Pump
```

---

# 📈 Advantages

- Saves Water
- Reduces Human Effort
- Low Cost
- Real-Time Monitoring
- Smart Irrigation
- Remote Access
- Scalable Design
- Easy Installation
- Energy Efficient

---

# 🌾 Applications

- Agriculture
- Greenhouses
- Home Gardens
- Nurseries
- Smart Farming
- Research Farms
- Precision Agriculture

---

# 🚀 Future Enhancements

- Machine Learning Prediction Model
- Weather Forecast Integration
- Crop Recommendation
- Multiple Farm Support
- Voice Assistant
- SMS Alerts
- Solar Powered System
- Fertigation Control
- Disease Detection using AI
- Cloud Analytics Dashboard

---

# 📷 Project Architecture

```
        Sensors
 ┌──────────────────────┐
 │ Soil Moisture Sensor │
 │ DHT11 Sensor         │
 │ LDR Sensor           │
 └──────────┬───────────┘
            │
            ▼
         ESP32
            │
         WiFi
            │
            ▼
 Firebase Realtime Database
            │
            ▼
     Android Application
            │
      AI Decision Engine
            │
     Pump Control Logic
            │
            ▼
       Relay Module
            │
            ▼
       Water Pump
```

---

# 📂 Project Structure

```
AgriSense-AI/

│
├── ESP32_Code/
│
├── Android_App/
│
├── Firebase/
│
├── Images/
│
├── Documentation/
│
├── PPT/
│
├── README.md
│
└── LICENSE
```

---

# ⚙ Installation

### ESP32

1. Install Arduino IDE.
2. Install ESP32 Board Package.
3. Install required libraries.
4. Configure WiFi credentials.
5. Configure Firebase credentials.
6. Upload the code.

---

### Android App

1. Open project in Android Studio.
2. Connect Firebase.
3. Build APK.
4. Install on Android device.
5. Login and monitor live data.

---

# 📸 Project Demonstration

The system demonstrates:

- Live Sensor Monitoring
- Firebase Real-Time Synchronization
- AI Prediction
- Automatic Pump Control
- Manual Water Control
- Android Dashboard
- Historical Graphs

---

# 📋 Results

- Successfully monitored environmental parameters.
- Achieved real-time Firebase synchronization.
- Implemented AI-based irrigation prediction.
- Controlled water pump automatically using relay.
- Developed a responsive Android application for monitoring and control.
- Demonstrated reliable communication between ESP32, Firebase, and Android application.

---

# 👨‍💻 Developed By

**AgriSense AI Team**

Department of Computer Science & Engineering

National Institute of Technology Warangal (NIT Warangal)

---

# 📜 License

This project is developed for academic and research purposes.

---

# ⭐ Acknowledgement

We sincerely thank our faculty mentors, the Department of Computer Science & Engineering, and NIT Warangal for their guidance and support throughout the development of this project.

---

## 🌿 "Smart Irrigation Today for a Sustainable Tomorrow."
