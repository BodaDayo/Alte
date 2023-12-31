package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.rgbstudios.alte.R
import com.rgbstudios.alte.databinding.FragmentSignInBinding
import com.rgbstudios.alte.utils.CountryCodeProvider
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import java.util.concurrent.TimeUnit

class SignInFragment : Fragment() {

    private val sharedViewModel: AlteViewModel by activityViewModels()

    private lateinit var binding: FragmentSignInBinding
    private lateinit var fullPhoneNumber:String
    private val countryCodeProvider = CountryCodeProvider()
    private val toastManager = ToastManager()
    private val auth = FirebaseAuth.getInstance()
    private var verificationStarted = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        verificationStarted = savedInstanceState?.getBoolean("verificationStarted") ?: false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            // Set up the Spinner (Dropdown)
            val countryCodeList = countryCodeProvider.getCountryCodes()

            val adapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, countryCodeList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            numberPrefixSpinner.adapter = adapter

            // Set the default selection
            val defaultSelection = countryCodeList.indexOf("+234")
            numberPrefixSpinner.setSelection(defaultSelection)

            sendOTPBtn.setOnClickListener {
                val countryCode = numberPrefixSpinner.selectedItem as String
                val phoneNumber = phoneEditTextNumber.text.toString()

                if (phoneNumber.isNotBlank()) {
                    fullPhoneNumber = "$countryCode$phoneNumber"

                    progressBar.visibility = View.VISIBLE
                    sendOTPBtn.text = ""

                    // Initiate phone authentication
                    initiatePhoneAuthentication()
                } else {
                    toastManager.showShortToast(requireContext(), "Please enter a phone number.")
                }
            }
        }
    }

    private fun initiatePhoneAuthentication() {
        // The test phone number and code should be whitelisted in the console.
        val phoneNumber = "+16505554567"
        val smsCode = "123456"

        val firebaseAuthSettings = auth.firebaseAuthSettings

        // Configure faking the auto-retrieval with the whitelisted numbers.
        firebaseAuthSettings.setAutoRetrievedSmsCodeForPhoneNumber(phoneNumber, smsCode)

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(30L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                    verificationStarted = false

                    binding.progressBar.visibility = View.GONE
                    binding.sendOTPBtn.text = getString(R.string.send)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // Handle verification failure
                    if (e is FirebaseAuthInvalidCredentialsException) {
                        toastManager.showShortToast(requireContext(), "Invalid phone number format.")
                    } else if (e is FirebaseTooManyRequestsException) {
                        toastManager.showShortToast(requireContext(), "SMS quota exceeded. Please try again later.")
                    }
                    verificationStarted = false
                    binding.progressBar.visibility = View.GONE
                    binding.sendOTPBtn.text = getString(R.string.send)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    sharedViewModel.saveVerificationInfo(verificationId, token, fullPhoneNumber)
                    binding.progressBar.visibility = View.GONE
                    binding.sendOTPBtn.text = getString(R.string.send)

                    // Navigate to OtpFragment
                    navigateToOtpFragment()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

        /*

        toastManager.showLongToast(requireContext(), fullPhoneNumber)

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
        verificationStarted = true

         */
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
            verificationStarted = false
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // Handle verification failure
            if (e is FirebaseAuthInvalidCredentialsException) {
                toastManager.showShortToast(requireContext(), "Invalid phone number format.")
            } else if (e is FirebaseTooManyRequestsException) {
                toastManager.showShortToast(requireContext(), "SMS quota exceeded. Please try again later.")
            }
            verificationStarted = false
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            sharedViewModel.saveVerificationInfo(verificationId, token, fullPhoneNumber)

            // Navigate to OtpFragment
            navigateToOtpFragment()
        }
    }

    private fun navigateToOtpFragment() {
        findNavController().navigate(R.id.action_signInFragment_to_otpFragment)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(requireContext() , "Authentication Successful!" , Toast.LENGTH_SHORT).show()
                    // Navigate
                    findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.d("TAG", "signInWithPhoneAuthCredential: ${task.exception.toString()}")
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    // Update UI
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the state of flags
        outState.putBoolean("verificationStarted", verificationStarted)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Restore the state of flags
        verificationStarted =
            savedInstanceState?.getBoolean("verificationStarted") ?: false

        binding.apply {
            // Reset imageSample layout visibility
            if (verificationStarted) {
                initiatePhoneAuthentication()
            }
        }
    }

}