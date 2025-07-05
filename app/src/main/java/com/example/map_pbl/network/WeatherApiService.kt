package com.example.map_pbl.network

import com.map_pbl.model.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("weather")
    fun getWeather(
        //@Query("q") city: String = "London",
        @Query("lat") lat : String,
        @Query("lon") lon : String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Call<WeatherResponse>
}

