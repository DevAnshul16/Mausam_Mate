Mausam Mate
![logo (2)](https://github.com/user-attachments/assets/d420e965-a5c1-4c6e-90c6-b464c677d13e)
Mausam Mate is a sleek, modern weather app built with Jetpack Compose and Kotlin for Android. It provides real-time weather updates, hourly forecasts, and air quality information using the OpenWeatherMap API. With dynamic animations and a polished dark-themed UI, it offers an intuitive way to check the weather for your current location or any custom location worldwide.

Features
Current Weather: Displays temperature, min/max temps, wind speed, and weather conditions (e.g., rain, clouds, clear).
Hourly Forecast: Shows a 5-hour forecast with temperature and condition icons.
Air Quality Index (AQI): Visualizes air quality with animations (good or poor).
Location-Based Weather: Fetches weather for your current location using GPS (with permission).
Custom Location Search: Search for weather in any city with real-time suggestions after 3 characters.
Dynamic Animations: Lottie animations for weather conditions (e.g., rainy, sunny, snowy) and wind speed.
Dark Theme: Professional, eye-friendly UI with a subtle gradient background.

Prerequisites
Android Studio (latest stable version recommended)
Android SDK 21+ (min SDK)
OpenWeatherMap API key (sign up here)
Installation
1.git clone https://github.com/yourusername/mausam-mate.git
cd mausam-mate
2. Add Your API Key:
Open MainActivity.kt.
Replace "7efb32d7a7b4182bff3009ed2517e8c1" with your OpenWeatherMap API key in the weatherApi and geoApi Retrofit instances.

Build and Run:
Open the project in Android Studio.
Sync the project with Gradle files.
Run the app on an emulator or physical device (API 21+).

Usage
Launch the App:

Grant location permission when prompted to fetch weather for your current location.
Alternatively, use the search bar to find weather for any city.
Explore Features:
Check the main weather card for current conditions and temperature.
Scroll to see wind speed, AQI, and hourly forecast.
Tap the refresh button to update weather data.
Search Custom Locations:
Type at least 3 letters in the search field to see suggestions.
Select a suggestion or finish typing, then press the search icon to fetch weather.

Tech Stack
Language: Kotlin
UI Framework: Jetpack Compose
Networking: Retrofit with Gson
Location: Google Play Services (FusedLocationProviderClient)
Animations: Lottie for Android
API: OpenWeatherMap (Weather, Forecast, Air Pollution, Geocoding)
