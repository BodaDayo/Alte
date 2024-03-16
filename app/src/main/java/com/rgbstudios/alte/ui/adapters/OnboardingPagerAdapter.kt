package com.rgbstudios.alte.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.alte.R
import com.rgbstudios.alte.databinding.ItemOnboardingPagerBinding

class OnboardingPagerAdapter(
    private val context: Context,
    private var list: List<Int>,
) : RecyclerView.Adapter<OnboardingPagerAdapter.PagerViewHolder>() {

    inner class PagerViewHolder(val binding: ItemOnboardingPagerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val binding =
            ItemOnboardingPagerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PagerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        val image = list[position]
        holder.binding.apply {

            pagerImage.setImageResource(image)

            when (position) {
                0 -> {
                    pagerTV.text = context.getString(R.string.chat)
                }
                1 -> {
                    pagerTV.text = context.getString(R.string.voice_call)
                }
                else -> {
                    pagerTV.text = context.getString(R.string.video_call)
                }
            }

        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}