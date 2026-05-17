package com.maximus.nishaan.feature.missing

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.maximus.nishaan.R
import com.maximus.nishaan.core.util.toShortId
import com.maximus.nishaan.databinding.FragmentConfirmationBinding

/** Confirmation screen after successful missing person report submission. */
class ConfirmationFragment : Fragment(R.layout.fragment_confirmation) {

    private var _binding: FragmentConfirmationBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentConfirmationBinding.bind(view)

        val personName = arguments?.getString("personName") ?: ""
        val reportId = arguments?.getString("reportId") ?: ""

        binding.confirmationBody.text = getString(R.string.confirmation_body, personName)
        binding.reportIdText.text = getString(R.string.confirmation_report_id, reportId.toShortId())

        binding.btnBackToHome.setOnClickListener { navigateHome() }

        // Prevent going back to submitted form
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { navigateHome() }
            })
    }

    private fun navigateHome() {
        findNavController().popBackStack(R.id.homeDashboardFragment, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
