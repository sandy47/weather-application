package com.example.myapplication

import retrofit2.http.GET
import retrofit2.http.Query

interface GeoApi {
    @GET("direct")
    suspend fun getCoordinates(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("limit") limit: Int = 1 // You can adjust the limit as needed
    ): List<GeoResponse>
}
