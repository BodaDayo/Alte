package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.rgbstudios.alte.R
import com.rgbstudios.alte.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private lateinit var binding: FragmentOnboardingBinding
    private lateinit var customBar: LottieAnimationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            customBar = lottieAnimationView
            showAnim.setOnClickListener {
                texty.visibility = View.GONE
                showAnim.visibility = View.GONE
                showProgress()
                closer.visibility = View.VISIBLE
            }

            closer.setOnClickListener {
                texty.visibility = View.VISIBLE
                showAnim.visibility = View.VISIBLE

                customBar.visibility = View.GONE
                closer.visibility = View.GONE
            }
        }
    }

    private fun showProgress() {
        // Set the animation file
        customBar.setAnimation("progress_animation.json")

        // Start the animation
        customBar.playAnimation()

        customBar.visibility = View.VISIBLE
    }

}