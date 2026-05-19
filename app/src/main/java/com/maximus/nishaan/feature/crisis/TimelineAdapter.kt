package com.maximus.nishaan.feature.crisis

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.ItemTimelineEventBinding
import com.maximus.nishaan.domain.model.TimelineEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Adapter for vertical crisis timeline events inside CrisisTimelineFragment. */
class TimelineAdapter : ListAdapter<TimelineEvent, TimelineAdapter.TimelineViewHolder>(TimelineDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val binding = ItemTimelineEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TimelineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        holder.bind(getItem(position), position == itemCount - 1)
    }

    class TimelineViewHolder(
        private val binding: ItemTimelineEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun bind(event: TimelineEvent, isLast: Boolean) {
            val ctx = binding.root.context
            
            val colorRes = when (event.type) {
                "SENTINEL" -> R.color.color_agent_sentinel
                "ANALYST" -> R.color.color_agent_analyst
                "COMMANDER" -> R.color.color_agent_commander
                "MATCHER" -> R.color.color_agent_matcher
                else -> R.color.color_primary // SYSTEM
            }
            val color = ContextCompat.getColor(ctx, colorRes)

            val dotDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            binding.timelineDot.background = dotDrawable

            binding.timelineLine.visibility = if (isLast) View.INVISIBLE else View.VISIBLE

            binding.eventBadge.text = event.type
            binding.eventBadge.setTextColor(color)

            binding.timestampText.text = timeFormat.format(Date(event.timestamp))
            binding.eventTitle.text = event.title
            binding.eventDescription.text = event.description
        }
    }

    private object TimelineDiff : DiffUtil.ItemCallback<TimelineEvent>() {
        override fun areItemsTheSame(old: TimelineEvent, new: TimelineEvent) = old.eventId == new.eventId
        override fun areContentsTheSame(old: TimelineEvent, new: TimelineEvent) = old == new
    }
}
