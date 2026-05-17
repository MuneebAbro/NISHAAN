package com.maximus.nishaan.feature.missing

import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentMissingHubBinding
import com.maximus.nishaan.domain.model.MissingPerson
import com.maximus.nishaan.domain.model.MissingPersonStatus
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Missing Persons Hub — lists all missing persons with search and tab filters.
 * Tabs: All, By Crisis (linked), Unlinked.
 */
class MissingHubFragment : Fragment(R.layout.fragment_missing_hub) {

    private var _binding: FragmentMissingHubBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MissingPersonAdapter
    private var allPersons: List<MissingPerson> = emptyList()
    private var searchQuery = ""
    private var activeTab = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMissingHubBinding.bind(view)

        val crisisId = arguments?.getString("crisisId")

        adapter = MissingPersonAdapter { person ->
            val bundle = Bundle().apply { putString("reportId", person.reportId) }
            findNavController().navigate(R.id.action_missingHub_to_missingDetail, bundle)
        }
        binding.missingRecycler.adapter = adapter

        // Search filter
        binding.searchInput.addTextChangedListener { text ->
            searchQuery = text?.toString().orEmpty()
            applyFilters()
        }

        // Tab filter
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { activeTab = tab.position; applyFilters() }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Observe missing persons from Firestore
        val app = requireActivity().application as NishaanApplication
        viewLifecycleOwner.lifecycleScope.launch {
            app.appContainer.missingPersonRepository.observeMissingPersons(crisisId)
                .catch { /* Handle error */ }
                .collect { persons ->
                    allPersons = persons
                    applyFilters()
                }
        }
    }

    private fun applyFilters() {
        var filtered = allPersons

        // Tab filter
        when (activeTab) {
            1 -> filtered = filtered.filter { it.linkedCrisisId != null }
            2 -> filtered = filtered.filter { it.linkedCrisisId == null }
        }

        // Search filter
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.personName.contains(searchQuery, ignoreCase = true)
            }
        }

        adapter.submitList(filtered)
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.missingRecycler.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
