package com.maximus.nishaan.feature.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentSettingsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Settings screen. Manages theme preferences, notification settings, and app language.
 */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        val app = requireActivity().application as NishaanApplication
        val dataStore = app.appContainer.dataStore

        // Back button listener
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Language settings navigation
        binding.btnChangeLanguage.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_languageSelect)
        }

        // Sign out
        binding.btnSignOut.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.profile_sign_out_confirm)
                .setPositiveButton(R.string.btn_yes) { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    app.appContainer.userRepository.clearCache()
                    findNavController().navigate(R.id.action_settings_to_auth)
                }
                .setNegativeButton(R.string.btn_no, null)
                .show()
        }

        // Load and bind initial states from DataStore
        viewLifecycleOwner.lifecycleScope.launch {
            // Load theme
            val themeMode = dataStore.data
                .map { it[stringPreferencesKey("app_theme")] ?: "system" }
                .first()
            
            when (themeMode) {
                "light" -> binding.radioThemeLight.isChecked = true
                "dark" -> binding.radioThemeDark.isChecked = true
                else -> binding.radioThemeSystem.isChecked = true
            }

            // Load notification preferences
            binding.switchCritical.isChecked = dataStore.data.map { it[booleanPreferencesKey("notif_critical")] ?: true }.first()
            binding.switchMedium.isChecked = dataStore.data.map { it[booleanPreferencesKey("notif_medium")] ?: true }.first()
            binding.switchLow.isChecked = dataStore.data.map { it[booleanPreferencesKey("notif_low")] ?: false }.first()
            binding.switchMissing.isChecked = dataStore.data.map { it[booleanPreferencesKey("notif_missing")] ?: true }.first()

            // Setup preference listeners only AFTER values are initialized to avoid loop triggers
            setupPreferenceListeners(app)
        }
    }

    private fun setupPreferenceListeners(app: NishaanApplication) {
        val dataStore = app.appContainer.dataStore

        // Theme selection listener
        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val themeMode = when (checkedId) {
                R.id.radioThemeLight -> "light"
                R.id.radioThemeDark -> "dark"
                else -> "system"
            }

            viewLifecycleOwner.lifecycleScope.launch {
                dataStore.edit { prefs ->
                    prefs[stringPreferencesKey("app_theme")] = themeMode
                }
                applyTheme(themeMode)
            }
        }

        // Notification preferences listeners
        binding.switchCritical.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                dataStore.edit { prefs ->
                    prefs[booleanPreferencesKey("notif_critical")] = isChecked
                }
            }
        }

        binding.switchMedium.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                dataStore.edit { prefs ->
                    prefs[booleanPreferencesKey("notif_medium")] = isChecked
                }
            }
        }

        binding.switchLow.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                dataStore.edit { prefs ->
                    prefs[booleanPreferencesKey("notif_low")] = isChecked
                }
            }
        }

        binding.switchMissing.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                dataStore.edit { prefs ->
                    prefs[booleanPreferencesKey("notif_missing")] = isChecked
                }
            }
        }
    }

    private fun applyTheme(themeMode: String) {
        val mode = when (themeMode) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
