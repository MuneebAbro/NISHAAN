package com.maximus.nishaan.feature.home

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.domain.model.Crisis
import com.maximus.nishaan.domain.model.Severity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class FullScreenMapFragment : Fragment(R.layout.fragment_full_screen_map) {

    private var googleMap: GoogleMap? = null
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: LatLng? = null
    private var latestCrises: List<Crisis> = emptyList()

    private var activePolyline: com.google.android.gms.maps.model.Polyline? = null
    private var safeDestinationMarker: com.google.android.gms.maps.model.Marker? = null
    private var activeRoutePoints: List<LatLng>? = null
    private var activeSafeDestination: LatLng? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        view.findViewById<View>(R.id.btnCollapseMap).setOnClickListener {
            findNavController().popBackStack()
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
        mapFragment?.getMapAsync { map ->
            googleMap = map
            configureMap(map)
            
            // Move camera to args
            val lat = arguments?.getFloat("latitude")?.toDouble() ?: 0.0
            val lng = arguments?.getFloat("longitude")?.toDouble() ?: 0.0
            val zoom = arguments?.getFloat("zoom") ?: 12f
            
            if (lat != 0.0 && lng != 0.0) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoom))
            }
            
            fetchUserLocation()
            observeCrises()
        }
        
        setupSafeRouteListeners(view)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun fetchUserLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLocation = LatLng(location.latitude, location.longitude)
                // Evaluate if user is in danger and should see the Safe Route button
                checkUserSafetyStatus()
            }
        }
    }

    private fun checkUserSafetyStatus() {
        val userLoc = userLocation ?: return
        var isInDanger = false
        for (crisis in latestCrises) {
            val crisisLatLng = LatLng(crisis.centroidLat, crisis.centroidLng)
            val results = FloatArray(1)
            Location.distanceBetween(
                userLoc.latitude, userLoc.longitude,
                crisisLatLng.latitude, crisisLatLng.longitude,
                results
            )
            if (results[0] <= crisis.impactRadiusKm * 1000.0) {
                isInDanger = true
                break
            }
        }

        val fabSafeRoute = view?.findViewById<View>(R.id.fabSafeRoute)
        if (isInDanger) {
            fabSafeRoute?.visibility = View.VISIBLE
        } else {
            fabSafeRoute?.visibility = View.GONE
        }
    }

    private fun setupSafeRouteListeners(view: View) {
        val fabSafeRoute = view.findViewById<View>(R.id.fabSafeRoute)
        val chipSafeRouteLoading = view.findViewById<View>(R.id.chipSafeRouteLoading)
        val bannerSafeRoute = view.findViewById<View>(R.id.bannerSafeRoute)
        val btnDismissSafeRoute = view.findViewById<View>(R.id.btnDismissSafeRoute)

        fabSafeRoute.setOnClickListener {
            val userLoc = userLocation
            if (userLoc == null) {
                android.widget.Toast.makeText(requireContext(), "Location required to calculate safe route", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            chipSafeRouteLoading.visibility = View.VISIBLE
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(1000) // Simulating network/Directions API delay
                
                val destination = com.maximus.nishaan.core.maps.SafeRouteManager.calculateSafeDestination(userLoc, latestCrises)
                val routePoints = com.maximus.nishaan.core.maps.SafeRouteManager.generateSafeRoutePoints(userLoc, destination, latestCrises)
                
                activeRoutePoints = routePoints
                activeSafeDestination = destination
                
                plotCrisesOnMap(latestCrises)
                
                chipSafeRouteLoading.visibility = View.GONE
                bannerSafeRoute.visibility = View.VISIBLE
                
                try {
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                        .include(userLoc)
                        .include(destination)
                        .build()
                    googleMap?.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 120))
                } catch (e: Exception) {
                    googleMap?.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(destination, 13f))
                }
            }
        }

        btnDismissSafeRoute.setOnClickListener {
            activeRoutePoints = null
            activeSafeDestination = null
            activePolyline?.remove()
            activePolyline = null
            safeDestinationMarker?.remove()
            safeDestinationMarker = null
            bannerSafeRoute.visibility = View.GONE
            plotCrisesOnMap(latestCrises)
        }
    }

    private fun configureMap(map: GoogleMap) {
        val uiMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        try {
            if (isNightMode) {
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark))
            } else {
                map.setMapStyle(null)
            }
        } catch (e: Exception) {
            android.util.Log.e("FullScreenMapFragment", "Failed to apply map style.", e)
        }

        map.uiSettings.isZoomControlsEnabled = true
        try {
            map.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            // Permission might not be granted, ignore
        }
        map.uiSettings.isMyLocationButtonEnabled = true
        map.uiSettings.isCompassEnabled = true
    }

    private fun observeCrises() {
        val app = requireActivity().application as NishaanApplication
        viewLifecycleOwner.lifecycleScope.launch {
            app.appContainer.crisisRepository.observeActiveCrises()
                .catch { /* Handle error silently */ }
                .collect { crises ->
                    latestCrises = crises
                    plotCrisesOnMap(crises)
                    checkUserSafetyStatus()
                }
        }
    }

    private fun plotCrisesOnMap(crises: List<Crisis>) {
        val map = googleMap ?: return
        map.clear()

        crises.forEach { crisis ->
            val position = LatLng(crisis.centroidLat, crisis.centroidLng)
            val markerColor = getSeverityHue(crisis.severity)
            val circleStrokeColor = getSeverityColor(crisis.severity)
            val circleFillColor = getSeverityFillColor(crisis.severity)

            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(crisis.titleEn)
                    .snippet("${crisis.severity.name} • ${crisis.crisisType.name.replace("_", " ")}")
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(markerColor))
            )

            map.addCircle(
                CircleOptions()
                    .center(position)
                    .radius(crisis.impactRadiusKm * 1000)
                    .strokeColor(circleStrokeColor)
                    .fillColor(circleFillColor)
                    .strokeWidth(4f)
            )

            crisis.spreadPrediction?.let { prediction ->
                val strokeColor = ContextCompat.getColor(requireContext(), R.color.color_severity_monitoring)
                val fillColor = ContextCompat.getColor(requireContext(), R.color.color_severity_monitoring_bg)
                map.addCircle(
                    CircleOptions()
                        .center(position)
                        .radius(prediction.predictedRadiusKm * 1000)
                        .strokeColor(strokeColor)
                        .fillColor(fillColor)
                        .strokeWidth(3f)
                        .strokePattern(listOf(com.google.android.gms.maps.model.Dash(20f), com.google.android.gms.maps.model.Gap(10f)))
                )
            }
        }

        val route = activeRoutePoints
        val dest = activeSafeDestination
        if (route != null && dest != null) {
            val polylineOptions = com.google.android.gms.maps.model.PolylineOptions()
                .addAll(route)
                .color(android.graphics.Color.parseColor("#2E7D32"))
                .width(12f)
            activePolyline = map.addPolyline(polylineOptions)

            safeDestinationMarker = map.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(dest)
                    .title("Safe Evacuation Zone")
                    .snippet("Evacuate to this coordinated shelter.")
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN))
            )
        }
    }

    private fun getSeverityHue(severity: Severity): Float = when (severity) {
        Severity.CRITICAL -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
        Severity.HIGH -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
        Severity.MEDIUM -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_YELLOW
        Severity.LOW -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
        Severity.MONITORING -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
    }

    private fun getSeverityColor(severity: Severity): Int =
        ContextCompat.getColor(requireContext(), when (severity) {
            Severity.CRITICAL -> R.color.color_geofence_stroke_40
            Severity.HIGH -> R.color.color_geofence_stroke_40
            Severity.MEDIUM -> R.color.color_severity_medium
            Severity.LOW -> R.color.color_severity_low
            Severity.MONITORING -> R.color.color_severity_monitoring
        })

    private fun getSeverityFillColor(severity: Severity): Int =
        ContextCompat.getColor(requireContext(), when (severity) {
            Severity.CRITICAL -> R.color.color_geofence_fill_10
            Severity.HIGH -> R.color.color_geofence_fill_10
            Severity.MEDIUM -> R.color.color_severity_medium_bg
            Severity.LOW -> R.color.color_severity_low_bg
            Severity.MONITORING -> R.color.color_severity_monitoring_bg
        })

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
    }
}
