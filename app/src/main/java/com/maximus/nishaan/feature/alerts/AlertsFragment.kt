package com.maximus.nishaan.feature.alerts

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentAlertsListBinding
import com.maximus.nishaan.domain.model.Crisis
import com.maximus.nishaan.domain.model.Severity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Alerts List screen — shows all crisis events as premium card-style items,
 * sorted by severity then recency, with a summary stat header.
 */
class AlertsFragment : Fragment(R.layout.fragment_alerts_list) {

    private var _binding: FragmentAlertsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AlertAdapter
    
    private var allCrises: List<Crisis> = emptyList()
    private var currentFilter: Severity? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAlertsListBinding.bind(view)

        adapter = AlertAdapter { crisis ->
            val bundle = Bundle().apply { putString("crisisId", crisis.crisisId) }
            findNavController().navigate(R.id.crisisDetailFragment, bundle)
        }
        binding.alertsRecycler.adapter = adapter

        val app = requireActivity().application as NishaanApplication
        viewLifecycleOwner.lifecycleScope.launch {
            app.appContainer.crisisRepository.observeActiveCrises()
                .catch { /* Handle error */ }
                .collect { crises ->
                    allCrises = crises.sortedWith(
                        compareBy<Crisis> { it.severity.ordinal }
                            .thenByDescending { it.createdAt }
                    )
                    
                    // Update stat chips
                    updateStatChips(allCrises)

                    applyFilter()
                }
        }

        binding.cardStatTotal.setOnClickListener {
            currentFilter = null
            applyFilter()
        }
        binding.cardStatCritical.setOnClickListener {
            currentFilter = Severity.CRITICAL
            applyFilter()
        }
        binding.cardStatHigh.setOnClickListener {
            currentFilter = Severity.HIGH
            applyFilter()
        }

        binding.btnMarkAllRead.setOnClickListener {
            // TODO: Mark all as read in local DataStore/Room
        }
    }

    /** Populate the header stat chips with counts by severity. */
    private fun updateStatChips(crises: List<Crisis>) {
        val total = crises.size
        val critical = crises.count { it.severity == Severity.CRITICAL }
        val high = crises.count { it.severity == Severity.HIGH }

        binding.txtStatTotal.text = total.toString()
        binding.txtStatCritical.text = critical.toString()
        binding.txtStatHigh.text = high.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyFilter() {
        val filtered = if (currentFilter == null) {
            allCrises
        } else {
            allCrises.filter { it.severity == currentFilter }
        }
        adapter.submitList(filtered)
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.alertsRecycler.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        binding.statChipsRow.visibility = if (allCrises.isEmpty()) View.GONE else View.VISIBLE
        
        updateChipSelection()
    }

    private fun updateChipSelection() {
        binding.cardStatTotal.setBackgroundResource(
            if (currentFilter == null) R.drawable.bg_stat_chip_selected else R.drawable.bg_stat_chip
        )
        binding.cardStatCritical.setBackgroundResource(
            if (currentFilter == Severity.CRITICAL) R.drawable.bg_stat_chip_selected else R.drawable.bg_stat_chip
        )
        binding.cardStatHigh.setBackgroundResource(
            if (currentFilter == Severity.HIGH) R.drawable.bg_stat_chip_selected else R.drawable.bg_stat_chip
        )
    }
}
