package com.awad.addplace

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.awad.addplace.util.LocationModel

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

private const val TAG = "MapsActivity, myTag"

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var lat: Double = 0.0
    private var lon: Double = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val intent = intent
        lat = intent.getDoubleExtra("lat", 34.0)
        lon = intent.getDoubleExtra("lon", 24.0)

        Log.d(TAG, "onCreate: lon = $lon")
        Log.d(TAG, "onCreate: lat = $lat")

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera

        showMarkers()
    }

    private fun showMarkers() {

        val intent = intent
        val locations = intent.getParcelableArrayListExtra<LocationModel>("locations")

        for (location in locations!!) {
            val lat = location.lat
            val lng = location.lng
            val sydney = LatLng(lat, lng)

            mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Gaza"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))


        }
    }
}