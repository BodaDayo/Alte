package com.rgbstudios.alte.ui.fragments

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.databinding.FragmentOtpBinding
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import java.util.concurrent.TimeUnit

class OtpFragment : Fragment() {

    private lateinit var binding: FragmentOtpBinding
    private lateinit var verificationId: String
    private lateinit var resendingToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var fullPhoneNumber: String
    private val sharedViewModel: AlteViewModel by activityViewModels()
    private val toastManager = ToastManager()
    private val firebase = FirebaseAccess()
    private val auth = firebase.auth
    private var resendCountdownTimer: CountDownTimer? = null
    private var resendCountdownSeconds: Long = 90

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addTextChangeListener()
        resendOTPTvVisibility()

        verificationId = sharedViewModel.verificationInfo.value!!.first
        resendingToken = sharedViewModel.verificationInfo.value!!.second
        fullPhoneNumber = sharedViewModel.verificationInfo.value!!.third

        binding.apply {
            // Explicitly request focus on the first EditText
            otpEditText1.requestFocus()

            // Show the keyboard
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(otpEditText1, InputMethodManager.SHOW_IMPLICIT)

            userPhoneNumber.text = fullPhoneNumber

            // Set initial countdown text
            resendCountdown.text = formatCountdown(resendCountdownSeconds)

            // Start the countdown timer
            startResendCountdown()

            verifyOTPBtn.setOnClickListener {
                handleOTPVerification(verificationId)
            }

            resendTextView.setOnClickListener {
                // Reset the countdown timer
                resetResendCountdown()

                // Resend OTP logic
                resendOTPTvVisibility()
                resendVerificationCode()
            }
        }
    }

    private fun handleOTPVerification(verificationId: String) {
        binding.apply {
            //collect otp from all the edit texts
            val typedOTP =
                (otpEditText1.text.toString() + otpEditText2.text.toString() + otpEditText3.text.toString()
                        + otpEditText4.text.toString() + otpEditText5.text.toString() + otpEditText6.text.toString())

            if (typedOTP.isNotEmpty()) {
                if (typedOTP.length == 6) {
                    val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(
                        verificationId, typedOTP
                    )
                    progressBar.visibility = View.VISIBLE
                    verifyOTPBtn.text = ""
                    signInWithPhoneAuthCredential(credential)
                } else {
                    Toast.makeText(requireContext(), "Please Enter Correct OTP", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(requireContext(), "Please Enter OTP", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun resendOTPTvVisibility() {
        binding.apply {
            otpEditText1.setText("")
            otpEditText2.setText("")
            otpEditText3.setText("")
            otpEditText4.setText("")
            otpEditText5.setText("")
            otpEditText6.setText("")

            resendTextView.isEnabled = false

            Handler(Looper.myLooper()!!).postDelayed(Runnable {

                resendTextView.isEnabled = true
            }, 90000)
        }
    }

    private fun resendVerificationCode() {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(requireActivity())                 // Activity (for callback binding)
            .setCallbacks(callbacks)
            .setForceResendingToken(resendingToken)// OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            signInWithPhoneAuthCredential(credential)
            binding.progressBar.visibility = View.GONE
            binding.verifyOTPBtn.text = getString(R.string.verify)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.

            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                Log.d("TAG", "onVerificationFailed: $e")
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
                Log.d("TAG", "onVerificationFailed: $e")
            }
            toastManager.showShortToast(
                requireContext(),
                e.message
            )
            binding.progressBar.visibility = View.GONE
            binding.verifyOTPBtn.text = getString(R.string.verify)
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            // Save verification ID and resending token so we can use them later
            sharedViewModel.saveVerificationInfo(verificationId, token, fullPhoneNumber)
            binding.progressBar.visibility = View.GONE
            binding.verifyOTPBtn.text = getString(R.string.verify)
        }
    }

    private fun addTextChangeListener() {
        binding.apply {
            otpEditText1.addTextChangedListener(EditTextWatcher(otpEditText1))
            otpEditText2.addTextChangedListener(EditTextWatcher(otpEditText2))
            otpEditText3.addTextChangedListener(EditTextWatcher(otpEditText3))
            otpEditText4.addTextChangedListener(EditTextWatcher(otpEditText4))
            otpEditText5.addTextChangedListener(EditTextWatcher(otpEditText5))
            otpEditText6.addTextChangedListener(EditTextWatcher(otpEditText6))
        }
    }

    inner class EditTextWatcher(private val view: View) : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            binding.apply {

                val text = p0.toString()
                when (view.id) {
                    R.id.otpEditText1 -> if (text.length == 1) otpEditText2.requestFocus()
                    R.id.otpEditText2 -> if (text.length == 1) otpEditText3.requestFocus() else if (text.isEmpty()) otpEditText1.requestFocus()
                    R.id.otpEditText3 -> if (text.length == 1) otpEditText4.requestFocus() else if (text.isEmpty()) otpEditText2.requestFocus()
                    R.id.otpEditText4 -> if (text.length == 1) otpEditText5.requestFocus() else if (text.isEmpty()) otpEditText3.requestFocus()
                    R.id.otpEditText5 -> if (text.length == 1) otpEditText6.requestFocus() else if (text.isEmpty()) otpEditText4.requestFocus()
                    R.id.otpEditText6 -> if (text.isEmpty()) otpEditText5.requestFocus()
                }
            }
        }

    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(
                        requireContext(),
                        "Authenticate Successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate
                    findNavController().navigate(R.id.action_otpFragment_to_homeFragment)
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.d("TAG", "signInWithPhoneAuthCredential: ${task.exception.toString()}")
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        toastManager.showShortToast(
                            requireContext(),
                            "Invalid phone number format."
                        )
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.verifyOTPBtn.text = getString(R.string.verify)
                }
            }
    }

    private fun startResendCountdown() {
        resendCountdownTimer = object : CountDownTimer(resendCountdownSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                resendCountdownSeconds = millisUntilFinished / 1000
                binding.resendCountdown.text = formatCountdown(resendCountdownSeconds)
            }

            override fun onFinish() {
                binding.resendCountdown.visibility = View.GONE
                resendCountdownTimer = null
            }
        }.start()
    }

    private fun resetResendCountdown() {
        // Cancel the existing countdown timer
        resendCountdownTimer?.cancel()

        // Reset the countdown duration
        resendCountdownSeconds = 90

        // Set initial countdown text
        binding.resendCountdown.text = formatCountdown(resendCountdownSeconds)

        // Start the countdown timer again
        startResendCountdown()
    }

    private fun formatCountdown(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

}