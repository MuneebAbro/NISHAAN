package com.maximus.nishaan.feature.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maximus.nishaan.R
import com.maximus.nishaan.databinding.ItemOnboardingSlideBinding

/**
 * Pager adapter for the 3 onboarding slides.
 */
class OnboardingPagerAdapter : RecyclerView.Adapter<OnboardingPagerAdapter.SlideViewHolder>() {

    data class Slide(val titleRes: Int, val bodyRes: Int, val iconRes: Int)

    private val slides = listOf(
        Slide(R.string.onboarding_slide_1_title, R.string.onboarding_slide_1_body, R.drawable.ic_launcher_foreground),
        Slide(R.string.onboarding_slide_2_title, R.string.onboarding_slide_2_body, R.drawable.ic_launcher_foreground),
        Slide(R.string.onboarding_slide_3_title, R.string.onboarding_slide_3_body, R.drawable.ic_launcher_foreground)
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val binding = ItemOnboardingSlideBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SlideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size

    class SlideViewHolder(
        private val binding: ItemOnboardingSlideBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(slide: Slide) {
            binding.slideTitle.setText(slide.titleRes)
            binding.slideBody.setText(slide.bodyRes)
            binding.slideIcon.setImageResource(slide.iconRes)
        }
    }
}
