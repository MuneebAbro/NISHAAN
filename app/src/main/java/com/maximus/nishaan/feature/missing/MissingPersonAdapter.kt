package com.maximus.nishaan.feature.missing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maximus.nishaan.R
import com.bumptech.glide.Glide
import com.maximus.nishaan.core.util.toTimeAgo
import com.maximus.nishaan.databinding.ItemMissingPersonBinding
import com.maximus.nishaan.domain.model.MissingPerson
import com.maximus.nishaan.domain.model.MissingPersonStatus

/** Adapter for missing person cards in the hub. */
class MissingPersonAdapter(
    private val onClick: (MissingPerson) -> Unit
) : ListAdapter<MissingPerson, MissingPersonAdapter.PersonViewHolder>(PersonDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val binding = ItemMissingPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PersonViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PersonViewHolder(
        private val binding: ItemMissingPersonBinding,
        private val onClick: (MissingPerson) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(person: MissingPerson) {
            val ctx = binding.root.context
            binding.personName.text = person.personName
            binding.personDetails.text = "${person.personAge} yrs • ${person.personGender}"
            binding.lastSeenText.text = if (person.lastSeenAddress.isNotEmpty()) {
                person.lastSeenAddress
            } else {
                person.submittedAt.toTimeAgo()
            }

            // Status chip + severity stripe color + status dot
            when (person.status) {
                MissingPersonStatus.SEARCHING -> {
                    binding.statusChip.text = ctx.getString(R.string.missing_status_searching)
                    binding.statusChip.setTextColor(ContextCompat.getColor(ctx, R.color.color_accent_info))
                    binding.statusChip.setBackgroundColor(ContextCompat.getColor(ctx, R.color.color_severity_monitoring_bg))
                    binding.severityStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.color_accent_info))
                    binding.statusDot.visibility = android.view.View.VISIBLE
                    binding.statusDot.setBackgroundResource(R.drawable.circle_pulse_green)
                }
                MissingPersonStatus.LINKED -> {
                    binding.statusChip.text = "Linked"
                    binding.statusChip.setTextColor(ContextCompat.getColor(ctx, R.color.color_secondary))
                    binding.statusChip.setBackgroundColor(ContextCompat.getColor(ctx, R.color.color_severity_medium_bg))
                    binding.severityStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.color_secondary))
                    binding.statusDot.visibility = android.view.View.VISIBLE
                }
                MissingPersonStatus.FOUND -> {
                    binding.statusChip.text = ctx.getString(R.string.missing_status_found)
                    binding.statusChip.setTextColor(ContextCompat.getColor(ctx, R.color.color_accent_low))
                    binding.statusChip.setBackgroundColor(ContextCompat.getColor(ctx, R.color.color_severity_low_bg))
                    binding.severityStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.color_accent_low))
                    binding.statusDot.visibility = android.view.View.GONE
                }
                MissingPersonStatus.POTENTIAL_DUPLICATE -> {
                    binding.statusChip.text = "Duplicate?"
                    binding.statusChip.setTextColor(ContextCompat.getColor(ctx, R.color.color_secondary))
                    binding.statusChip.setBackgroundColor(ContextCompat.getColor(ctx, R.color.color_severity_medium_bg))
                    binding.severityStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.color_secondary))
                    binding.statusDot.visibility = android.view.View.GONE
                }
            }

            // Load photo
            if (!person.photoUrl.isNullOrEmpty()) {
                Glide.with(ctx)
                    .load(person.photoUrl)
                    .centerCrop()
                    .into(binding.photoThumbnail)
            } else {
                binding.photoThumbnail.setBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.color_surface_variant_dark)
                )
            }

            binding.root.setOnClickListener { onClick(person) }
        }
    }

    private object PersonDiff : DiffUtil.ItemCallback<MissingPerson>() {
        override fun areItemsTheSame(old: MissingPerson, new: MissingPerson) = old.reportId == new.reportId
        override fun areContentsTheSame(old: MissingPerson, new: MissingPerson) = old == new
    }
}
