package com.maximus.nishaan.feature.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentAuthBinding
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

/**
 * Auth screen — Sign In (email/password) or Continue as Guest (anonymous).
 * Uses Firebase Auth for both flows.
 */
class AuthFragment : Fragment(R.layout.fragment_auth) {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAuthBinding.bind(view)

        // If already signed in, skip auth
        if (auth.currentUser != null) {
            navigateToHome()
            return
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.emailInput.text?.toString()?.trim().orEmpty()
            val password = binding.passwordInput.text?.toString().orEmpty()

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError(getString(R.string.auth_error_invalid_email))
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showError(getString(R.string.auth_error_empty_password))
                return@setOnClickListener
            }

            setLoading(true)
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { navigateToHome() }
                .addOnFailureListener { e ->
                    setLoading(false)
                    // If user doesn't exist, try creating account
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { navigateToHome() }
                        .addOnFailureListener { createError ->
                            setLoading(false)
                            showError(createError.localizedMessage ?: getString(R.string.error_generic))
                        }
                }
        }

        binding.btnGuest.setOnClickListener {
            setLoading(true)
            auth.signInAnonymously()
                .addOnSuccessListener { navigateToHome() }
                .addOnFailureListener { e ->
                    setLoading(false)
                    showError(e.localizedMessage ?: getString(R.string.error_generic))
                }
        }

        binding.btnCreateAccount.setOnClickListener {
            val email = binding.emailInput.text?.toString()?.trim().orEmpty()
            val password = binding.passwordInput.text?.toString().orEmpty()

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError(getString(R.string.auth_error_invalid_email))
                return@setOnClickListener
            }
            if (password.length < 6) {
                showError("Password must be at least 6 characters")
                return@setOnClickListener
            }

            setLoading(true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { navigateToHome() }
                .addOnFailureListener { e ->
                    setLoading(false)
                    showError(e.localizedMessage ?: getString(R.string.error_generic))
                }
        }
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignIn.isEnabled = !loading
        binding.btnGuest.isEnabled = !loading
        binding.btnCreateAccount.isEnabled = !loading
        binding.errorText.visibility = View.GONE
    }

    private fun navigateToHome() {
        val user = auth.currentUser
        if (user == null || user.isAnonymous) {
            if (isAdded && findNavController().currentDestination?.id == R.id.authFragment) {
                findNavController().navigate(R.id.action_auth_to_home)
            }
            return
        }

        val app = requireActivity().application as NishaanApplication
        lifecycleScope.launch {
            val result = app.appContainer.userRepository.getUserProfile(user.uid)
            if (!isAdded) return@launch

            result.fold(
                onSuccess = { profile ->
                    if (findNavController().currentDestination?.id == R.id.authFragment) {
                        if (profile != null && profile.isVerified) {
                            findNavController().navigate(R.id.action_auth_to_home)
                        } else {
                            findNavController().navigate(R.id.action_auth_to_signup)
                        }
                    }
                },
                onFailure = {
                    if (findNavController().currentDestination?.id == R.id.authFragment) {
                        findNavController().navigate(R.id.action_auth_to_signup)
                    }
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
