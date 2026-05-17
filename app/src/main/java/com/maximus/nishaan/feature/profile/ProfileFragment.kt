package com.maximus.nishaan.feature.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentProfileBinding
import com.maximus.nishaan.feature.missing.MissingPersonAdapter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Profile screen — user info, notification preferences, my reports, sign out.
 */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        // Display user info
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            binding.userName.text = user.displayName ?: "User"
            binding.userEmail.text = user.email ?: ""
            binding.createAccountBanner.visibility = View.GONE
        } else {
            binding.userName.text = getString(R.string.profile_guest_label)
            binding.userEmail.text = "Guest Mode"
            binding.createAccountBanner.visibility = View.VISIBLE
        }

        // My Reports — show user's submitted missing person reports
        val app = requireActivity().application as NishaanApplication
        val reportsAdapter = MissingPersonAdapter { /* No click action for own reports */ }
        binding.myReportsRecycler.adapter = reportsAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            app.appContainer.missingPersonRepository.observeMissingPersons()
                .catch { /* Handle error */ }
                .collect { persons ->
                    // Filter to current user's reports (or show all for guest)
                    val uid = user?.uid ?: "guest"
                    val myReports = persons.filter { it.submittedByUid == uid }.take(3)
                    reportsAdapter.submitList(myReports)
                }
        }

        binding.btnSeeAllReports.setOnClickListener {
            findNavController().navigate(R.id.missingHubFragment)
        }

        // Sign out
        binding.btnSignOut.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.profile_sign_out_confirm)
                .setPositiveButton(R.string.btn_yes) { _, _ ->
                    auth.signOut()
                    findNavController().navigate(R.id.action_profile_to_auth)
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
