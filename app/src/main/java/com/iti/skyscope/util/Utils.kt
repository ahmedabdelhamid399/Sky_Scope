package com.iti.skyscope.util

import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.iti.weatherwatch.R
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/*
This is a Kotlin file with utility functions for the "Weather Watch" app. The file contains functions to perform tasks such as getting an icon corresponding to a given weather condition, converting dates and times to the appropriate format, and checking if the device is connected to the internet. The functions in this file are called by various parts of the "Weather Watch" app, so it can be considered a helper file for the app.

The class contains the following functions:

    getIcon: This function takes a string as input (which represents a weather condition) and returns the corresponding icon as an integer.
    convertLongToTime: This function takes a Unix timestamp as input and returns a string representation of the time in 12-hour format (e.g. 3:30 PM).
    convertCalenderToDayString: This function takes a Calendar object as input and returns a string representation of the day of the week (e.g. Monday).
    convertLongToDayDate: This function takes a Unix timestamp as input and returns a string representation of the date (e.g. April 10, 2023).
    getSharedPreferences: This function takes a context as input and returns a SharedPreferences object for the app.
    isSharedPreferencesLocationAndTimeZoneNull: This function checks if the location and time zone fields in the app's SharedPreferences are null or empty and returns a boolean.
    isSharedPreferencesLatAndLongNull: This function checks if the latitude and longitude fields in the app's SharedPreferences are null or zero and returns a boolean.
    updateSharedPreferences: This function updates the latitude, longitude, location, and time zone fields in the app's SharedPreferences.
    isOnline: This function checks if the device is connected to the internet and returns a boolean.
    getCurrentLocale: This function returns the current locale of the device.
    getCityText: This function takes a latitude, longitude, and language as input and returns the name of the city corresponding to the given coordinates.
    convertNumbersToArabic: This function takes a number as input and returns a string representation of the number in Arabic numerals.
*/
fun getIcon(imageString: String): Int {
    val imageInInteger: Int
    when (imageString) {
        "01d" -> imageInInteger = R.drawable.icon_clearsky_day
        "01n" -> imageInInteger = R.drawable.icon_clearsky_night
        "02d" -> imageInInteger = R.drawable.icon_clouds_medium_day
        "02n" -> imageInInteger = R.drawable.icon_clouds_medium_night
        "03n" -> imageInInteger = R.drawable.icon_clouds_medium_day
        "03d" -> imageInInteger = R.drawable.icon_clouds_medium_night
        "04d" -> imageInInteger = R.drawable.icon_clouds_high
        "04n" -> imageInInteger = R.drawable.icon_clouds_high
        "09d" -> imageInInteger = R.drawable.icon_rain_medium_day
        "09n" -> imageInInteger = R.drawable.icon_rain_medium_night
        "10d" -> imageInInteger = R.drawable.icon_rain_high_day
        "10n" -> imageInInteger = R.drawable.icon_rain_high_night
        "11d" -> imageInInteger = R.drawable.icon_thunderstorm_day
        "11n" -> imageInInteger = R.drawable.icon_thunderstorm_night
        "13d" -> imageInInteger = R.drawable.icon_snow_day
        "13n" -> imageInInteger = R.drawable.icon_snow_night
        "50d" -> imageInInteger = R.drawable.icon_mist_day
        "50n" -> imageInInteger = R.drawable.icon_mist_night
        else  -> imageInInteger = R.drawable.icon_clouds_high
    }
    return imageInInteger
}

fun convertLongToTime(time: Long, language: String): String {
    val date = Date(TimeUnit.SECONDS.toMillis(time))
    val format = SimpleDateFormat("h:mm a", Locale(language))
    return format.format(date)
}

fun convertCalenderToDayString(calendar: Calendar, language: String): String {
    return calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale(language))
}

fun convertLongToDayDate(time: Long, language: String): String {
    val date = Date(time)
    val format = SimpleDateFormat("d MMM, yyyy", Locale(language))
    return format.format(date)
}

fun getSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(
        context.getString(R.string.shared_pref),
        Context.MODE_PRIVATE
    )
}

fun isSharedPreferencesLocationAndTimeZoneNull(context: Context): Boolean {
    val myPref = getSharedPreferences(context)
    val location = myPref.getString(context.getString(R.string.location), null)
    val timeZone = myPref.getString(context.getString(R.string.timeZone), null)
    return location.isNullOrEmpty() && timeZone.isNullOrEmpty()
}

fun isSharedPreferencesLatAndLongNull(context: Context): Boolean {
    val myPref = getSharedPreferences(context)
    val lat = myPref.getFloat(context.getString(R.string.lat), 0.0f)
    val long = myPref.getFloat(context.getString(R.string.lon), 0.0f)
    return lat == 0.0f && long == 0.0f
}

fun updateSharedPreferences(
    context: Context,
    lat: Double,
    long: Double,
    location: String,
    timeZone: String
) {
    val editor = getSharedPreferences(context).edit()
//    editor.clear()
    editor.putFloat(context.getString(R.string.lat), lat.toFloat())
    editor.putFloat(context.getString(R.string.lon), long.toFloat())
    editor.putString(context.getString(R.string.location), location)
    editor.putString(context.getString(R.string.timeZone), timeZone)
    editor.apply()
}

fun isOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities != null) {
        when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                return true
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> {
                return true
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                return true
            }
        }
    }
    return false
}

fun getCurrentLocale(context: Context): Locale? {
    return context.resources.configuration.locales[0]
}

fun getCityText(context: Context, lat: Double, lon: Double, language: String): String {
    var city = "Unknown!"
    val geocoder = Geocoder(context, Locale(language))
    try {
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        if (addresses!!.isNotEmpty()) {
            city = "${addresses[0].adminArea}, ${addresses[0].countryName}"
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
//        val knownName = addresses[0].featureName // elglaa
    return city
}

fun convertNumbersToArabic(value: Double): String {
    return (value.toString() + "")
        .replace("1".toRegex(), "١").replace("2".toRegex(), "٢")
        .replace("3".toRegex(), "٣").replace("4".toRegex(), "٤")
        .replace("5".toRegex(), "٥").replace("6".toRegex(), "٦")
        .replace("7".toRegex(), "٧").replace("8".toRegex(), "٨")
        .replace("9".toRegex(), "٩").replace("0".toRegex(), "٠")
}

fun convertNumbersToArabic(value: Int): String {
    return (value.toString() + "")
        .replace("1".toRegex(), "١").replace("2".toRegex(), "٢")
        .replace("3".toRegex(), "٣").replace("4".toRegex(), "٤")
        .replace("5".toRegex(), "٥").replace("6".toRegex(), "٦")
        .replace("7".toRegex(), "٧").replace("8".toRegex(), "٨")
        .replace("9".toRegex(), "٩").replace("0".toRegex(), "٠")
}
