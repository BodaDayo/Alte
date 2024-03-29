package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.firebase.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentOnboardingBinding
import com.rgbstudios.alte.ui.adapters.OnboardingPagerAdapter
import com.rgbstudios.alte.utils.AvatarManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class OnboardingFragment : Fragment() {

    private lateinit var binding: FragmentOnboardingBinding
    private val firebase = FirebaseAccess()
    private val avatarManager = AvatarManager()
    private var currentPosition = 0

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

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
            val adapter =
                OnboardingPagerAdapter(requireContext(), avatarManager.getOnboardingImages())
            viewPager.adapter = adapter

            skipTV.setOnClickListener {
                findNavController().navigate(R.id.action_onboardingFragment_to_onboardingFinalFragment)
            }

            doneBtnOF.setOnClickListener {
                findNavController().navigate(R.id.action_onboardingFragment_to_onboardingFinalFragment)
                alteViewModel.updateFirstLaunchStatus(false)
            }

            previousBtnOP.setOnClickListener {
                viewPager.currentItem = currentPosition - 1
                viewPager.currentItem = currentPosition++
            }

            nextBtnOP.setOnClickListener {
                viewPager.currentItem = currentPosition++
            }

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    currentPosition = position
                    updatePageIndicator(position)
                    updateButtons(position)
                }
            })
        }
    }

    private fun updateButtons(position: Int) {
        binding.apply {
            when (position) {
                0 -> {
                    previousBtnOP.visibility = View.GONE
                    nextBtnOP.visibility = View.VISIBLE
                    doneBtnOF.visibility = View.GONE
                }
                1 -> {
                    previousBtnOP.visibility = View.VISIBLE
                    nextBtnOP.visibility = View.VISIBLE
                    doneBtnOF.visibility = View.GONE
                }
                else -> {
                    previousBtnOP.visibility = View.GONE
                    nextBtnOP.visibility = View.GONE
                    doneBtnOF.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updatePageIndicator(position: Int) {
        binding.apply {
            val indicatorTintActive = resources.getColor(R.color.md_theme_light_primary, null)
            val indicatorTintInactive = resources.getColor(R.color.my_pager_grey, null)

            circle1.setColorFilter(if (position == 0) indicatorTintActive else indicatorTintInactive)
            circle2.setColorFilter(if (position == 1) indicatorTintActive else indicatorTintInactive)
            circle3.setColorFilter(if (position == 2) indicatorTintActive else indicatorTintInactive)
        }
    }


}