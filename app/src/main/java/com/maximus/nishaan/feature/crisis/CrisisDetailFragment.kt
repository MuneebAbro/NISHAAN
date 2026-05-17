package com.maximus.nishaan.feature.crisis

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.core.ui.UiState
import com.maximus.nishaan.core.util.Constants
import com.maximus.nishaan.core.util.toTimeAgo
import com.maximus.nishaan.databinding.FragmentCrisisDetailBinding
import com.maximus.nishaan.domain.model.AgentTrace
import com.maximus.nishaan.domain.model.Crisis
import com.maximus.nishaan.domain.model.CrisisType
import com.maximus.nishaan.domain.model.Severity

/**
 * Crisis Detail screen showing severity, confidence, agencies,
 * missing persons count, and agent trace preview.
 */
class CrisisDetailFragment : Fragment(R.layout.fragment_crisis_detail) {

    private var _binding: FragmentCrisisDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CrisisDetailViewModel
    private var crisisId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCrisisDetailBinding.bind(view)

        crisisId = arguments?.getString("crisisId") ?: return

        val app = requireActivity().application as NishaanApplication
        viewModel = CrisisDetailViewModel(
            app.appContainer.crisisRepository,
            app.appContainer.agentTraceRepository
        )

        observeState()
        viewModel.loadCrisis(crisisId)

        binding.btnViewFullTrace.setOnClickListener {
            val title = (viewModel.crisisState.value as? UiState.Success)?.data?.titleEn ?: ""
            val bundle = Bundle().apply {
                putString("crisisId", crisisId)
                putString("crisisTitle", title)
            }
            findNavController().navigate(R.id.action_crisisDetail_to_agentTrace, bundle)
        }

        binding.missingPersonsCard.setOnClickListener {
            val bundle = Bundle().apply { putString("crisisId", crisisId) }
            findNavController().navigate(R.id.action_crisisDetail_to_missingHub, bundle)
        }
    }

    private fun observeState() {
        viewModel.crisisState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> { /* Could show loading indicator */ }
                is UiState.Error -> { /* Could show error state */ }
                is UiState.Success -> renderCrisis(state.data)
            }
        }

        viewModel.traces.observe(viewLifecycleOwner) { traces ->
            renderTracePreview(traces.take(3))
        }
    }

    private fun renderCrisis(crisis: Crisis) {
        binding.crisisTitle.text = crisis.titleEn
        binding.crisisTypeLabel.text = crisis.crisisType.toDisplayString()
        binding.confidenceText.text = getString(R.string.crisis_detail_confidence, crisis.confidence)
        binding.timeAgoText.text = getString(R.string.crisis_detail_detected_ago, crisis.createdAt.toTimeAgo())
        binding.descriptionText.text = crisis.descriptionEn.ifEmpty { crisis.analystReasoning }
        binding.missingCountText.text = getString(R.string.crisis_detail_missing_count, crisis.missingPersonsCount)

        // Severity badge
        val severityColor = crisis.severity.toColorRes()
        val severityBgColor = crisis.severity.toBgColorRes()
        binding.severityBadge.text = crisis.severity.name
        binding.severityBadge.setTextColor(ContextCompat.getColor(requireContext(), severityColor))
        binding.severityBadge.setBackgroundColor(ContextCompat.getColor(requireContext(), severityBgColor))

        // Agency chips
        binding.agencyChipGroup.removeAllViews()
        crisis.assignedAgencies.forEach { agency ->
            val chip = Chip(requireContext()).apply {
                text = agency.replace("_", " ").replaceFirstChar { it.uppercase() }
                isClickable = false
            }
            binding.agencyChipGroup.addView(chip)
        }
    }

    private fun renderTracePreview(traces: List<AgentTrace>) {
        binding.tracePreviewContainer.removeAllViews()
        traces.forEach { trace ->
            val row = TextView(requireContext()).apply {
                val agentColor = getAgentColor(trace.agentName)
                text = "● ${trace.agentName} — ${trace.action}"
                setTextColor(ContextCompat.getColor(requireContext(), agentColor))
                textSize = 13f
                setPadding(0, 8, 0, 8)
                typeface = android.graphics.Typeface.MONOSPACE
            }
            binding.tracePreviewContainer.addView(row)
        }
    }

    private fun getAgentColor(agentName: String): Int = when (agentName) {
        Constants.AGENT_SENTINEL -> R.color.color_agent_sentinel
        Constants.AGENT_ANALYST -> R.color.color_agent_analyst
        Constants.AGENT_COMMANDER -> R.color.color_agent_commander
        Constants.AGENT_MATCHER -> R.color.color_agent_matcher
        else -> R.color.color_accent_info
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun CrisisType.toDisplayString(): String = when (this) {
    CrisisType.FLOOD -> "Flood"
    CrisisType.EARTHQUAKE -> "Earthquake"
    CrisisType.HEATWAVE -> "Heatwave"
    CrisisType.CIVIL_UNREST -> "Civil Unrest"
    CrisisType.TRAFFIC_ACCIDENT -> "Traffic Accident"
    CrisisType.INFRASTRUCTURE_FAILURE -> "Infrastructure Failure"
    CrisisType.UNKNOWN -> "Unknown"
}

private fun Severity.toColorRes(): Int = when (this) {
    Severity.CRITICAL -> R.color.color_severity_critical
    Severity.HIGH -> R.color.color_severity_high
    Severity.MEDIUM -> R.color.color_severity_medium
    Severity.LOW -> R.color.color_severity_low
    Severity.MONITORING -> R.color.color_severity_monitoring
}

private fun Severity.toBgColorRes(): Int = when (this) {
    Severity.CRITICAL -> R.color.color_severity_critical_bg
    Severity.HIGH -> R.color.color_severity_high_bg
    Severity.MEDIUM -> R.color.color_severity_medium_bg
    Severity.LOW -> R.color.color_severity_low_bg
    Severity.MONITORING -> R.color.color_severity_monitoring_bg
}
