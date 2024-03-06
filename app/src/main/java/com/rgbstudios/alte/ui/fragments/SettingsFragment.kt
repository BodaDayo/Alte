package com.rgbstudios.alte.ui.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.activityViewModels
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentSettingsBinding
import com.rgbstudios.alte.utils.SharedPreferencesManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory
import java.util.concurrent.Executor

class SettingsFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var fragmentContext: Context
    private val toastManager = ToastManager()
    private val thisFragment = this
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var isSlidingPaneLayoutOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        fragmentContext = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            // Lock the SlidingPaneLayout
            slider.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED

            alteViewModel.closeSlider.observe(viewLifecycleOwner) { toClose ->
                if (toClose) {
                    slider.closePane()
                    isSlidingPaneLayoutOpen = false
                }
            }

            accountsLayout.setOnClickListener {
                openDetailsPane(getString(R.string.account))
            }

            notificationsLayout.setOnClickListener {
                openDetailsPane(getString(R.string.notifications))
            }

            privacyLayout.setOnClickListener {
                openDetailsPane(getString(R.string.privacy))
            }

            popBack.setOnClickListener {
                popBackStackManager()
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                popBackStackManager()
            }
        }

    }

    private fun openDetailsPane(item: String) {
        alteViewModel.setSettingsItem(item)
        alteViewModel.toggleSlider(false)

        // Replace the details pane with the details fragment
        val detailsFragment = SettingsDetailsFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.settingsDetails, detailsFragment)
            .commit()

        // Open the details pane
        binding.slider.openPane()
        isSlidingPaneLayoutOpen = true
    }

    private fun popBackStackManager() {
        if (isSlidingPaneLayoutOpen) {
            alteViewModel.toggleSlider(true)
        } else {
            // If no changes, simply pop back stack
            activity?.supportFragmentManager?.popBackStack()
        }
    }

}