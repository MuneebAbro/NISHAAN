package com.maximus.nishaan.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maximus.nishaan.R
import com.maximus.nishaan.core.util.toTimeAgo
import com.maximus.nishaan.databinding.ItemCrisisCardBinding
import com.maximus.nishaan.domain.model.Crisis
import com.maximus.nishaan.domain.model.Severity

/** Adapter for horizontal crisis alert banner on Home Dashboard. */
class CrisisCardAdapter(
    private val onCrisisClick: (Crisis) -> Unit
) : ListAdapter<Crisis, CrisisCardAdapter.CrisisViewHolder>(CrisisDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrisisViewHolder {
        val binding = ItemCrisisCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CrisisViewHolder(binding, onCrisisClick)
    }

    override fun onBindViewHolder(holder: CrisisViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CrisisViewHolder(
        private val binding: ItemCrisisCardBinding,
        private val onClick: (Crisis) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(crisis: Crisis) {
            val ctx = binding.root.context
            val severityColor = ContextCompat.getColor(ctx, crisis.severity.toColorRes())

            binding.severityStripe.setBackgroundColor(severityColor)
            
            val badgeBg = when (crisis.severity.name) {
                "CRITICAL" -> R.drawable.bg_badge_red
                "HIGH"     -> R.drawable.bg_badge_amber
                else       -> R.drawable.bg_badge_blue
            }
            binding.severityBadge.setBackgroundResource(badgeBg)
            binding.severityBadge.text = crisis.severity.name
            binding.crisisTypeText.text = crisis.crisisType.name.replace("_", " ")
            binding.crisisTitle.text = crisis.titleEn
            binding.timeAgoText.text = crisis.createdAt.toTimeAgo()
            
            if (crisis.assignedAgencies.isNotEmpty()) {
                binding.assigneeText.text = crisis.assignedAgencies.joinToString(", ")
            } else {
                binding.assigneeText.text = "Unassigned"
            }

            binding.root.setOnClickListener { onClick(crisis) }
        }
    }

    private object CrisisDiff : DiffUtil.ItemCallback<Crisis>() {
        override fun areItemsTheSame(old: Crisis, new: Crisis) = old.crisisId == new.crisisId
        override fun areContentsTheSame(old: Crisis, new: Crisis) = old == new
    }
}

private fun Severity.toColorRes(): Int = when (this) {
    Severity.CRITICAL -> R.color.color_severity_critical
    Severity.HIGH -> R.color.color_severity_high
    Severity.MEDIUM -> R.color.color_severity_medium
    Severity.LOW -> R.color.color_severity_low
    Severity.MONITORING -> R.color.color_severity_monitoring
}

