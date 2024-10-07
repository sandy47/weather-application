package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    fun saveLastSearchedCity(city: String) {
        sharedPreferences.edit().putString("last_city", city).apply()
    }

    fun getLastSearchedCity(): String? {
        return sharedPreferences.getString("last_city", null)
    }
}
