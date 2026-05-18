package com.maximus.nishaan.feature.home

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
 */
class HomeDashboardFragment : Fragment(R.layout.fragment_home_dashboard) {

    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var crisisAdapter: CrisisCardAdapter
    private var googleMap: GoogleMap? = null
    private var latestCrises: List<Crisis> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeDashboardBinding.bind(view)

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
                }
        }

        // FAB → Report Missing
        binding.fabReportMissing.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_reportMissing)
        }
    }

    private fun configureMap(map: GoogleMap) {
        // Dark mode map style
        try {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark))
        } catch (_: Exception) {
            // Fallback to default style if resource missing
        }

        // Default camera: Karachi, Pakistan
        val karachi = LatLng(24.8607, 67.0011)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(karachi, 11f))
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isCompassEnabled = false
    }

    private fun plotCrisesOnMap(crises: List<Crisis>) {
        val map = googleMap ?: return
        map.clear()

        if (crises.isEmpty()) return

        val boundsBuilder = LatLngBounds.builder()

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
        }

        // Zoom to fit all markers with padding
        try {
            val bounds = boundsBuilder.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } catch (_: Exception) {
            // Single point — just zoom to it
            val first = crises.first()
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(first.centroidLat, first.centroidLng), 12f)
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
