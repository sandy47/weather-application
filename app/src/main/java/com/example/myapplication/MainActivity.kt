package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter

class MainActivity : ComponentActivity() {
    private val apiKey = "e31f9dcd519259af40e017cab72bfb7a"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize permission request launcher
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    getLastLocation() // Get location if permission is granted
                } else {
                    error(R.string.error_message)
                }
            }

        // Always request location permission
        requestLocationPermission()

        // Set content with Composable
        setContent {
            WeatherScreen(apiKey)
        }
    }

    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    // Fetch weather using coordinates in Composable context
                    setContent {
                        WeatherScreen(apiKey, it.latitude, it.longitude)
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherScreen(apiKey: String, lat: Double? = null, lon: Double? = null) {
    val preferencesManager =
        PreferencesManager(LocalContext.current) // Initialize PreferencesManager
    val viewModel: WeatherViewModel = viewModel(factory = WeatherViewModelFactory(apiKey))

    // Auto-fetch weather for the last searched city (if available)
    val lastCity = preferencesManager.getLastSearchedCity()
    if (!lastCity.isNullOrEmpty()) {
        viewModel.fetchWeather(lastCity) // Fetch weather for last city
    } else if (lat != null && lon != null) {
        viewModel.fetchWeatherByCoordinates(lat, lon) // Fetch weather for current location
    }

    WeatherApp(viewModel, preferencesManager)
}

@Composable
fun WeatherApp(viewModel: WeatherViewModel, preferencesManager: PreferencesManager) {
    var city by remember { mutableStateOf(preferencesManager.getLastSearchedCity() ?: "") }

    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable { keyboardController?.hide() } // Hide keyboard on click outside
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = city,
            onValueChange = { city = it },
            label = { Text(stringResource(id = R.string.enter_city)) },
            keyboardActions = KeyboardActions(
                onDone = {
                    if (city.isNotEmpty()) {
                        viewModel.fetchWeather(city) // Fetch weather for the entered city
                        preferencesManager.saveLastSearchedCity(city) // Save the last searched city
                        keyboardController?.hide() // Hide the keyboard when the button is clicked
                    } else {
                        viewModel.error = "Please enter a city name." // Handle empty input
                    }
                }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            keyboardController?.hide()
            if (city.isNotEmpty()) {
                viewModel.fetchWeather(city)
                preferencesManager.saveLastSearchedCity(city) // Save the last searched city
                keyboardController?.hide()
            } else {
                viewModel.error = "Please enter a city name."
            }
        }) {
            Text(stringResource(id = R.string.get_weather))
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.loading) {
            CircularProgressIndicator()
        } else {
            viewModel.weather?.let { weather ->
                val tempInCelsius = weather.main.temp - 273.15
                val tempInFahrenheit = (tempInCelsius * 9 / 5) + 32
                val weatherIconUrl =
                    "https://openweathermap.org/img/wn/${weather.weather[0].icon}@2x.png"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Load and display the weather icon
                    Image(
                        painter = rememberImagePainter(weatherIconUrl),
                        contentDescription = "Weather Icon",
                        modifier = Modifier.size(64.dp) // Adjust size as needed
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Display the temperature
                    Text(
                        text = "Weather in ${weather.name}: ${tempInCelsius.toInt()}°C / ${tempInFahrenheit.toInt()}°F"
                    )
                }
            }

            if (viewModel.error.isNotEmpty()) {
                Text("Error: ${viewModel.error}", color = MaterialTheme.colors.error)
            }
        }
    }
}
