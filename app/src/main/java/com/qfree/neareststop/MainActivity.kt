package com.qfree.neareststop

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled =true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        checkLocation()
    }

    private fun checkLocation(){
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showAlertLocation()
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocationUpdates()
    }

    private fun showAlertLocation() {
        val dialog = AlertDialog.Builder(this)
        dialog.setMessage("Your location settings is set to Off, Please enable location to use this application")
        dialog.setPositiveButton("Settings") { _, _ ->
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(myIntent)
        }
        dialog.setNegativeButton("Cancel") { _, _ ->
            finish()
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun getLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 2000
        locationRequest.smallestDisplacement = 5.0f //170 m = 0.1 mile
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //according to your app
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                if (locationResult.locations.isNotEmpty()) {
                    val currentLocation = GeoPoint(locationResult.lastLocation, "Текущая позиция")
                    val locations = arrayOf(
                        GeoPoint(56.173217,47.467644, "Сосновский поворот"),
                        GeoPoint(56.181488,47.426464, "Коллективный сад Заволжье"),
                        GeoPoint(56.172766,47.380344, "Астраханка"),
                        GeoPoint(56.171649,47.318466, "Санаторий Чувашия"),
                        GeoPoint(56.175548,47.291663, "Октябрьский"),
                        GeoPoint(56.186775,47.251416, "Коллективный сад"),
                        GeoPoint(56.187101,47.235781, "Сосновская"),
                        GeoPoint(56.187854,47.227643, "Сосновка")
                    )

                    GeoPoint.getNearestLocation(
                        currentLocation,
                        locations,
                        webView
                    )
                }
            }
        }
    }

    // Start location updates
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    // Stop location updates
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Stop receiving location update when activity not visible/foreground
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    // Start receiving location update when activity  visible/foreground
    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }
}

class GeoPoint {
    val lat: Double
    val lon: Double
    val name: String

    constructor(lat: Double, lon: Double, name: String) {
        this.lat = lat
        this.lon = lon
        this.name = name
    }

    constructor(location: Location, name: String) {
        this.lat = location.latitude
        this.lon = location.longitude
        this.name = name
    }

    companion object {
        fun getNearestLocation(current: GeoPoint, locations: Array<GeoPoint>, webView: WebView) {
            var res: GeoPoint? = null
            var lastDistance = Float.MAX_VALUE
            val locDistance = FloatArray(1)
            var yourData = "<table cellpadding=\"10\"><tbody><tr><th>Остановка</th><th>Расстояние (м)</th></tr>"

            for (loc in locations) {
                Location.distanceBetween(
                    current.lat, current.lon,
                    loc.lat, loc.lon, locDistance
                )

                yourData += "<tr><td>${loc.name}</td><td>${locDistance[0].toInt()}</td></tr>"

                if (res == null || locDistance[0] < lastDistance) {
                    res = loc
                    lastDistance = locDistance[0]
                }
            }

            yourData = yourData.replace(res?.name.toString(), "<strong><font color=\"red\">${res?.name}</font></strong>", true)
            yourData = yourData.replace(">" + lastDistance.toInt().toString() + "<", "><strong><font color=\"red\">${lastDistance.toInt()}</font></strong><", true)

            yourData += "</tbody></table>"
            webView.loadData(yourData, "text/html", "UTF-8")
        }
    }
}