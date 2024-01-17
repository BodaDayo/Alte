package com.rgbstudios.alte.ui.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class SplashFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var delayedNavigationHandler: Handler? = null
    private val toastManager = ToastManager()

    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by viewModels {
        AlteViewModelFactory(requireActivity().application, AlteRepository(firebase))
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
        val firebase = FirebaseAccess()
        auth = firebase.auth

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

        delayedNavigationHandler?.postDelayed(Runnable {

            // Check if it's the first launch
            val isFirstLaunch = alteViewModel.isFirstLaunch.value ?: true

            if (isFirstLaunch) {

                // If it's the first launch, navigate to OnboardingFragmentOne
                findNavController().navigate(R.id.action_splashFragment_to_onboardingFragment)

            } else {
                // It's not first launch
                if (auth.currentUser != null) {

                    val isUsernameSet = alteViewModel.isUsernameSet.value ?: false

                    if (isUsernameSet) {

                        // If username is set, navigate to HomeFragment
                        findNavController().navigate(R.id.action_splashFragment_to_homeFragment)

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