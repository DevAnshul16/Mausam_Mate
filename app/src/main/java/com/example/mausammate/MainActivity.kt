package com.example.mausammate

import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
    private val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
    private val geoApi: GeoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/geo/1.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeoApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(this, Locale.getDefault())

        setContent {
            MausamMateTheme {
                val scope = rememberCoroutineScope()
                var location by remember { mutableStateOf("Fetching Location...") }
                var weather by remember { mutableStateOf(WeatherData(0f, 0f, 0f, 0f, "Loading...", 0, 0L, 0L, emptyList())) }
                var searchQuery by remember { mutableStateOf("") }
                var isLoading by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                var locationSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        fetchCurrentLocationWeather(scope) { placeName, data ->
                            location = placeName
                            weather = data
                        }
                    } else {
                        location = "Location Permission Denied"
                    }
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }

                LaunchedEffect(searchQuery) {
                    if (searchQuery.length >= 3) {
                        scope.launch {
                            try {
                                val geoData = geoApi.getCoordinates(searchQuery, "7efb32d7a7b4182bff3009ed2517e8c1", 5)
                                locationSuggestions = geoData.map { "${it.name}, ${it.state ?: it.country}" }
                            } catch (e: Exception) {
                                Log.e("GeoError", "Suggestions failed: ${e.message}")
                                locationSuggestions = emptyList()
                            }
                        }
                    } else {
                        locationSuggestions = emptyList()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HomeScreen(
                            location = location,
                            weather = weather,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSearch = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        val geoData = geoApi.getCoordinates(searchQuery, "7efb32d7a7b4182bff3009ed2517e8c1", 1)
                                        if (geoData.isNotEmpty()) {
                                            val lat = geoData[0].lat
                                            val lon = geoData[0].lon
                                            location = "${geoData[0].name}, ${geoData[0].state ?: geoData[0].country}"
                                            fetchWeatherData(scope, lat, lon) { data ->
                                                weather = data
                                            }
                                            locationSuggestions = emptyList()
                                            searchQuery = "" // Clear after search
                                        } else {
                                            errorMessage = "Location Not Found"
                                        }
                                    } catch (e: Exception) {
                                        Log.e("GeoError", "Search failed: ${e.message}")
                                        errorMessage = "Search Error: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            onRefresh = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    fetchCurrentLocationWeather(scope) { placeName, data ->
                                        location = placeName
                                        weather = data
                                        isLoading = false
                                    }
                                }
                            },
                            locationSuggestions = locationSuggestions,
                            onSuggestionClick = { suggestion ->
                                searchQuery = suggestion
                            }
                        )
                        errorMessage?.let {
                            Snackbar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                action = {
                                    TextButton(onClick = { errorMessage = null }) {
                                        Text("Dismiss")
                                    }
                                }
                            ) {
                                Text(it)
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocationWeather(
        scope: CoroutineScope,
        onDataReceived: (String, WeatherData) -> Unit
    ) {
        scope.launch {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val placeName = address.locality ?: address.subAdminArea ?: "Unknown Location"
                            val city = address.adminArea ?: ""
                            val fullLocation = "$placeName, $city"
                            fetchWeatherData(scope, location.latitude, location.longitude) { data ->
                                onDataReceived(fullLocation, data)
                            }
                        } else {
                            onDataReceived("Unknown Location", WeatherData(0f, 0f, 0f, 0f, "Unknown", 0, 0L, 0L, emptyList()))
                        }
                    } else {
                        onDataReceived("Location Not Available", WeatherData(0f, 0f, 0f, 0f, "Unavailable", 0, 0L, 0L, emptyList()))
                    }
                }.addOnFailureListener {
                    onDataReceived("Failed to Fetch Location", WeatherData(0f, 0f, 0f, 0f, "Error", 0, 0L, 0L, emptyList()))
                }
            } catch (e: Exception) {
                onDataReceived("Error: ${e.message}", WeatherData(0f, 0f, 0f, 0f, "Error", 0, 0L, 0L, emptyList()))
            }
        }
    }

    private fun fetchWeatherData(scope: CoroutineScope, lat: Double, lon: Double, onDataReceived: (WeatherData) -> Unit) {
        scope.launch {
            try {
                val weatherData = weatherApi.getWeather(lat, lon, "7efb32d7a7b4182bff3009ed2517e8c1")
                val airData = weatherApi.getAirPollution(lat, lon, "7efb32d7a7b4182bff3009ed2517e8c1")
                val forecastData = weatherApi.getForecast(lat, lon, "7efb32d7a7b4182bff3009ed2517e8c1")
                Log.d("WeatherData", "Temp: ${weatherData.main.temp}, Min: ${weatherData.main.temp_min}, Max: ${weatherData.main.temp_max}")
                onDataReceived(
                    WeatherData(
                        temp = weatherData.main.temp,
                        minTemp = weatherData.main.temp_min,
                        maxTemp = weatherData.main.temp_max,
                        windSpeed = weatherData.wind.speed,
                        condition = weatherData.weather[0].main,
                        aqi = airData.list[0].main.aqi,
                        sunrise = weatherData.sys.sunrise * 1000L,
                        sunset = weatherData.sys.sunset * 1000L,
                        hourlyForecast = forecastData.list.take(5).map {
                            HourlyForecast(it.dt, it.main.temp, it.weather[0].main)
                        }
                    )
                )
            } catch (e: Exception) {
                Log.e("WeatherError", "API call failed: ${e.message}")
                onDataReceived(WeatherData(0f, 0f, 0f, 0f, "Error: ${e.message}", 0, 0L, 0L, emptyList()))
            }
        }
    }
}

