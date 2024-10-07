package com.example.myapplication

data class GeoResponse(
    val lat: Double,
    val lon: Double,
    val name: String,
    val country: String,
    val state: String? // State is optional, depending on the query
)
