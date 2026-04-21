# 🌤 Weather Bot — Dialogflow ES + Spring Boot Webhook

A REST API webhook built with **Java Spring Boot** that powers a Dialogflow ES weather chatbot.
It handles current weather and 8-day forecast requests by integrating with the **OpenWeatherMap API**.

---

## 🏗 Architecture

```
User → Dialogflow ES → Webhook (Spring Boot) → OpenWeatherMap API
                     ←                        ←
```

---

## 📋 Prerequisites

Make sure you have the following installed:

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org |
| ngrok | Latest | https://ngrok.com |
| Git | Any | https://git-scm.com |

You also need free accounts on:
- **OpenWeatherMap**: https://openweathermap.org/api (for API key)
- **Google Cloud / Dialogflow ES**: https://dialogflow.cloud.google.com

---

## 🚀 Step-by-Step Setup Guide

### STEP 1 — Get Your OpenWeatherMap API Key

1. Go to https://openweathermap.org and create a free account.
2. Navigate to **API Keys** in your profile dashboard.
3. Copy your default API key (or generate a new one).
4. ⚠️ New keys take up to 2 hours to activate.

---

### STEP 2 — Configure the Spring Boot Application

1. Open `src/main/resources/application.properties`
2. Replace the placeholder with your actual API key:

```properties
openweather.api.key=YOUR_ACTUAL_API_KEY_HERE
```

---

### STEP 3 — Build and Run the Spring Boot App

```bash
# Clone the repository (if from GitHub)
git clone https://github.com/YOUR_USERNAME/weather-dialogflow-bot.git
cd weather-dialogflow-bot

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The server will start on **http://localhost:8080**

**Verify it's running:**
```bash
curl http://localhost:8080/webhook/health
# Should return: Weather Bot Webhook is running! ✅
```

---

### STEP 4 — Expose Localhost with ngrok

Dialogflow needs a public HTTPS URL to reach your local server.

```bash
# Install ngrok (if not installed)
# Mac: brew install ngrok
# Windows/Linux: Download from https://ngrok.com/download

# Start ngrok tunnel on port 8080
ngrok http 8080
```

ngrok will show output like:
```
Forwarding  https://abc123xyz.ngrok-free.app → http://localhost:8080
```

**Copy the HTTPS URL** — you'll need it in Dialogflow.

> ⚠️ The ngrok URL changes every time you restart it (on the free plan).
> You must update the Dialogflow webhook URL each session.

---

### STEP 5 — Create Your Dialogflow ES Agent

1. Go to https://dialogflow.cloud.google.com
2. Click **"Create Agent"**
3. Name it `WeatherBot`, set language to **English**, click **Create**

---

### STEP 6 — Create Intents in Dialogflow ES

#### Intent 1: Default Welcome Intent
This already exists. Modify its responses:
- Add response: `"This is the Weather BOT. How may I help you?"`

#### Intent 2: Current Weather Intent
1. Click **"Create Intent"**
2. **Name**: `Current Weather`
3. **Action name**: `weather.current`
4. **Training phrases** (add at least 5):
   - `What is the current weather?`
   - `What's the weather in London?`
   - `My city is Karachi`
   - `Tell me the weather for Dubai`
   - `Current weather in Paris`
5. **Parameters**:
   - Parameter name: `geo-city`
   - Entity: `@sys.geo-city`
   - Value: `$geo-city`
   - Check **Required** ✅
   - Prompts: `Please provide your city.`
6. **Fulfillment**: Enable **"Enable webhook call for this intent"** ✅
7. Click **Save**

#### Intent 3: Weather Forecast Intent
1. Click **"Create Intent"**
2. **Name**: `Weather Forecast`
3. **Action name**: `weather.forecast`
4. **Training phrases**:
   - `Please tell me the weather forecast from tomorrow for London`
   - `What's the forecast for New York?`
   - `Weather forecast from 2024-01-20 for Karachi`
   - `Give me a 5-day forecast for Dubai`
   - `Forecast from next Monday for Paris`
5. **Parameters**:
   - Parameter 1:
     - Name: `geo-city`, Entity: `@sys.geo-city`, Required ✅
     - Prompt: `Which city do you want the forecast for?`
   - Parameter 2:
     - Name: `date`, Entity: `@sys.date`, Required: ❌ (optional)
