package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.firebase.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class SplashFragment : Fragment() {

    private var delayedNavigationHandler: Handler? = null

    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(requireActivity().application as AlteApplication, AlteRepository(firebase))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Call the function to handle delayed navigation
        handleDelayedNavigation()
    }

    override fun onResume() {
        super.onResume()

        // Call the function to handle delayed navigation when the fragment resumes
        handleDelayedNavigation()
    }

    private fun handleDelayedNavigation() {
        // Remove any previously posted callbacks to avoid multiple executions
        delayedNavigationHandler?.removeCallbacksAndMessages(null)

        // Create a new Handler for delayed navigation
        delayedNavigationHandler = Handler(Looper.myLooper()!!)

        delayedNavigationHandler?.postDelayed({

            // Check if it's the first launch
            val isFirstLaunch = alteViewModel.isFirstLaunch.value ?: true

            if (isFirstLaunch) {

                // If it's the first launch, navigate to OnboardingFragmentOne
                findNavController().navigate(R.id.action_splashFragment_to_onboardingFragment)

            } else {
                val auth = firebase.auth

                // It's not first launch
                if (auth.currentUser != null) {

                    val isUsernameSet = alteViewModel.isUsernameSet.value ?: false

                    if (isUsernameSet) {

                        // If username is set, navigate to MessagesFragment
                        findNavController().navigate(R.id.action_splashFragment_to_messagesFragment)

                    } else {
                        // Navigate to completeRegistrationFragment
                        findNavController().navigate(R.id.action_splashFragment_to_completeRegistrationFragment)
                    }
                } else {

                    findNavController().navigate(R.id.action_splashFragment_to_onboardingFinalFragment)
                }
            }
        }, 2000)
    }
}