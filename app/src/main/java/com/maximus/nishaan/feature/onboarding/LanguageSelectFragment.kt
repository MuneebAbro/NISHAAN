package com.maximus.nishaan.feature.onboarding

import android.os.Bundle
import android.view.View
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.core.util.Constants
import com.maximus.nishaan.databinding.FragmentLanguageSelectBinding
import kotlinx.coroutines.launch

/**
 * Language selection screen. Saves choice to DataStore and navigates to Permissions.
 */
class LanguageSelectFragment : Fragment(R.layout.fragment_language_select) {

    private var _binding: FragmentLanguageSelectBinding? = null
    private val binding get() = _binding!!
    private var selectedLanguage: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLanguageSelectBinding.bind(view)

        binding.cardEnglish.setOnClickListener { selectLanguage("en") }
        binding.cardUrdu.setOnClickListener { selectLanguage("ur") }
        binding.cardRomanUrdu.setOnClickListener { selectLanguage("roman_ur") }

        binding.btnContinue.setOnClickListener {
            val lang = selectedLanguage ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val app = requireActivity().application as NishaanApplication
                app.appContainer.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.PREF_LANGUAGE)] = lang
                }
                findNavController().navigate(R.id.action_language_to_permissions)
            }
        }
    }

    private fun selectLanguage(lang: String) {
        selectedLanguage = lang
        binding.btnContinue.isEnabled = true

        // Update checkmark visibility
        binding.checkEnglish.visibility = if (lang == "en") View.VISIBLE else View.GONE
        binding.checkUrdu.visibility = if (lang == "ur") View.VISIBLE else View.GONE
        binding.checkRomanUrdu.visibility = if (lang == "roman_ur") View.VISIBLE else View.GONE

        // Highlight selected card border
        val primary = requireContext().getColor(R.color.color_primary)
        val divider = requireContext().getColor(R.color.color_divider_dark)
        binding.cardEnglish.strokeColor = if (lang == "en") primary else divider
        binding.cardUrdu.strokeColor = if (lang == "ur") primary else divider
        binding.cardRomanUrdu.strokeColor = if (lang == "roman_ur") primary else divider
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