data class WeatherData(
    val temp: Float,
    val minTemp: Float,
    val maxTemp: Float,
    val windSpeed: Float,
    val condition: String,
    val aqi: Int,
    val sunrise: Long,
    val sunset: Long,
    val hourlyForecast: List<HourlyForecast>
)

data class HourlyForecast(
    val dt: Long,
    val temp: Float,
    val condition: String
)

interface WeatherApi {
    @GET("weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    @GET("air_pollution")
    suspend fun getAirPollution(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String
    ): AirPollutionResponse

    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): ForecastResponse
}

interface GeoApi {
    @GET("direct")
    suspend fun getCoordinates(
        @Query("q") query: String,
        @Query("appid") apiKey: String,
        @Query("limit") limit: Int = 1
    ): List<GeoResponse>
}

data class WeatherResponse(
    val main: Main,
    val wind: Wind,
    val weather: List<Weather>,
    val sys: Sys
)

data class AirPollutionResponse(val list: List<AirPollutionData>)
data class ForecastResponse(val list: List<ForecastItem>)
data class ForecastItem(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>
)

data class AirPollutionData(val main: AirPollutionMain)
data class AirPollutionMain(val aqi: Int)
data class Main(val temp: Float, val temp_min: Float, val temp_max: Float)
data class Wind(val speed: Float)
data class Weather(val main: String)
data class Sys(val sunrise: Long, val sunset: Long)

data class GeoResponse(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String?
)

@Composable
fun MausamMateTheme(content: @Composable () -> Unit) {
    val isDarkTheme = true
    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme(
            primary = Color(0xFFBB86FC),
            background = Color(0xFF121212),
            onBackground = Color.White,
            surface = Color(0xFF1E1E1E),
            onSurface = Color.White
        ) else lightColorScheme(
            primary = Color(0xFF6200EE),
            background = Color.White,
            onBackground = Color.Black
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
            bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp)
        ),
        content = content
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreen(
    location: String,
    weather: WeatherData,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    locationSuggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    val currentTime = System.currentTimeMillis()
    val isDay = currentTime in weather.sunrise..weather.sunset
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF121212), Color(0xFF1E1E1E))
    )

    var isLoading by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search Location", color = Color.Gray) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                    }
                    if (locationSuggestions.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(MaterialTheme.colorScheme.surface)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            items(locationSuggestions) { suggestion ->
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSuggestionClick(suggestion) }
                                        .padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Location and Refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = location,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                IconButton(onClick = {
                    isLoading = true
                    onRefresh()
                }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Weather Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${weather.temp.roundToInt()}°C",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Text(
                        text = "${weather.condition}  |  Min: ${weather.minTemp.roundToInt()}°C  Max: ${weather.maxTemp.roundToInt()}°C",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SeasonalAnimation(weather.condition, isDay, weather.temp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wind and AQI Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WindAnimation(weather.windSpeed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Wind",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${(weather.windSpeed * 3.6f).roundToInt()} km/h",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AQIAnimation(weather.aqi)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AQI",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${weather.aqi}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hourly Forecast
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Hourly Forecast",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow {
                        items(weather.hourlyForecast) { forecast ->
                            HourlyForecastItem(forecast)
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HourlyForecastItem(forecast: HourlyForecast) {
    val time = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(forecast.dt * 1000))
    val emoji = when (forecast.condition.lowercase()) {
        "clear" -> "☀️"
        "clouds" -> "☁️"
        "rain", "drizzle" -> "🌧️"
        "snow" -> "❄️"
        else -> "🌤️"
    }
    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .width(70.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = "$emoji ${forecast.temp.roundToInt()}°C",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Text(
            text = forecast.condition,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SeasonalAnimation(condition: String, isDay: Boolean, temperature: Float) {
    val composition by rememberLottieComposition(
        when (condition.lowercase()) {
            "rain", "drizzle" -> LottieCompositionSpec.RawRes(R.raw.rainy)
            "clouds" -> LottieCompositionSpec.RawRes(R.raw.clouds)
            "snow" -> LottieCompositionSpec.RawRes(R.raw.winter)
            "clear" -> if (isDay) LottieCompositionSpec.RawRes(R.raw.summer) else LottieCompositionSpec.RawRes(R.raw.sleep)
            else -> if (temperature < 5) LottieCompositionSpec.RawRes(R.raw.winter) else LottieCompositionSpec.RawRes(R.raw.clouds)
        }
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape = RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(280.dp)
        )
    }
}

@Composable
fun AQIAnimation(aqi: Int) {
    val composition by rememberLottieComposition(
        if (aqi <= 2) LottieCompositionSpec.RawRes(R.raw.good_aqi)
        else LottieCompositionSpec.RawRes(R.raw.danger)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(60.dp)
    )
}

@Composable
fun WindAnimation(windSpeed: Float) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.windmill)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = windSpeed / 2f.coerceAtLeast(0.5f)
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(60.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MausamMateTheme {
        HomeScreen(
            location = "Mayur Vihar, Delhi",
            weather = WeatherData(
                25f, 20f, 30f, 5f, "Rain", 1,
                System.currentTimeMillis() - 3600000,
                System.currentTimeMillis() + 3600000,
                listOf(
                    HourlyForecast(System.currentTimeMillis() / 1000, 26f, "Clear"),
                    HourlyForecast(System.currentTimeMillis() / 1000 + 3600, 27f, "Clouds")
                )
            ),
            searchQuery = "",
            onSearchQueryChange = {},
            onSearch = {},
            onRefresh = {},
            locationSuggestions = listOf("Delhi, India", "Mumbai, India"),
            onSuggestionClick = {}
        )
    }
}