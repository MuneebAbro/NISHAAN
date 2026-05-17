package com.maximus.nishaan.feature.missing

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.bumptech.glide.Glide
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentMissingDetailBinding
import com.maximus.nishaan.domain.model.MissingPerson
import com.maximus.nishaan.domain.model.MissingPersonStatus
import kotlinx.coroutines.launch

/**
 * Missing Person Detail screen — shows full report info,
 * MATCHER status, reporter contact, and Mark as Found action.
 */
class MissingDetailFragment : Fragment(R.layout.fragment_missing_detail) {

    private var _binding: FragmentMissingDetailBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMissingDetailBinding.bind(view)

        val reportId = arguments?.getString("reportId") ?: return
        val app = requireActivity().application as NishaanApplication

        viewLifecycleOwner.lifecycleScope.launch {
            val result = app.appContainer.missingPersonRepository.getMissingPersonById(reportId)
            result.fold(
                onSuccess = { person -> renderPerson(person, app) },
                onFailure = {
                    Snackbar.make(binding.root, R.string.error_generic, Snackbar.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun renderPerson(person: MissingPerson, app: NishaanApplication) {
        binding.personName.text = person.personName
        binding.personDetails.text = "${person.personAge} yrs • ${person.personGender}"
        binding.descriptionText.text = person.description
        binding.lastSeenText.text = person.lastSeenAddress.ifEmpty { "${person.lastSeenLat}, ${person.lastSeenLng}" }
        binding.reporterPhoneText.text = person.reporterPhone
        binding.reporterRelationshipText.text = person.reporterRelationship

        // Load photo from Firebase Storage URL
        if (!person.photoUrl.isNullOrEmpty()) {
            binding.personPhoto.visibility = View.VISIBLE
            Glide.with(this)
                .load(person.photoUrl)
                .centerCrop()
                .into(binding.personPhoto)
        } else {
            binding.personPhoto.visibility = View.GONE
        }

        // MATCHER status
        binding.matcherStatusText.text = getString(R.string.missing_detail_matcher_status, person.status.name)

        // Status badge
        when (person.status) {
            MissingPersonStatus.SEARCHING -> {
                binding.statusBadge.text = getString(R.string.missing_status_searching)
                binding.statusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_accent_info))
                binding.statusBadge.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_severity_monitoring_bg))
            }
            MissingPersonStatus.LINKED -> {
                binding.statusBadge.text = "Linked"
                binding.statusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_secondary))
                binding.statusBadge.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_severity_medium_bg))
                person.linkedCrisisId?.let {
                    binding.linkedCrisisText.text = getString(R.string.missing_detail_linked_crisis) + ": $it"
                    binding.linkedCrisisText.visibility = View.VISIBLE
                }
            }
            MissingPersonStatus.FOUND -> {
                binding.statusBadge.text = getString(R.string.missing_status_found)
                binding.statusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_accent_low))
                binding.statusBadge.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_severity_low_bg))
                binding.btnMarkFound.visibility = View.GONE
            }
            MissingPersonStatus.POTENTIAL_DUPLICATE -> {
                binding.statusBadge.text = "Duplicate?"
                binding.statusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_secondary))
                binding.statusBadge.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_severity_medium_bg))
            }
        }

        // Mark as Found
        binding.btnMarkFound.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.missing_detail_mark_found_confirm, person.personName))
                .setPositiveButton(R.string.btn_yes) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val result = app.appContainer.missingPersonRepository.markAsFound(person.reportId)
                        result.fold(
                            onSuccess = {
                                Snackbar.make(binding.root, "${person.personName} marked as found", Snackbar.LENGTH_LONG).show()
                                binding.btnMarkFound.visibility = View.GONE
                                binding.statusBadge.text = getString(R.string.missing_status_found)
                                binding.statusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_accent_low))
                            },
                            onFailure = {
                                Snackbar.make(binding.root, R.string.error_generic, Snackbar.LENGTH_LONG).show()
                            }
                        )
                    }
                }
                .setNegativeButton(R.string.btn_no, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
