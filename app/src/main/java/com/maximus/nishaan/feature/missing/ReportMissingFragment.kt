package com.maximus.nishaan.feature.missing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.core.util.Constants
import com.maximus.nishaan.databinding.FragmentReportMissingBinding
import com.maximus.nishaan.domain.model.MissingPerson
import com.maximus.nishaan.domain.model.MissingPersonStatus
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Multi-step missing person report form (3 steps).
 * Step 1: Personal Details → Step 2: Location & Photo → Step 3: Contact Info → Submit
 */
class ReportMissingFragment : Fragment(R.layout.fragment_report_missing) {

    private var _binding: FragmentReportMissingBinding? = null
    private val binding get() = _binding!!
    private var currentStep = 1
    private var selectedPhotoBytes: ByteArray? = null

    // Camera launcher — takes a photo and returns a thumbnail bitmap
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            handleCapturedBitmap(bitmap)
        }
    }

    // Gallery picker — uses the modern Photo Picker API
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null && bytes.size > Constants.MAX_PHOTO_SIZE_BYTES) {
                    Snackbar.make(binding.root, R.string.report_photo_too_large, Snackbar.LENGTH_LONG).show()
                    return@registerForActivityResult
                }

                if (bytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        handleCapturedBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, R.string.error_generic, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReportMissingBinding.bind(view)

        setupRelationshipDropdown()
        updateStepUI()

        // Photo tap zone — show camera/gallery chooser
        binding.photoTapZone.setOnClickListener { showPhotoSourceDialog() }

        binding.btnNext.setOnClickListener {
            when (currentStep) {
                1 -> { if (validateStep1()) { currentStep = 2; updateStepUI() } }
                2 -> { currentStep = 3; updateStepUI() }
                3 -> { if (validateStep3()) submitReport() }
            }
        }

        binding.btnBack.setOnClickListener {
            if (currentStep > 1) { currentStep--; updateStepUI() }
        }
    }

    private fun showPhotoSourceDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.report_photo_label)
            .setItems(arrayOf(
                getString(R.string.report_photo_camera),
                getString(R.string.report_photo_gallery)
            )) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
            .show()
    }

    private fun handleCapturedBitmap(bitmap: Bitmap) {
        // Scale down if needed
        val scaled = scaleBitmap(bitmap, Constants.PHOTO_MAX_DIMENSION_PX)

        // Convert to JPEG bytes
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        selectedPhotoBytes = stream.toByteArray()

        // Show preview
        binding.photoPreview.setImageBitmap(scaled)
        binding.photoPreview.visibility = View.VISIBLE
        binding.photoPlaceholder.visibility = View.GONE
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun updateStepUI() {
        binding.stepIndicator.text = "Step $currentStep of 3"
        binding.step1Container.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        binding.step2Container.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        binding.step3Container.visibility = if (currentStep == 3) View.VISIBLE else View.GONE
        binding.btnBack.visibility = if (currentStep > 1) View.VISIBLE else View.GONE
        binding.btnNext.text = if (currentStep == 3) getString(R.string.btn_submit_report) else getString(R.string.btn_next)
    }

    private fun validateStep1(): Boolean {
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        val age = binding.ageInput.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            binding.nameLayout.error = "Name is required"
            return false
        }
        binding.nameLayout.error = null
        if (age.isEmpty()) {
            binding.ageLayout.error = "Age is required"
            return false
        }
        binding.ageLayout.error = null
        if (binding.genderToggle.checkedButtonId == View.NO_ID) {
            Snackbar.make(binding.root, "Please select gender", Snackbar.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun validateStep3(): Boolean {
        val phone = binding.phoneInput.text?.toString()?.trim().orEmpty()
        if (phone.isEmpty()) {
            binding.phoneLayout.error = "Phone number is required"
            return false
        }
        binding.phoneLayout.error = null
        if (!binding.confirmCheckbox.isChecked) {
            Snackbar.make(binding.root, "Please confirm the report accuracy", Snackbar.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun setupRelationshipDropdown() {
        val items = listOf("Father", "Mother", "Sibling", "Spouse", "Friend", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        binding.relationshipDropdown.setAdapter(adapter)
    }

    private fun submitReport() {
        binding.loadingOverlay.visibility = View.VISIBLE

        val gender = when (binding.genderToggle.checkedButtonId) {
            R.id.btnMale -> "male"
            R.id.btnFemale -> "female"
            else -> "other"
        }

        val person = MissingPerson(
            reportId = "",
            submittedByUid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest",
            reporterPhone = binding.phoneInput.text?.toString()?.trim().orEmpty(),
            reporterRelationship = binding.relationshipDropdown.text?.toString().orEmpty(),
            personName = binding.nameInput.text?.toString()?.trim().orEmpty(),
            personAge = binding.ageInput.text?.toString()?.toIntOrNull() ?: 0,
            personGender = gender,
            description = binding.descriptionInput.text?.toString()?.trim().orEmpty(),
            lastSeenLat = 24.8607,
            lastSeenLng = 67.0011,
            lastSeenAddress = binding.addressInput.text?.toString()?.trim().orEmpty(),
            photoUrl = null,
            linkedCrisisId = null,
            status = MissingPersonStatus.SEARCHING,
            matchScore = null,
            submittedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val app = requireActivity().application as NishaanApplication
        viewLifecycleOwner.lifecycleScope.launch {
            val result = app.appContainer.missingPersonRepository.submitReport(person, selectedPhotoBytes)
            binding.loadingOverlay.visibility = View.GONE
            result.fold(
                onSuccess = { reportId ->
                    val bundle = Bundle().apply {
                        putString("personName", person.personName)
                        putString("reportId", reportId)
                    }
                    findNavController().navigate(R.id.action_reportMissing_to_confirmation, bundle)
                },
                onFailure = {
                    Snackbar.make(binding.root, R.string.report_error_submission, Snackbar.LENGTH_LONG).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
