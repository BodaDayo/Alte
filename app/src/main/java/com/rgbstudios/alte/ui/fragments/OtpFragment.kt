package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.rgbstudios.alte.R
import com.rgbstudios.alte.databinding.FragmentOtpBinding

class OtpFragment : Fragment() {

    private lateinit var binding: FragmentOtpBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            verifyOTPBtn.setOnClickListener {
                // Navigate to
                findNavController().navigate(R.id.action_otpFragment_to_homeFragment)
            }
        }
    }
}