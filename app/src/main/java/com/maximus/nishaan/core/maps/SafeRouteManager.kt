package com.maximus.nishaan.core.maps

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.maximus.nishaan.domain.model.Crisis
import kotlin.math.sqrt

/**
 * Utility manager to calculate safe evacuation destinations and safe routes (Feature 4).
 */
object SafeRouteManager {

    /**
     * Calculates the safe destination LatLng.
     * If inside a crisis area, calculates a vector leading out of the crisis.
     * Otherwise, falls back to a predefined shelter (Karachi Expo Centre).
     */
    fun calculateSafeDestination(userLocation: LatLng, activeCrises: List<Crisis>): LatLng {
        var activeDangerCrisis: Crisis? = null

        for (crisis in activeCrises) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                crisis.centroidLat, crisis.centroidLng,
                results
            )
            val distanceInMeters = results[0]
            val radiusInMeters = crisis.impactRadiusKm * 1000.0

            if (distanceInMeters <= radiusInMeters) {
                activeDangerCrisis = crisis
                break
            }
        }

        if (activeDangerCrisis != null) {
            val uLat = userLocation.latitude
            val uLng = userLocation.longitude
            val cLat = activeDangerCrisis.centroidLat
            val cLng = activeDangerCrisis.centroidLng
            val rKm = activeDangerCrisis.impactRadiusKm

            val diffLat = uLat - cLat
            val diffLng = uLng - cLng
            val dist = sqrt(diffLat * diffLat + diffLng * diffLng)

            return if (dist > 0) {
                // Vector away from center: D = U + 1.5 * (diff / dist) * (radius in degrees)
                // 1km ~ 0.009 degrees
                val radiusDegrees = rKm * 0.009
                val destLat = uLat + 1.5 * (diffLat / dist) * radiusDegrees
                val destLng = uLng + 1.5 * (diffLng / dist) * radiusDegrees
                LatLng(destLat, destLng)
            } else {
                LatLng(uLat + rKm * 0.009 * 1.5, uLng + rKm * 0.009 * 1.5)
            }
        } else {
            // Karachi Expo Centre
            return LatLng(24.8988, 67.0744)
        }
    }

    /**
     * Generates a curved safe route routing away from the nearest crisis center.
     */
    fun generateSafeRoutePoints(start: LatLng, destination: LatLng, activeCrises: List<Crisis>): List<LatLng> {
        val points = mutableListOf<LatLng>()
        points.add(start)

        val midLat = (start.latitude + destination.latitude) / 2.0
        val midLng = (start.longitude + destination.longitude) / 2.0
        val directMid = LatLng(midLat, midLng)

        var closestCrisis: Crisis? = null
        var minDistance = Double.MAX_VALUE

        for (crisis in activeCrises) {
            val results = FloatArray(1)
            Location.distanceBetween(
                directMid.latitude, directMid.longitude,
                crisis.centroidLat, crisis.centroidLng,
                results
            )
            val dist = results[0].toDouble()
            if (dist < minDistance) {
                minDistance = dist
                closestCrisis = crisis
            }
        }

        if (closestCrisis != null && minDistance < closestCrisis.impactRadiusKm * 1500) {
            // Curve away from closestCrisis center:
            val dLat = destination.latitude - start.latitude
            val dLng = destination.longitude - start.longitude
            
            // Perpendicular vector (-dLng, dLat)
            var perpLat = -dLng
            var perpLng = dLat
            val perpLength = sqrt(perpLat * perpLat + perpLng * perpLng)
            if (perpLength > 0) {
                perpLat /= perpLength
                perpLng /= perpLength
            }

            val cLat = closestCrisis.centroidLat
            val cLng = closestCrisis.centroidLng
            val testLat1 = directMid.latitude + perpLat * 0.015
            val testLng1 = directMid.longitude + perpLng * 0.015
            val testLat2 = directMid.latitude - perpLat * 0.015
            val testLng2 = directMid.longitude - perpLng * 0.015

            val dist1 = (testLat1 - cLat) * (testLat1 - cLat) + (testLng1 - cLng) * (testLng1 - cLng)
            val dist2 = (testLat2 - cLat) * (testLat2 - cLat) + (testLng2 - cLng) * (testLng2 - cLng)

            val shiftLat: Double
            val shiftLng: Double
            if (dist1 > dist2) {
                shiftLat = perpLat * 0.015
                shiftLng = perpLng * 0.015
            } else {
                shiftLat = -perpLat * 0.015
                shiftLng = -perpLng * 0.015
            }

            val midpoint = LatLng(directMid.latitude + shiftLat, directMid.longitude + shiftLng)
            
            for (i in 1..4) {
                val t = i / 5.0
                // Bezier curve
                val lat = (1 - t) * (1 - t) * start.latitude + 2 * (1 - t) * t * midpoint.latitude + t * t * destination.latitude
                val lng = (1 - t) * (1 - t) * start.longitude + 2 * (1 - t) * t * midpoint.longitude + t * t * destination.longitude
                points.add(LatLng(lat, lng))
            }
        } else {
            for (i in 1..4) {
                val t = i / 5.0
                val lat = start.latitude + t * (destination.latitude - start.latitude)
                val lng = start.longitude + t * (destination.longitude - start.longitude)
                points.add(LatLng(lat, lng))
            }
        }

        points.add(destination)
        return points
    }
}
