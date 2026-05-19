package com.maximus.nishaan.feature.crisis

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.core.ui.UiState
import com.maximus.nishaan.core.util.Constants
import com.maximus.nishaan.databinding.FragmentCrisisOverviewBinding
import com.maximus.nishaan.databinding.ItemAgentTraceBinding
import com.maximus.nishaan.domain.model.AgentTrace
import com.maximus.nishaan.domain.model.Crisis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrisisOverviewFragment : Fragment(R.layout.fragment_crisis_overview) {

    private var _binding: FragmentCrisisOverviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CrisisDetailViewModel by lazy {
        val app = requireActivity().application as NishaanApplication
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CrisisDetailViewModel(
                    app.appContainer.crisisRepository,
                    app.appContainer.agentTraceRepository,
                    app.appContainer.dataStore
                ) as T
            }
        }
        ViewModelProvider(requireParentFragment(), factory)[CrisisDetailViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCrisisOverviewBinding.bind(view)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnVerifyYes.setOnClickListener { viewModel.submitVote("YES") }
        binding.btnVerifyNo.setOnClickListener { viewModel.submitVote("NO") }
        binding.btnVerifyUnsure.setOnClickListener { viewModel.submitVote("UNSURE") }
    }

    private fun observeViewModel() {
        viewModel.crisisState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    bindCrisisData(state.data)
                }
                else -> { /* Parent handles loading/error states */ }
            }
        }

        viewModel.traces.observe(viewLifecycleOwner) { traces ->
            populateTracePreview(traces)
        }

        viewModel.verifications.observe(viewLifecycleOwner) { verifications ->
            val yes = verifications.count { it.response == "YES" }
            val no = verifications.count { it.response == "NO" }
            val unsure = verifications.count { it.response == "UNSURE" }
            binding.verifyTallyText.text = getString(R.string.verify_tally_format, yes, no, unsure)
        }

        viewModel.userVote.observe(viewLifecycleOwner) { vote ->
            updateVoteButtons(vote)
        }
    }

    private fun bindCrisisData(crisis: Crisis) {
        val prediction = crisis.spreadPrediction
        if (prediction != null) {
            binding.cardSpreadPrediction.visibility = View.VISIBLE
            binding.spreadConfidenceChip.text = "${prediction.confidence}% Conf"
            val formattedDir = prediction.direction.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
            binding.spreadRadiusText.text = "Radius: ${prediction.predictedRadiusKm} km ($formattedDir)"
            val isUrdu = java.util.Locale.getDefault().language == "ur"
            binding.spreadReasoningText.text = if (isUrdu && prediction.reasoningUr.isNotEmpty()) {
                prediction.reasoningUr
            } else {
                prediction.reasoningEn
            }
        } else {
            binding.cardSpreadPrediction.visibility = View.GONE
        }

        binding.descriptionText.text = crisis.descriptionEn.ifEmpty { crisis.analystReasoning }
        binding.missingCountText.text = getString(R.string.crisis_detail_missing_count, crisis.missingPersonsCount)

        // Setup Missing Persons card click
        binding.missingPersonsCard.setOnClickListener {
            val bundle = Bundle().apply {
                putString("crisisId", crisis.crisisId)
                putString("crisisTitle", crisis.titleEn)
            }
            findNavController().navigate(R.id.action_crisisDetail_to_missingHub, bundle)
        }

        // Setup Agent Trace button click
        binding.btnViewFullTrace.setOnClickListener {
            val bundle = Bundle().apply {
                putString("crisisId", crisis.crisisId)
                putString("crisisTitle", crisis.titleEn)
            }
            findNavController().navigate(R.id.action_crisisDetail_to_agentTrace, bundle)
        }

        // Populate agencies
        binding.agencyChipGroup.removeAllViews()
        crisis.assignedAgencies.forEach { agency ->
            val chip = Chip(requireContext()).apply {
                text = agency
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.color_surface_variant_dark)
                )
                setTextColor(ContextCompat.getColor(requireContext(), R.color.color_on_surface_dark))
                chipStrokeWidth = 0f
            }
            binding.agencyChipGroup.addView(chip)
        }
    }

    private fun updateVoteButtons(votedResponse: String?) {
        val ctx = requireContext()
        val greenColor = ContextCompat.getColor(ctx, R.color.color_severity_low)
        val greenBg = ContextCompat.getColor(ctx, R.color.color_severity_low_bg)
        val redColor = ContextCompat.getColor(ctx, R.color.color_severity_critical)
        val redBg = ContextCompat.getColor(ctx, R.color.color_severity_critical_bg)
        val orangeColor = ContextCompat.getColor(ctx, R.color.color_severity_high)
        val orangeBg = ContextCompat.getColor(ctx, R.color.color_severity_high_bg)
        val defaultStroke = ContextCompat.getColor(ctx, R.color.color_divider_dark)

        fun highlight(btn: MaterialButton, select: Boolean, activeColor: Int, activeBg: Int) {
            if (select) {
                btn.strokeColor = ColorStateList.valueOf(activeColor)
                btn.strokeWidth = 4
                btn.setBackgroundColor(activeBg)
                btn.setTextColor(activeColor)
            } else {
                btn.strokeColor = ColorStateList.valueOf(defaultStroke)
                btn.strokeWidth = 1
                btn.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent))
                btn.setTextColor(ContextCompat.getColor(ctx, R.color.color_on_surface_dark))
            }
        }

        highlight(binding.btnVerifyYes, votedResponse == "YES", greenColor, greenBg)
        highlight(binding.btnVerifyNo, votedResponse == "NO", redColor, redBg)
        highlight(binding.btnVerifyUnsure, votedResponse == "UNSURE", orangeColor, orangeBg)

        if (votedResponse != null) {
            binding.verifyUserResponseText.visibility = View.VISIBLE
            binding.verifyUserResponseText.text = getString(R.string.verify_user_response_format, votedResponse)
        } else {
            binding.verifyUserResponseText.visibility = View.GONE
        }
    }

    private fun populateTracePreview(traces: List<AgentTrace>) {
        if (_binding == null) return
        binding.tracePreviewContainer.removeAllViews()
        
        val previewTraces = traces.sortedByDescending { it.timestamp }.take(3)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        previewTraces.forEach { trace ->
            val traceBinding = ItemAgentTraceBinding.inflate(
                layoutInflater, binding.tracePreviewContainer, false
            )
            val ctx = requireContext()
            
            // Set timeline line visibility off for preview items
            traceBinding.timelineLine.visibility = View.GONE

            val agentColor = ContextCompat.getColor(ctx, when (trace.agentName) {
                Constants.AGENT_SENTINEL -> R.color.color_agent_sentinel
                Constants.AGENT_ANALYST -> R.color.color_agent_analyst
                Constants.AGENT_COMMANDER -> R.color.color_agent_commander
                Constants.AGENT_MATCHER -> R.color.color_agent_matcher
                else -> R.color.color_accent_info
            })

            val dotDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(agentColor)
            }
            traceBinding.timelineDot.background = dotDrawable

            traceBinding.agentNameBadge.text = trace.agentName
            traceBinding.agentNameBadge.setTextColor(agentColor)
            traceBinding.timestampText.text = timeFormat.format(Date(trace.timestamp))

            if (trace.confidence != null) {
                traceBinding.confidenceText.text = "${trace.confidence}%"
                traceBinding.confidenceText.visibility = View.VISIBLE
            } else {
                traceBinding.confidenceText.visibility = View.GONE
            }

            traceBinding.actionText.text = if (trace.action == "VERIFICATION_ADJUSTED") {
                "👥 " + trace.action.replace("_", " ")
            } else {
                trace.action.replace("_", " ")
            }
            traceBinding.reasoningText.text = trace.reasoningSummary

            binding.tracePreviewContainer.addView(traceBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
