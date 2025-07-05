package com.example.map_pbl

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.map_pbl.network.WeatherApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.map_pbl.model.WeatherResponse
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var weatherApi: WeatherApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        weatherApi = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)

        requestLocationPermission()
    }


    private fun requestLocationPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        locationPermissionRequest.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                fetchWeather(location.latitude, location.longitude)
            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        val apiKey = "ABBCD" // Replace with actual API key
        val call = weatherApi.getWeather(lat.toString(), lon.toString(), apiKey)
        Log.e("Respo", "${call}")
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { weatherResponse ->
                        updateUI(weatherResponse)
                    }
                } else {
                    Log.e("WeatherAPI", "Error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WeatherAPI", "API call failed: ${call}", t)
            }
        })
    }


    private fun updateUI(weatherResponse: WeatherResponse) {
        val mainWeather = weatherResponse.main
        val weatherDesc = weatherResponse.weather[0].description

        findViewById<TextView>(R.id.weatherDescription).text = weatherDesc
        findViewById<TextView>(R.id.currentTemperature).text = "Temp: ${mainWeather.temp}°C"
        findViewById<TextView>(R.id.feelsLikeTemperature).text = "Feels Like: ${mainWeather.feelsLike}°C"
        findViewById<TextView>(R.id.humidity).text = "Humidity: ${mainWeather.humidity}%"
        findViewById<TextView>(R.id.windSpeed).text = "Wind: ${weatherResponse.wind.speed} m/s"

        // Display additional information
        findViewById<TextView>(R.id.cityName).text = "City: ${weatherResponse.cityName}"
        findViewById<TextView>(R.id.sunrise).text = "Sunrise: ${convertTimestamp(weatherResponse.system.sunrise)}"
        findViewById<TextView>(R.id.sunset).text = "Sunset: ${convertTimestamp(weatherResponse.system.sunset)}"

        val iconCode = weatherResponse.weather[0].icon // from API: example "01d", "10n", etc.
        val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"

        Glide.with(this)
            .load(iconUrl)
            .into(findViewById(R.id.weatherIcon))


        val currentTime = System.currentTimeMillis() / 1000
        val isNight = currentTime > weatherResponse.system.sunset || currentTime < weatherResponse.system.sunrise
        //BG IMAGE
        val imageUrl = when (weatherResponse.weather[0].main) {
            "Clear" -> if (isNight) "https://images.unsplash.com/photo-1502920917128-1aa500764b79?auto=format&fit=crop&w=1200&q=80" else "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80"
            "Clouds" -> "https://images.unsplash.com/photo-1499346030926-9a72daac6c63?auto=format&fit=crop&w=1200&q=80"
            "Rain", "Drizzle" -> "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?auto=format&fit=crop&w=1200&q=80"
            "Thunderstorm" -> "https://images.unsplash.com/photo-1601981081245-0a9ba3c9b3d8?auto=format&fit=crop&w=1200&q=80"
            "Snow" -> "https://images.unsplash.com/photo-1608889175119-bc443ebf637a?auto=format&fit=crop&w=1200&q=80"
            "Mist", "Fog", "Haze", "Smoke" -> "https://images.unsplash.com/photo-1485217988980-11786ced9454?auto=format&fit=crop&w=1200&q=80"
            "Dust", "Sand", "Ash" -> "https://images.unsplash.com/photo-1603394701775-8bcbe7bd42db?auto=format&fit=crop&w=1200&q=80"
            "Tornado", "Squall" -> "https://images.unsplash.com/photo-1598419401650-fc1b5e9cf714?auto=format&fit=crop&w=1200&q=80"
            else -> "https://images.unsplash.com/photo-1499346030926-9a72daac6c63?auto=format&fit=crop&w=1200&q=80"
        }
        Glide.with(this)
            .load(imageUrl)
            .into(findViewById(R.id.backgroundImage))
        val humidityIconURL = "https://cdn-icons-png.flaticon.com/128/727/727790.png"
        Glide.with(this)
            .load(humidityIconURL)
            .into(findViewById(R.id.humidity_icon))
        val windspeedIconURL = "https://cdn-icons-png.flaticon.com/128/959/959711.png"
        Glide.with(this)
            .load(windspeedIconURL)
            .into(findViewById(R.id.windSpeed_icon))
        val SunriseIconURL = "https://cdn-icons-png.flaticon.com/128/3920/3920639.png"
        Glide.with(this)
            .load(SunriseIconURL)
            .into(findViewById(R.id.sunrise_icon))
        val SunsetIconURL = "https://cdn-icons-png.flaticon.com/128/3920/3920728.png"
        Glide.with(this)
            .load(SunsetIconURL)
            .into(findViewById(R.id.sunset_icon))
        //Transition
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val cityNameTextView = findViewById<TextView>(R.id.cityName)
        cityNameTextView.startAnimation(fadeIn)

        val tempCard = findViewById<CardView>(R.id.tempCard) // use actual ID
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        tempCard.startAnimation(slideUp)


    }

    // Helper function to convert timestamp to readable time
    private fun convertTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp * 1000))
    }



}
