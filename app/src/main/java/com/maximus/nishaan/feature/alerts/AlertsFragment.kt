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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Alerts List screen — shows all crisis events as notification-style list,
 * sorted by severity then recency.
 */
class AlertsFragment : Fragment(R.layout.fragment_alerts_list) {

    private var _binding: FragmentAlertsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AlertAdapter

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
                    val sorted = crises.sortedWith(
                        compareBy<Crisis> { it.severity.ordinal }
                            .thenByDescending { it.createdAt }
                    )
                    adapter.submitList(sorted)
                    binding.emptyState.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
                    binding.alertsRecycler.visibility = if (sorted.isEmpty()) View.GONE else View.VISIBLE
                }
        }

        binding.btnMarkAllRead.setOnClickListener {
            // TODO: Mark all as read in local DataStore/Room
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
