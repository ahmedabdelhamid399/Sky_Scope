package com.iti.skyscope.favorites.view

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.iti.weatherwatch.R
import com.iti.weatherwatch.databinding.DisplayFavoriteWeatherFragmentBinding
import com.iti.skyscope.datasource.WeatherRepository
import com.iti.skyscope.datasource.model.*
import com.iti.skyscope.favorites.viewmodel.DisplayFavoriteViewModelFactory
import com.iti.skyscope.favorites.viewmodel.DisplayFavoriteWeatherViewModel
import com.iti.skyscope.home.view.*
import com.iti.skyscope.util.*
import java.util.*

/*
This is a Kotlin class in an Android app, specifically a Fragment that displays the weather information for a favorite location.

The imports at the top of the file import necessary classes from various packages in the app, including those for handling UI elements, navigation, data storage, and weather data.

The DisplayFavoriteWeather class extends the Fragment class, indicating that it is a UI component of the app that can be added to an activity to display a portion of the app's user interface. The class contains several properties, including adapters for the recycler views displaying weather data, latitude and longitude coordinates for the location, and units and language settings for the user.

The onCreateView method inflates the layout for the fragment and returns the root view. The onViewCreated method is called after the view is created, and it sets up the UI elements and event handlers for the fragment, including handling navigation to other parts of the app.

The initTimeRecyclerView and initDayRecyclerView methods set up the adapters for the recycler views that display the temperature data by day and by hour.

The setUnitSetting method sets the units for temperature and wind speed based on the language and user settings.

The fetchTempPerDayRecycler and fetchTempPerTimeRecycler methods set the data for the temperature recycler views, based on the daily and hourly temperature data from the OpenWeather API.

The setData method sets the data for the UI elements in the fragment based on the weather data returned by the OpenWeather API.

The handleBackButton method sets up the event handler for the back button, allowing the user to navigate back to the previous screen when the back button is pressed.
 */
class DisplayFavoriteWeather : Fragment() {
    private lateinit var tempPerDayAdapter: TempPerDayAdapter
    private lateinit var tempPerTimeAdapter: TempPerTimeAdapter
    private lateinit var windSpeedUnit: String
    private lateinit var temperatureUnit: String
    private val viewModel: DisplayFavoriteWeatherViewModel by viewModels {
        DisplayFavoriteViewModelFactory(WeatherRepository.getRepository(requireActivity().application))
    }

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var language: String = "en"
    private var units: String = "metric"

    private lateinit var binding: DisplayFavoriteWeatherFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DisplayFavoriteWeatherFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleBackButton()
        binding.btnBack.setOnClickListener {
            Navigation.findNavController(it)
                .navigate(R.id.action_displayFavoriteWeather_to_navigation_dashboard)
        }

        //tempPerHourAdapter
        initTimeRecyclerView()

