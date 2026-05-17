package com.maximus.nishaan.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.core.util.Constants
import com.maximus.nishaan.databinding.FragmentPermissionsBinding
import kotlinx.coroutines.launch

/**
 * Permissions screen — requests Location (required), Notifications (required),
 * and Camera (optional) in sequence.
 */
class PermissionsFragment : Fragment(R.layout.fragment_permissions) {

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateStatus(binding.statusLocation, granted)
        if (granted) requestNotificationPermission()
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateStatus(binding.statusNotification, granted)
        requestCameraPermission()
    }

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateStatus(binding.statusCamera, granted)
        checkAllGranted()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPermissionsBinding.bind(view)

        binding.btnGrant.setOnClickListener {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        binding.btnSkipCamera.setOnClickListener {
            navigateToAuth()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            updateStatus(binding.statusNotification, true)
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun updateStatus(statusView: android.widget.TextView, granted: Boolean) {
        if (granted) {
            statusView.text = "✅"
            statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_accent_low))
        } else {
            statusView.text = "❌"
            statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_primary))
        }
    }

    private fun checkAllGranted() {
        val locationGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (locationGranted) {
            binding.btnGrant.text = getString(R.string.btn_continue)
            binding.btnGrant.setOnClickListener { navigateToAuth() }
        } else {
            binding.btnSkipCamera.visibility = View.VISIBLE
        }
    }

    private fun navigateToAuth() {
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireActivity().application as NishaanApplication
            app.appContainer.dataStore.edit { prefs ->
                prefs[booleanPreferencesKey(Constants.PREF_ONBOARDING_COMPLETE)] = true
            }
            findNavController().navigate(R.id.action_permissions_to_auth)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