6. **Fulfillment**: Enable **"Enable webhook call for this intent"** ✅
7. Click **Save**

#### Intent 4: Goodbye Intent
1. **Name**: `Goodbye`
2. **Training phrases**: `Thanks`, `No thanks`, `That's all`, `Goodbye`
3. **Responses**: `Thank you for contacting. Goodbye!`
4. No fulfillment needed.

---

### STEP 7 — Configure Fulfillment Webhook in Dialogflow

1. In the Dialogflow left menu, click **"Fulfillment"**
2. Toggle **Webhook** to **ENABLED**
3. **URL**: Paste your ngrok HTTPS URL + `/webhook`
   ```
   https://abc123xyz.ngrok-free.app/webhook
   ```
4. Leave Headers empty
5. Click **Save**

---

### STEP 8 — Test the Bot

In Dialogflow, open the **"Try it now"** panel on the right.

Test these conversations:

**Current weather flow:**
```
You: Hi
Bot: This is the Weather BOT. How may I help you?

You: What is the current weather?
Bot: Please provide your city.

You: My city is Karachi
Bot: 🌤 Current weather in Karachi, PK:
     • Condition: Clear sky
     • Temperature: 32.5°C (Feels like 35.0°C)
     • Humidity: 65%
     • Wind Speed: 4.2 m/s
```

**Forecast flow:**
```
You: Please tell me the weather forecast from tomorrow for London
Bot: 📅 Weather forecast for London from Tuesday, Jan 21 to Tuesday, Jan 28:
     • Tuesday, Jan 21: Overcast clouds, 8.0°C, Humidity: 80%
     • ...
```

---

## 📁 Project Structure

```
weather-dialogflow-bot/
├── src/
│   ├── main/
│   │   ├── java/com/weatherbot/
│   │   │   ├── WeatherBotApplication.java       ← Entry point
│   │   │   ├── controller/
│   │   │   │   └── WebhookController.java        ← POST /webhook endpoint
│   │   │   ├── service/
│   │   │   │   ├── DialogflowService.java        ← Intent routing logic
│   │   │   │   └── WeatherApiService.java        ← OpenWeatherMap calls
│   │   │   ├── model/
│   │   │   │   ├── DialogflowRequest.java        ← Incoming webhook model
│   │   │   │   ├── DialogflowResponse.java       ← Outgoing webhook model
│   │   │   │   ├── CurrentWeatherResponse.java   ← OWM current weather model
│   │   │   │   └── ForecastWeatherResponse.java  ← OWM forecast model
│   │   │   └── config/
│   │   │       └── GlobalExceptionHandler.java   ← Error handling
│   │   └── resources/
│   │       └── application.properties            ← API key config
│   └── test/
│       └── java/com/weatherbot/service/
│           └── DialogflowServiceTest.java        ← Unit tests
├── pom.xml
└── README.md
```

---

## 🧪 Running Tests

```bash
mvn test
```

---

## 📦 Building for Deployment (JAR)

```bash
mvn clean package
java -jar target/weather-dialogflow-bot-1.0.0.jar
```

---

## 🌐 Deploy to a Cloud Host (Optional — for permanent URL)

Instead of ngrok, you can deploy to:
- **Railway.app** — Free, supports Java/Maven, gives permanent URL
- **Render.com** — Free tier available
- **Heroku** — Classic choice

For Railway:
1. Push your code to GitHub
2. Connect the repo on https://railway.app
3. Set environment variable: `OPENWEATHER_API_KEY=your_key`
4. Use the provided URL as your Dialogflow webhook

---

## 🔧 API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| `POST` | `/webhook` | Dialogflow ES webhook endpoint |
| `GET` | `/webhook/health` | Health check |

---

## ⚠️ Common Issues

| Problem | Solution |
|---------|----------|
| 401 from OpenWeatherMap | Check your API key in `application.properties` |
| 404 city not found | City name may be misspelled or not in OWM database |
| Dialogflow says "no response" | Check ngrok is running and URL is correctly set |
| ngrok URL expired | Restart ngrok and update Dialogflow webhook URL |
| Tests failing | Make sure Mockito is on the classpath (included in `pom.xml`) |
