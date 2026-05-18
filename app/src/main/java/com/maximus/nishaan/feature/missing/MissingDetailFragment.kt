package com.maximus.nishaan.feature.missing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentMissingDetailBinding
import com.maximus.nishaan.domain.model.MissingPerson
import com.maximus.nishaan.domain.model.MissingPersonStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.storage.FirebaseStorage

/**
 * Missing Person Detail screen — shows full report info,
 * MATCHER status, reporter contact, and Mark as Found action.
 */
class MissingDetailFragment : Fragment(R.layout.fragment_missing_detail) {

    private var _binding: FragmentMissingDetailBinding? = null
    private val binding get() = _binding!!
    private var currentPerson: MissingPerson? = null

    private val pickProofImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadProof(it) }
    }

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
        currentPerson = person
        binding.personName.text = person.personName
        binding.personDetails.text = "${person.personAge} yrs • ${person.personGender}"
        binding.descriptionText.text = person.description
        binding.lastSeenText.text = person.lastSeenAddress.ifEmpty { "${person.lastSeenLat}, ${person.lastSeenLng}" }
        binding.reporterPhoneText.text = person.reporterPhone
        binding.reporterRelationshipText.text = person.reporterRelationship

        binding.btnCallReporter.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${person.reporterPhone}")
            }
            startActivity(intent)
        }

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
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user == null || user.isAnonymous) {
                Snackbar.make(binding.root, R.string.missing_error_not_verified, Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val profileResult = app.appContainer.userRepository.getUserProfile(user.uid)
                profileResult.fold(
                    onSuccess = { profile ->
                        if (profile != null && profile.isVerified) {
                            showMarkFoundDialog(person)
                        } else {
                            Snackbar.make(binding.root, R.string.missing_error_not_verified, Snackbar.LENGTH_LONG).show()
                        }
                    },
                    onFailure = {
                        Snackbar.make(binding.root, R.string.error_generic, Snackbar.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    private fun showMarkFoundDialog(person: MissingPerson) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.missing_mark_found_proof_title)
            .setMessage(R.string.missing_mark_found_proof_desc)
            .setPositiveButton(R.string.btn_continue) { _, _ ->
                pickProofImage.launch("image/*")
            }
            .setNegativeButton(R.string.btn_no, null)
            .show()
    }

    private fun uploadProof(uri: Uri) {
        val app = requireActivity().application as NishaanApplication
        val person = currentPerson ?: return
        val user = FirebaseAuth.getInstance().currentUser ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            binding.btnMarkFound.isEnabled = false
            try {
                val ref = FirebaseStorage.getInstance().reference
                    .child("found_proofs/${person.reportId}_${user.uid}.jpg")
                ref.putFile(uri).await()
                val proofUrl = ref.downloadUrl.await().toString()

                val result = app.appContainer.missingPersonRepository.markAsFound(person.reportId, proofUrl, user.uid)
                result.fold(
                    onSuccess = {
                        Snackbar.make(binding.root, "${person.personName} marked as found with proof", Snackbar.LENGTH_LONG).show()
                        binding.btnMarkFound.visibility = View.GONE
                        binding.statusBadge.text = getString(R.string.missing_status_found)
                        binding.statusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_accent_low))
                    },
                    onFailure = {
                        binding.btnMarkFound.isEnabled = true
                        Snackbar.make(binding.root, R.string.error_generic, Snackbar.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                binding.btnMarkFound.isEnabled = true
                Snackbar.make(binding.root, "Upload failed: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
