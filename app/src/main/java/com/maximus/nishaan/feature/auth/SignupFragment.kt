package com.maximus.nishaan.feature.auth

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentSignupBinding
import com.maximus.nishaan.domain.model.User
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class SignupFragment : Fragment(R.layout.fragment_signup) {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { launchImageCrop(it) }
    }

    // UCrop result handler
    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val croppedUri = UCrop.getOutput(result.data!!)
            if (croppedUri != null) {
                selectedImageUri = croppedUri
                Glide.with(this).load(croppedUri).into(binding.profileImage)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSignupBinding.bind(view)

        val auth = FirebaseAuth.getInstance()
        val app = requireActivity().application as NishaanApplication

        binding.btnAddPhoto.setOnClickListener { pickImage.launch("image/*") }

        binding.btnComplete.setOnClickListener {
            val name = binding.nameInput.text?.toString()?.trim().orEmpty()
            val cnic = binding.cnicInput.text?.toString()?.trim().orEmpty()
            val user = auth.currentUser ?: return@setOnClickListener

            if (name.isEmpty()) {
                binding.nameInput.error = "Name is required"
                return@setOnClickListener
            }
            if (cnic.length < 13) {
                binding.cnicInput.error = getString(R.string.auth_cnic_error)
                return@setOnClickListener
            }
            if (selectedImageUri == null) {
                Snackbar.make(binding.root, R.string.auth_photo_required, Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            setLoading(true)
            lifecycleScope.launch {
                val uploadResult = app.appContainer.userRepository.uploadProfileImage(user.uid, selectedImageUri!!)
                if (!isAdded) return@launch

                uploadResult.fold(
                    onSuccess = { photoUrl ->
                        val newUser = User(
                            uid = user.uid,
                            email = user.email.orEmpty(),
                            displayName = name,
                            cnic = cnic,
                            photoUrl = photoUrl,
                            isVerified = true
                        )
                        lifecycleScope.launch {
                            val saveResult = app.appContainer.userRepository.saveUserProfile(newUser)
                            if (!isAdded) return@launch

                            saveResult.fold(
                                onSuccess = {
                                    if (findNavController().currentDestination?.id == R.id.signupFragment) {
                                        findNavController().navigate(R.id.action_signup_to_home)
                                    }
                                },
                                onFailure = { e ->
                                    setLoading(false)
                                    Snackbar.make(binding.root, e.localizedMessage ?: "Save failed", Snackbar.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    onFailure = { e ->
                        setLoading(false)
                        Snackbar.make(binding.root, e.localizedMessage ?: "Upload failed", Snackbar.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    /**
     * Launches UCrop for WhatsApp-style crop/adjust on the profile picture.
     * Uses a 1:1 aspect ratio for the circular profile image.
     */
    private fun launchImageCrop(sourceUri: Uri) {
        val destFile = File(requireContext().cacheDir, "nishaan_profile_cropped_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)

        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(90)
            setToolbarColor(ContextCompat.getColor(requireContext(), R.color.color_background_dark))
            setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.color_background_dark))
            setToolbarWidgetColor(ContextCompat.getColor(requireContext(), R.color.white))
            setActiveControlsWidgetColor(ContextCompat.getColor(requireContext(), R.color.color_signal_red))
            setCircleDimmedLayer(true)
            setShowCropGrid(false)
            setShowCropFrame(false)
            setFreeStyleCropEnabled(false)
        }

        val intent = UCrop.of(sourceUri, destUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
            .withOptions(options)
            .getIntent(requireContext())

        cropLauncher.launch(intent)
    }

    private fun setLoading(loading: Boolean) {
        binding.btnComplete.isEnabled = !loading
        binding.loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
