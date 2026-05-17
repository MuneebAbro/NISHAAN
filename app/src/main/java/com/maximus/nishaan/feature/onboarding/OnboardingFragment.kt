package com.maximus.nishaan.feature.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentOnboardingBinding

/**
 * Onboarding flow with 3 slides via ViewPager2.
 * Skip → LanguageSelect. Last slide "Get Started" → LanguageSelect.
 */
class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        val adapter = OnboardingPagerAdapter()
        binding.viewPager.adapter = adapter

        // Connect dot indicator to ViewPager2
        TabLayoutMediator(binding.tabIndicator, binding.viewPager) { _, _ -> }.attach()

        // Update button text on page change
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.btnNext.text = if (position == adapter.itemCount - 1) {
                    getString(R.string.btn_get_started)
                } else {
                    getString(R.string.btn_next)
                }
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < adapter.itemCount - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                navigateToLanguageSelect()
            }
        }

        binding.btnSkip.setOnClickListener {
            navigateToLanguageSelect()
        }
    }

    private fun navigateToLanguageSelect() {
        findNavController().navigate(R.id.action_onboarding_to_language)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
