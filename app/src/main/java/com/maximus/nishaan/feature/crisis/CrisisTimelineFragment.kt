package com.maximus.nishaan.feature.crisis

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.maximus.nishaan.NishaanApplication
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.FragmentCrisisTimelineBinding

/** Fragment showing real-time list of events for the crisis. */
class CrisisTimelineFragment : Fragment(R.layout.fragment_crisis_timeline) {

    private var _binding: FragmentCrisisTimelineBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TimelineAdapter

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
        _binding = FragmentCrisisTimelineBinding.bind(view)

        adapter = TimelineAdapter()
        binding.timelineRecyclerView.adapter = adapter
        binding.timelineRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.timelineEvents.observe(viewLifecycleOwner) { events ->
            if (events.isNullOrEmpty()) {
                binding.timelineEmptyText.visibility = View.VISIBLE
                binding.timelineRecyclerView.visibility = View.GONE
            } else {
                binding.timelineEmptyText.visibility = View.GONE
                binding.timelineRecyclerView.visibility = View.VISIBLE
                adapter.submitList(events)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
