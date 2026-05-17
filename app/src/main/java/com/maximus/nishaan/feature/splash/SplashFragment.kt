package com.maximus.nishaan.feature.splash

import android.os.Bundle
import android.view.View
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.google.firebase.auth.FirebaseAuth
import com.maximus.nishaan.core.util.Constants
import com.maximus.nishaan.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Splash screen — shows logo with fade-in animation for 1.5s,
 * then navigates based on onboarding/auth state.
 */
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSplashBinding.bind(view)

        // Fade in logo over 500ms
        binding.logoText.animate()
            .alpha(1f)
            .setDuration(500)
            .start()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500) // 1.5 second splash duration

            val app = requireActivity().application as NishaanApplication
            val onboardingComplete = app.appContainer.dataStore.data
                .map { prefs ->
                    prefs[booleanPreferencesKey(Constants.PREF_ONBOARDING_COMPLETE)] ?: false
                }
                .first()

            if (!onboardingComplete) {
                findNavController().navigate(R.id.action_splash_to_onboarding)
            } else if (FirebaseAuth.getInstance().currentUser != null) {
                findNavController().navigate(R.id.action_splash_to_home)
            } else {
                findNavController().navigate(R.id.action_splash_to_auth)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
