package com.maximus.nishaan.feature.crisis

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.core.ui.UiState
import com.maximus.nishaan.core.util.toTimeAgo
import com.maximus.nishaan.databinding.FragmentCrisisDetailBinding
import com.maximus.nishaan.domain.model.Crisis
import com.maximus.nishaan.domain.model.CrisisType
import com.maximus.nishaan.domain.model.Severity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maximus.nishaan.databinding.DialogVerificationSimulationBinding

/**
 * Crisis Detail screen showing static header (severity, confidence, title)
 * and tabs (Overview, Timeline) using TabLayout and ViewPager2.
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

        // Set up toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Set up overflow menu click listener for simulating push verification
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_simulate_push) {
                val title = (viewModel.crisisState.value as? UiState.Success)?.data?.titleEn ?: "this crisis"
                val dialog = VerificationSimulationDialogFragment.newInstance(title)
                dialog.show(childFragmentManager, "verification_dialog")
                true
            } else {
                false
            }
        }

        // Share ViewModel across CrisisDetailFragment and children using childFragmentManager/requireParentFragment scope
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
        viewModel = ViewModelProvider(this, factory)[CrisisDetailViewModel::class.java]

        // Set up ViewPager2 with tabs
        val pagerAdapter = CrisisDetailPagerAdapter(this, crisisId)
        binding.viewPager.adapter = pagerAdapter

        com.google.android.material.tabs.TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Overview"
                1 -> "Timeline"
                else -> null
            }
        }.attach()

        observeState()
        viewModel.loadCrisis(crisisId)
    }

    private fun observeState() {
        viewModel.crisisState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> { /* Could show loading indicator */ }
                is UiState.Error -> { /* Could show error state */ }
                is UiState.Success -> renderHeader(state.data)
            }
        }
    }

    private fun renderHeader(crisis: Crisis) {
        binding.crisisTitle.text = crisis.titleEn
        binding.crisisTypeLabel.text = crisis.crisisType.toDisplayString()
        binding.confidenceText.text = getString(R.string.crisis_detail_confidence, crisis.confidence)
        binding.timeAgoText.text = getString(R.string.crisis_detail_detected_ago, crisis.createdAt.toTimeAgo())
        binding.crisisTypeIcon.setImageResource(crisis.crisisType.toIconRes())

        // Severity badge
        val severityColor = crisis.severity.toColorRes()
        val severityBgColor = crisis.severity.toBgColorRes()
        binding.severityBadge.text = crisis.severity.name
        binding.severityBadge.setTextColor(ContextCompat.getColor(requireContext(), severityColor))
        binding.severityBadge.setBackgroundColor(ContextCompat.getColor(requireContext(), severityBgColor))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CrisisDetailPagerAdapter(fragment: Fragment, private val crisisId: String) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle().apply {
            putString("crisisId", crisisId)
        }
        return when (position) {
            0 -> CrisisOverviewFragment().apply { arguments = bundle }
            1 -> CrisisTimelineFragment().apply { arguments = bundle }
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}

class VerificationSimulationDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogVerificationSimulationBinding? = null
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

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogVerificationSimulationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val crisisTitle = arguments?.getString("crisisTitle") ?: "this crisis"
        binding.dialogCrisisTitle.text = "SENTINEL has flagged a potential crisis: $crisisTitle. Help verify this situation."

        binding.btnDialogYes.setOnClickListener {
            viewModel.submitVote("YES")
            dismiss()
        }
        binding.btnDialogNo.setOnClickListener {
            viewModel.submitVote("NO")
            dismiss()
        }
        binding.btnDialogUnsure.setOnClickListener {
            viewModel.submitVote("UNSURE")
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(crisisTitle: String): VerificationSimulationDialogFragment {
            return VerificationSimulationDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("crisisTitle", crisisTitle)
                }
            }
        }
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

private fun CrisisType.toIconRes(): Int = when (this) {
    else -> R.drawable.ic_warning
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