        //tempPerDayAdapter
        initDayRecyclerView()
        val id = requireArguments().getInt("id")
        if (isOnline(requireContext())) {
            getOnlineNeeds()
            viewModel.updateWeather(latitude, longitude, units, language, id)
        } else {
            val snackBar = Snackbar.make(binding.root, "You are offline", Snackbar.LENGTH_LONG)
            snackBar.view.setBackgroundColor(Color.RED)
            snackBar.show()
            viewModel.getWeather(id)
        }
        viewModel.weather.observe(viewLifecycleOwner) {
            setUnitSetting(units)
            it?.let { it1 -> setData(it1) }
            fetchTempPerTimeRecycler(it?.hourly as ArrayList<Hourly>, temperatureUnit)
            fetchTempPerDayRecycler(it.daily as ArrayList<Daily>, temperatureUnit)
        }
    }

    private fun getOnlineNeeds() {
        latitude = requireArguments().getDouble("lat")
        longitude = requireArguments().getDouble("lon")
        units = getSharedPreferences(requireContext()).getString(
            getString(R.string.unitsSetting),
            "metric"
        )!!
        language = getSharedPreferences(requireContext()).getString(
            getString(R.string.languageSetting),
            "en"
        )!!
    }

    private fun initTimeRecyclerView() {
        val tempPerTimeLinearLayoutManager = LinearLayoutManager(HomeFragment().context)
        tempPerTimeLinearLayoutManager.orientation = RecyclerView.HORIZONTAL
        tempPerTimeAdapter = TempPerTimeAdapter(this.requireContext())
        binding.recyclerViewTempPerTime.layoutManager = tempPerTimeLinearLayoutManager
        binding.recyclerViewTempPerTime.adapter = tempPerTimeAdapter
    }

    private fun initDayRecyclerView() {
        val tempPerDayLinearLayoutManager = LinearLayoutManager(HomeFragment().context)
        tempPerDayAdapter = TempPerDayAdapter(this.requireContext())
        binding.recyclerViewTempPerDay.layoutManager = tempPerDayLinearLayoutManager
        binding.recyclerViewTempPerDay.adapter = tempPerDayAdapter
    }

    private fun setUnitSetting(units: String) {
        if (language == "en") {
            setEnglishUnits(units)
        } else {
            setArabicUnit(units)
        }
    }

    private fun fetchTempPerDayRecycler(daily: ArrayList<Daily>, temperatureUnit: String) {
        tempPerDayAdapter.apply {
            this.daily = daily
            this.temperatureUnit = temperatureUnit
            notifyDataSetChanged()
        }
    }

    private fun fetchTempPerTimeRecycler(hourly: ArrayList<Hourly>, temperatureUnit: String) {
        tempPerTimeAdapter.apply {
            this.hourly = hourly
            this.temperatureUnit = temperatureUnit
            notifyDataSetChanged()
        }
    }

    private fun setData(model: OpenWeatherApi) {
        val weather = model.current.weather[0]
        binding.apply {
            imageWeatherIcon.setImageResource(getIcon(weather.icon))
            textCurrentDay.text = convertCalenderToDayString(Calendar.getInstance(), language)
            textCurrentDate.text =
                convertLongToDayDate(Calendar.getInstance().timeInMillis, language)
            textTempDescription.text = weather.description
            textCity.text = getCityText(requireContext(), model.lat, model.lon, language)
            if (language == "ar") {
                bindArabicUnits(model)
            } else {
                bindEnglishUnits(model)
            }
        }
    }

    private fun handleBackButton() {
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        binding.root.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                Navigation.findNavController(v)
                    .navigate(R.id.action_displayFavoriteWeather_to_navigation_dashboard)
                return@OnKeyListener true
            }
            return@OnKeyListener false
        })
    }

    private fun setArabicUnit(units: String) {
        when (units) {
            "metric"   -> {
                temperatureUnit = " °م"
                windSpeedUnit = " م/ث"
            }
            "imperial" -> {
                temperatureUnit = " °ف"
                windSpeedUnit = " ميل/س"
            }
            "standard" -> {
                temperatureUnit = " °ك"
                windSpeedUnit = " م/ث"
            }
        }
    }

    private fun setEnglishUnits(units: String) {
        when (units) {
            "metric"   -> {
                temperatureUnit = " °C"
                windSpeedUnit = " m/s"
            }
            "imperial" -> {
                temperatureUnit = " °F"
                windSpeedUnit = " miles/h"
            }
            "standard" -> {
                temperatureUnit = " °K"
                windSpeedUnit = " m/s"
            }
        }
    }

    private fun bindArabicUnits(model: OpenWeatherApi) {
        binding.apply {
            textCurrentTempreture.text =
                convertNumbersToArabic(model.current.temp.toInt()).plus(temperatureUnit)
            textHumidity.text = convertNumbersToArabic(model.current.humidity)
                .plus("٪")
            textPressure.text = convertNumbersToArabic(model.current.pressure)
                .plus(" هب")
            textClouds.text = convertNumbersToArabic(model.current.clouds)
                .plus("٪")
            textVisibility.text = convertNumbersToArabic(model.current.visibility)
                .plus("م")
            textUvi.text = convertNumbersToArabic(model.current.uvi.toInt())
            textWindSpeed.text =
                convertNumbersToArabic(model.current.windSpeed).plus(windSpeedUnit)
        }
    }

    private fun bindEnglishUnits(model: OpenWeatherApi) {
        binding.apply {
            textCurrentTempreture.text = model.current.temp.toInt().toString().plus(temperatureUnit)
            textHumidity.text = model.current.humidity.toString().plus("%")
            textPressure.text = model.current.pressure.toString().plus(" hPa")
            textClouds.text = model.current.clouds.toString().plus("%")
            textVisibility.text = model.current.visibility.toString().plus("m")
            textUvi.text = model.current.uvi.toString()
            textWindSpeed.text = model.current.windSpeed.toString().plus(windSpeedUnit)
        }
    }

}
