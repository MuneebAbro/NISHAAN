package com.maximus.nishaan.feature.alerts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maximus.nishaan.R
import com.maximus.nishaan.core.util.toTimeAgo
import com.maximus.nishaan.databinding.ItemAlertBinding
import com.maximus.nishaan.domain.model.Crisis
import com.maximus.nishaan.domain.model.Severity

/** Adapter for the Alerts list — shows crises as notification-style items. */
class AlertAdapter(
    private val onClick: (Crisis) -> Unit
) : ListAdapter<Crisis, AlertAdapter.AlertViewHolder>(AlertDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlertViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AlertViewHolder(
        private val binding: ItemAlertBinding,
        private val onClick: (Crisis) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(crisis: Crisis) {
            val ctx = binding.root.context
            val severityColor = ContextCompat.getColor(ctx, crisis.severity.toColorRes())
            val severityBg = ContextCompat.getColor(ctx, crisis.severity.toBgColorRes())

            binding.severityStripe.setBackgroundColor(severityColor)
            binding.severityBadge.text = crisis.severity.name
            binding.severityBadge.setTextColor(severityColor)
            binding.severityBadge.setBackgroundColor(severityBg)
            binding.crisisTypeText.text = crisis.crisisType.name.replace("_", " ")
            binding.timeAgoText.text = crisis.createdAt.toTimeAgo()
            binding.alertTitle.text = crisis.titleEn
            binding.alertDescription.text = crisis.descriptionEn

            binding.root.setOnClickListener { onClick(crisis) }
        }
    }

    private object AlertDiff : DiffUtil.ItemCallback<Crisis>() {
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

private fun Severity.toBgColorRes(): Int = when (this) {
    Severity.CRITICAL -> R.color.color_severity_critical_bg
    Severity.HIGH -> R.color.color_severity_high_bg
    Severity.MEDIUM -> R.color.color_severity_medium_bg
    Severity.LOW -> R.color.color_severity_low_bg
    Severity.MONITORING -> R.color.color_severity_monitoring_bg
}
