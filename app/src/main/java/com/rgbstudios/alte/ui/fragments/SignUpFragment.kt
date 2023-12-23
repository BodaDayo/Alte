package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.rgbstudios.alte.R
import com.rgbstudios.alte.databinding.FragmentSignInBinding
import com.rgbstudios.alte.databinding.FragmentSignUpBinding

class SignUpFragment : Fragment() {

    private lateinit var binding: FragmentSignUpBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            btnSignUp.setOnClickListener {
                val phoneNumber = editTextPhoneNumber.text.toString()
                // Call a method to handle Firebase phone authentication
                // For simplicity, you can implement this method in the same class or use a separate helper class.
                authenticateWithPhoneNumber(phoneNumber)
            }
        }
    }

    private fun authenticateWithPhoneNumber(phoneNumber: String) {
        // Implement Firebase Phone Authentication here
        // Use FirebaseAuth.getInstance().verifyPhoneNumber(...) to send an OTP to the provided phone number
        // Handle the verification process and navigate to HomeFragment on success
        // Refer to Firebase documentation for details on phone authentication: https://firebase.google.com/docs/auth/android/phone-auth
        // Example (you need to replace this with actual Firebase Phone Authentication code):
        /*
        FirebaseAuth.getInstance().verifyPhoneNumber(
            phoneNumber,
            60, // Timeout duration
            java.util.concurrent.TimeUnit.SECONDS,
            requireActivity(), // Activity (for callback binding)
            null
        ) { verificationId, token ->
            // The SMS verification has been sent
            // Save the verificationId somewhere (e.g., in shared preferences) for later use
            // Proceed to the verification process, and navigate to HomeFragment on success
            navigateToHomeFragment()
        }
        */
    }

}