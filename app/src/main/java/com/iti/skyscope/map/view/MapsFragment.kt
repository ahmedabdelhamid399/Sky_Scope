package com.iti.skyscope.map.view

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.MatrixCursor
import android.location.Geocoder
import android.os.Bundle
import android.provider.BaseColumns
import android.view.*
import android.widget.Toast
import com.iti.weatherwatch.R
import androidx.appcompat.widget.SearchView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.iti.weatherwatch.databinding.FragmentMapsBinding
import com.iti.skyscope.datasource.WeatherRepository
import com.iti.skyscope.map.viewmodel.MapViewModel
import com.iti.skyscope.map.viewmodel.MapViewModelFactory
import com.iti.skyscope.util.getSharedPreferences

/*
This is a Kotlin class that implements a map screen. It displays a map using the Google Maps API and allows the user to search for a location and move the marker to a new location on the map. The user can save this new location as a favorite or use it as a one-time location.

The class extends the Fragment class, and it overrides its onCreateView and onViewCreated methods. The onCreateView method inflates the fragment's layout file and returns the root view. The onViewCreated method initializes the view and sets the event listeners.

The class has several properties, including a lat and lon property that store the current location's latitude and longitude, and a binding property that is a reference to the fragment's binding object.

The class also has a view model property that is created using the MapViewModelFactory class, which is responsible for creating a new instance of the MapViewModel class. The MapViewModel is responsible for retrieving and updating the weather data.

The class uses the Google Maps API to display the map, and it sets up a callback that is called when the map is ready to be used. In the callback, the class sets up the map's initial state and registers the event listeners for the map.

The class also sets up a SearchView widget that allows the user to search for a location by entering a text query. The class uses the Geocoder class to convert the text query to a location, and it displays the location on the map as a marker.
 */
class MapsFragment : Fragment() {

    private var lat = 30.0
    private var lon = 30.0
    private var _binding: FragmentMapsBinding? = null

    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(WeatherRepository.getRepository(requireActivity().application))
    }

    private val binding get() = _binding!!

    private var isFavorite: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFavorite = requireArguments().getBoolean(getString(R.string.isFavorite))
        handleBackButton()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        binding.btnDone.setOnClickListener {
            if (isFavorite) {
                navigateToFavoriteScreen(lat, lon)
            } else {
                saveLocationInSharedPreferences(lon, lat)
            }
        }
    }

    private val callback = OnMapReadyCallback { googleMap ->
        val worldmap = LatLng(lat, lon)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(worldmap, 1.0f))
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.setOnMapClickListener { location ->
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(location))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10.0f))
            lat = location.latitude
            lon = location.longitude
            binding.btnDone.visibility = View.VISIBLE
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.isNotEmpty()) {
                    val geocoder = Geocoder(requireContext())
                    val addressList = geocoder.getFromLocationName(query, 1)
                    if (addressList != null && addressList.isNotEmpty()) {
                        val address = addressList[0]
                        val location = LatLng(address.latitude, address.longitude)
                        googleMap.addMarker(MarkerOptions().position(location))
                        googleMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                location,
                                10.0f
                            )
                        )
                        lat = address.latitude
                        lon = address.longitude
                        binding.btnDone.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No results found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val geocoder = Geocoder(requireContext())
                if (newText != null && newText != "") {
                    val addressList = geocoder.getFromLocationName(newText, 3)
                    val suggestions = mutableListOf<String>()
                    if (addressList != null) {
                        for (address in addressList) {
                            suggestions.add(address.getAddressLine(0))
                        }
                    }
                    val cursor = MatrixCursor(arrayOf(BaseColumns._ID, "address"))
                    for ((index, suggestion) in suggestions.withIndex()) {
                        cursor.addRow(arrayOf(index, suggestion))
                    }
                    val from = arrayOf("address")
                    val to = intArrayOf(android.R.id.text1)
                    val adapter = SimpleCursorAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        cursor,
                        from,
                        to,
                        CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
                    )
                    binding.searchView.suggestionsAdapter = adapter
                    return true
                }
                return false
            }
        })

        binding.searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                // This method is not used, but needs to be implemented
                return true
            }

            @SuppressLint("Range")
            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = binding.searchView.suggestionsAdapter.getItem(position) as Cursor
                val address = cursor.getString(cursor.getColumnIndex("address"))
                val geocoder = Geocoder(requireContext())
                val addressList = geocoder.getFromLocationName(address, 1)
                if (addressList != null && addressList.isNotEmpty()) {
                    val location = LatLng(addressList[0].latitude, addressList[0].longitude)
                    googleMap.addMarker(MarkerOptions().position(location))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10.0f))
                    lat = addressList[0].latitude
                    lon = addressList[0].longitude
                    binding.btnDone.visibility = View.VISIBLE
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No results found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                binding.searchView.setQuery("", false)
                binding.searchView.clearFocus()
                return true
            }
        })
    }

    private fun navigateToFavoriteScreen(lat: Double, lon: Double) {
        val language = getSharedPreferences(requireContext()).getString(
            getString(R.string.languageSetting),
            "en"
        )
        val units = getSharedPreferences(requireContext()).getString(
            getString(R.string.unitsSetting),
            "metric"
        )
        try {
            viewModel.setFavorite("$lat", "$lon", language!!, units!!)
            Navigation.findNavController(binding.root)
                .navigate(R.id.action_mapsFragment_to_navigation_dashboard)
        } catch (e: Exception) {
            val snackBar = Snackbar.make(binding.root, "${e.message}", Snackbar.LENGTH_SHORT)
            snackBar.show()
        }
    }

    private fun saveLocationInSharedPreferences(long: Double, lat: Double) {
        val editor = getSharedPreferences(this.requireContext()).edit()
        editor.putFloat(getString(R.string.lat), lat.toFloat())
        editor.putFloat(getString(R.string.lon), long.toFloat())
        editor.apply()
        Navigation.findNavController(binding.root)
            .navigate(R.id.action_mapsFragment_to_navigation_home)
    }

    private fun handleBackButton() {
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        binding.root.setOnKeyListener(View.OnKeyListener { view, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (isFavorite) {
                    Navigation.findNavController(view)
                        .navigate(R.id.action_mapsFragment_to_navigation_dashboard)
                } else {
                    Navigation.findNavController(view)
                        .navigate(R.id.action_mapsFragment_to_navigation_home)

                }
                return@OnKeyListener true
            }
            false
        })
    }
}
