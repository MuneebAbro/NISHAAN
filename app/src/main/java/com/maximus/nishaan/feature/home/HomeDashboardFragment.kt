package com.maximus.nishaan.feature.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.Location
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentHomeDashboardBinding
import com.maximus.nishaan.domain.model.Crisis
import com.maximus.nishaan.domain.model.Severity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Home Dashboard — the root destination.
 * Shows real-time crisis alert banner from Firestore, Google Maps with crisis markers,
 * bottom nav, and FAB for reporting missing persons.
 * Detects current location and alerts the user if they are inside any crisis areas.
 */
class HomeDashboardFragment : Fragment(R.layout.fragment_home_dashboard) {

    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var crisisAdapter: CrisisCardAdapter
    private var googleMap: GoogleMap? = null
    private var latestCrises: List<Crisis> = emptyList()
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: LatLng? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            enableMyLocationOnMapAndCheckSafety()
        } else {
            android.util.Log.d("HomeDashboardFragment", "Location permissions denied. Centering on whole Pakistan.")
            centerCameraOnPakistan()
            updateSafetyStatusCard(null)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeDashboardBinding.bind(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Show initial safety card state immediately so it is visible and clickable right away
        updateSafetyStatusCard(null)

        // Setup crisis alert banner
        crisisAdapter = CrisisCardAdapter { crisis -> navigateToCrisisDetail(crisis) }
        binding.alertBannerRecycler.adapter = crisisAdapter
        binding.alertBannerRecycler.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )

        // Initialize Google Maps
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
        mapFragment?.getMapAsync { map ->
            googleMap = map
            configureMap(map)
            // Check permissions and position map camera
            checkLocationPermissions()
            // If crises loaded before map was ready, plot them now
            if (latestCrises.isNotEmpty()) plotCrisesOnMap(latestCrises)
        }

        // Observe active crises from Firestore
        val app = requireActivity().application as NishaanApplication
        viewLifecycleOwner.lifecycleScope.launch {
            app.appContainer.crisisRepository.observeActiveCrises()
                .catch { /* Handle error silently for now */ }
                .collect { crises ->
                    val sorted = crises.sortedWith(
                        compareBy<Crisis> { it.severity.ordinal }
                            .thenByDescending { it.createdAt }
                    ).take(5)
                    crisisAdapter.submitList(sorted)
                    binding.alertBannerRecycler.visibility =
                        if (sorted.isEmpty()) View.GONE else View.VISIBLE

                    latestCrises = sorted
                    googleMap?.let { plotCrisesOnMap(sorted) }

                    // Recheck safety status if user location is already set
                    userLocation?.let { checkUserSafetyStatus(it, sorted) }
                }
        }

        // Safety Status Row click listeners
        val safetyClickListener = View.OnClickListener {
            val userLoc = userLocation
            if (userLoc == null) {
                android.util.Log.d("HomeDashboardFragment", "Safety status clicked while location is disabled. Requesting permissions.")
                checkLocationPermissions()
            } else {
                android.util.Log.d("HomeDashboardFragment", "Safety status clicked. Centering camera on user location: $userLoc")
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 14f))
            }
        }
        binding.safetyStatusRow.setOnClickListener(safetyClickListener)
        binding.txtSafetyStatus.setOnClickListener(safetyClickListener)

        // FAB → Report Missing
        binding.fabReportMissing.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_reportMissing)
        }
    }

    private fun configureMap(map: GoogleMap) {
        val uiMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        android.util.Log.d("HomeDashboardFragment", "configureMap: Initializing map. uiMode=$uiMode, isNightMode=$isNightMode")
        
        try {
            if (isNightMode) {
                android.util.Log.d("HomeDashboardFragment", "configureMap: Applying dark mode map style (map_style_dark)")
                val success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark))
                android.util.Log.d("HomeDashboardFragment", "configureMap: Dark style setMapStyle success status = $success")
            } else {
                android.util.Log.d("HomeDashboardFragment", "configureMap: Applying standard light mode map style (null)")
                val success = map.setMapStyle(null)
                android.util.Log.d("HomeDashboardFragment", "configureMap: Light style setMapStyle success status = $success")
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeDashboardFragment", "configureMap: Failed to apply map style. Error: ${e.message}", e)
        }

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isCompassEnabled = false
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocationOnMapAndCheckSafety()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationOnMapAndCheckSafety() {
        val map = googleMap ?: return
        try {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        } catch (e: Exception) {
            android.util.Log.e("HomeDashboardFragment", "Error enabling isMyLocationEnabled: ${e.message}", e)
        }

        android.util.Log.d("HomeDashboardFragment", "enableMyLocationOnMapAndCheckSafety: Fetching user's last location")
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                android.util.Log.d("HomeDashboardFragment", "User location fetched successfully: $userLatLng")
                
                userLocation = userLatLng
                // Center map camera on user's location
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12f))
                
                // Plot crises (which will now encompass user's location inside bounds builder)
                plotCrisesOnMap(latestCrises)
                
                checkUserSafetyStatus(userLatLng, latestCrises)
            } else {
                android.util.Log.d("HomeDashboardFragment", "lastLocation returned null. Centering on whole Pakistan.")
                centerCameraOnPakistan()
                updateSafetyStatusCard(null)
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("HomeDashboardFragment", "Failed to get lastLocation. Error: ${e.message}", e)
            centerCameraOnPakistan()
            updateSafetyStatusCard(null)
        }
    }

    private fun centerCameraOnPakistan() {
        val map = googleMap ?: return
        android.util.Log.d("HomeDashboardFragment", "centerCameraOnPakistan: Animating camera to whole Pakistan")
        val pakistan = LatLng(30.3753, 69.3451)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pakistan, 5.5f))
    }

    private fun checkUserSafetyStatus(userLatLng: LatLng, crises: List<Crisis>) {
        if (_binding == null) return

        var activeDangerCrisis: Crisis? = null

        for (crisis in crises) {
            val crisisLatLng = LatLng(crisis.centroidLat, crisis.centroidLng)
            val results = FloatArray(1)
            Location.distanceBetween(
                userLatLng.latitude, userLatLng.longitude,
                crisisLatLng.latitude, crisisLatLng.longitude,
                results
            )
            val distanceInMeters = results[0]
            val radiusInMeters = crisis.impactRadiusKm * 1000.0

            android.util.Log.d("HomeDashboardFragment", "checkUserSafetyStatus: Distance to ${crisis.titleEn} is ${distanceInMeters}m (Radius: ${radiusInMeters}m)")

            if (distanceInMeters <= radiusInMeters) {
                activeDangerCrisis = crisis
                break
            }
        }

        updateSafetyStatusCard(activeDangerCrisis, userLatLng)
    }

    private fun updateSafetyStatusCard(dangerCrisis: Crisis?, userLatLng: LatLng? = null) {
        val binding = _binding ?: return

        if (userLatLng == null) {
            // Location disabled
            binding.viewStatusDot.setBackgroundResource(R.drawable.circle_pulse_green)
            binding.viewStatusDot.background?.setTint(
                ContextCompat.getColor(requireContext(), R.color.color_chalk)
            )
            binding.txtSafetyStatus.text = "Location disabled"
            binding.txtSafetyStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.color_chalk)
            )
            return
        }

        if (dangerCrisis != null) {
            // Warning: inside crisis area
            binding.viewStatusDot.setBackgroundResource(R.drawable.circle_pulse_green)
            binding.viewStatusDot.background?.setTint(
                ContextCompat.getColor(requireContext(), R.color.color_severity_critical)
            )
            binding.txtSafetyStatus.text = "Warning: Inside Crisis Area! (${dangerCrisis.titleEn})"
            binding.txtSafetyStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.color_severity_critical)
            )
        } else {
            // Safe
            binding.viewStatusDot.setBackgroundResource(R.drawable.circle_pulse_green)
            binding.viewStatusDot.background?.setTint(
                ContextCompat.getColor(requireContext(), R.color.color_severity_low)
            )
            binding.txtSafetyStatus.text = "You're Safe: No active crises nearby"
            binding.txtSafetyStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.color_severity_low)
            )
        }
    }

    private fun plotCrisesOnMap(crises: List<Crisis>) {
        val map = googleMap ?: return
        map.clear()

        val boundsBuilder = LatLngBounds.builder()
        var hasPoints = false

        crises.forEach { crisis ->
            val position = LatLng(crisis.centroidLat, crisis.centroidLng)
            val markerColor = getSeverityHue(crisis.severity)
            val circleStrokeColor = getSeverityColor(crisis.severity)
            val circleFillColor = getSeverityFillColor(crisis.severity)

            // Add marker
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(crisis.titleEn)
                    .snippet("${crisis.severity.name} • ${crisis.crisisType.name.replace("_", " ")}")
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(markerColor))
            )

            // Add geofence circle (impact radius)
            map.addCircle(
                CircleOptions()
                    .center(position)
                    .radius(crisis.impactRadiusKm * 1000) // km → meters
                    .strokeColor(circleStrokeColor)
                    .fillColor(circleFillColor)
                    .strokeWidth(4f)
            )

            boundsBuilder.include(position)
            hasPoints = true
        }

        userLocation?.let {
            boundsBuilder.include(it)
            hasPoints = true
        }

        if (hasPoints) {
            try {
                val bounds = boundsBuilder.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (_: Exception) {
                val center = userLocation ?: LatLng(crises.first().centroidLat, crises.first().centroidLng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 12f))
            }
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

    private fun navigateToCrisisDetail(crisis: Crisis) {
        val bundle = Bundle().apply { putString("crisisId", crisis.crisisId) }
        findNavController().navigate(R.id.action_home_to_crisisDetail, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        _binding = null
    }
}
