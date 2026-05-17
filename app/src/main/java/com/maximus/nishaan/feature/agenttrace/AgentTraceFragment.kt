package com.maximus.nishaan.feature.agenttrace

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentAgentTraceBinding

/**
 * Agent Trace View — real-time timeline of AI agent decisions.
 * Key feature for hackathon judges. Read-only, auto-updates via Firestore listener.
 */
class AgentTraceFragment : Fragment(R.layout.fragment_agent_trace) {

    private var _binding: FragmentAgentTraceBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AgentTraceViewModel
    private val adapter = AgentTraceAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAgentTraceBinding.bind(view)

        val crisisId = arguments?.getString("crisisId") ?: return
        val crisisTitle = arguments?.getString("crisisTitle") ?: ""

        binding.traceTitle.text = getString(R.string.agent_trace_title, crisisTitle)
        binding.traceRecycler.adapter = adapter
        binding.traceRecycler.layoutManager = LinearLayoutManager(requireContext())

        val app = requireActivity().application as NishaanApplication
        viewModel = AgentTraceViewModel(app.appContainer.agentTraceRepository)

        viewModel.traces.observe(viewLifecycleOwner) { traces ->
            adapter.submitList(traces)
            // Auto-scroll to top when new entry arrives
            if (traces.isNotEmpty()) {
                binding.traceRecycler.smoothScrollToPosition(0)
            }
        }

        viewModel.hasNewActivity.observe(viewLifecycleOwner) { hasNew ->
            binding.chipNewActivity.visibility = if (hasNew) View.VISIBLE else View.GONE
        }

        binding.chipNewActivity.setOnClickListener {
            binding.traceRecycler.smoothScrollToPosition(0)
            viewModel.clearNewActivity()
        }

        viewModel.observeCrisis(crisisId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
