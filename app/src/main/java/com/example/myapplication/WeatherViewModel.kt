package com.example.myapplication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

class WeatherViewModel(private val apiKey: String) : ViewModel() {
    var weather by mutableStateOf<WeatherResponse?>(null)
    var loading by mutableStateOf(false)
    var error by mutableStateOf("")

    fun fetchWeather(city: String) {
        viewModelScope.launch {
            loading = true
            try {
                val coordinates = fetchCoordinates(city)
                if (coordinates != null) {
                    val response =
                        fetchWeatherFromApi(coordinates.first, coordinates.second, apiKey)
                    weather = response
                    error = ""
                } else {
                    error = "Could not find coordinates for the city."
                }
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            } finally {
                loading = false
            }
        }
    }

    // New function to fetch weather by coordinates
    fun fetchWeatherByCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            loading = true
            try {
                val response = fetchWeatherFromApi(lat, lon, apiKey)
                weather = response
                error = ""
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            } finally {
                loading = false
            }
        }
    }

    private suspend fun fetchCoordinates(city: String): Pair<Double, Double>? {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/geo/1.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create<GeoApi>()
        val response = api.getCoordinates(city, apiKey)

        return if (response.isNotEmpty()) {
            val location = response[0] // Get the first match
            location.lat to location.lon // Return latitude and longitude
        } else {
            null // No coordinates found
        }
    }

    private suspend fun fetchWeatherFromApi(
        lat: Double,
        lon: Double,
        apiKey: String
    ): WeatherResponse? {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create<WeatherApi>()
        return api.getWeather(lat, lon, apiKey)
    }
}

class WeatherViewModelFactory(private val apiKey: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel(apiKey) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
