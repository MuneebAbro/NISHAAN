package com.maximus.nishaan.feature.agenttrace

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maximus.nishaan.R
import com.maximus.nishaan.core.util.Constants
import com.maximus.nishaan.databinding.ItemAgentTraceBinding
import com.maximus.nishaan.domain.model.AgentTrace
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** RecyclerView adapter for agent trace timeline entries. */
class AgentTraceAdapter : ListAdapter<AgentTrace, AgentTraceAdapter.TraceViewHolder>(TraceDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TraceViewHolder {
        val binding = ItemAgentTraceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TraceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TraceViewHolder, position: Int) {
        holder.bind(getItem(position), position == itemCount - 1)
    }

    class TraceViewHolder(
        private val binding: ItemAgentTraceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun bind(trace: AgentTrace, isLast: Boolean) {
            val ctx = binding.root.context
            val agentColor = ContextCompat.getColor(ctx, getAgentColorRes(trace.agentName))

            // Timeline dot (circular, colored by agent)
            val dotDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(agentColor)
            }
            binding.timelineDot.background = dotDrawable

            // Hide line on last item
            binding.timelineLine.visibility = if (isLast) android.view.View.INVISIBLE else android.view.View.VISIBLE

            // Agent name badge
            binding.agentNameBadge.text = trace.agentName
            binding.agentNameBadge.setTextColor(agentColor)

            // Timestamp
            binding.timestampText.text = timeFormat.format(Date(trace.timestamp))

            // Confidence
            if (trace.confidence != null) {
                binding.confidenceText.text = "${trace.confidence}%"
                binding.confidenceText.visibility = android.view.View.VISIBLE
            } else {
                binding.confidenceText.visibility = android.view.View.GONE
            }

            // Action + reasoning
            binding.actionText.text = when (trace.action) {
                "VERIFICATION_ADJUSTED" -> "👥 " + trace.action.replace("_", " ")
                "WITNESS_CORROBORATED" -> "🔍 " + trace.action.replace("_", " ")
                else -> trace.action.replace("_", " ")
            }
            binding.reasoningText.text = trace.reasoningSummary
        }

        private fun getAgentColorRes(agentName: String): Int = when (agentName) {
            Constants.AGENT_SENTINEL -> R.color.color_agent_sentinel
            Constants.AGENT_ANALYST -> R.color.color_agent_analyst
            Constants.AGENT_COMMANDER -> R.color.color_agent_commander
            Constants.AGENT_MATCHER -> R.color.color_agent_matcher
            else -> R.color.color_accent_info
        }
    }

    private object TraceDiff : DiffUtil.ItemCallback<AgentTrace>() {
        override fun areItemsTheSame(old: AgentTrace, new: AgentTrace) = old.traceId == new.traceId
        override fun areContentsTheSame(old: AgentTrace, new: AgentTrace) = old == new
    }
}
