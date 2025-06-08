# Mausam Mate - Modern Weather App with Jetpack Compose

## Overview

**Mausam Mate** is a sleek, modern weather application developed for Android using **Kotlin** and **Jetpack Compose**. The app provides real-time weather updates, hourly forecasts, and air quality information by integrating with the **OpenWeatherMap API**. Featuring a polished dark theme with smooth animations, Mausam Mate offers an intuitive and visually appealing way to check weather conditions for your current or any custom location worldwide.

The app aims to deliver accurate weather data alongside a user-friendly experience with dynamic UI elements and easy location search.

---

## Key Features

- **Current Weather Display**: Shows temperature, min/max temps, wind speed, and overall weather conditions like rain, clouds, or clear skies.
- **Hourly Forecast**: Provides a 5-hour forecast with temperature trends and weather icons.
- **Air Quality Index (AQI)**: Visualizes air quality status with engaging animations indicating good or poor air quality.
- **Location-Based Weather**: Automatically fetches weather data based on the user’s current GPS location (with permission).
- **Custom Location Search**: Allows searching weather by city name with real-time suggestions starting after typing 3 characters.
- **Dynamic Animations**: Uses Lottie animations for weather conditions (rainy, sunny, snowy) and wind speed for an immersive UI.
- **Dark Theme**: Features a professional dark mode with subtle gradients, easy on the eyes during night-time usage.

---

## Technology Stack

- **Kotlin**: The primary language for Android app development.
- **Jetpack Compose**: For building the modern and responsive UI.
- **Retrofit & Gson**: To handle networking and JSON parsing for API calls.
- **Google Play Services**: Used for fetching location data via `FusedLocationProviderClient`.
- **Lottie for Android**: To display smooth and interactive weather animations.
- **OpenWeatherMap API**: Source of weather, forecast, air pollution, and geocoding data.

---

## Installation Instructions

Follow these steps to clone the repo and run the app locally:

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/mausam-mate.git
   cd mausam-mate
2. Add your OpenWeatherMap API key:

Open MainActivity.kt.

Replace the placeholder API key "7efb32d7a7b4182bff3009ed2517e8c1" with your own API key in the Retrofit instances for weather and geocoding.

3. Build and run:

Open the project in Android Studio (latest stable version recommended).

Sync Gradle files.

Run the app on an emulator or a physical device (minimum SDK 21).
