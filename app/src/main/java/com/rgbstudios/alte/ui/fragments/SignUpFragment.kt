package com.rgbstudios.alte.ui.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentSignUpBinding
import com.rgbstudios.alte.utils.AvatarManager
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory
import java.io.ByteArrayOutputStream

class SignUpFragment : Fragment() {

    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by viewModels {
        AlteViewModelFactory(requireActivity().application, AlteRepository(firebase))
    }

    private val auth = firebase.auth
    private lateinit var binding: FragmentSignUpBinding
    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()

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
            signUpButton.isEnabled = false

            signInTv.setOnClickListener {
                findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
            }

            agreementRead.setOnCheckedChangeListener { _, isChecked ->
                // Update the isEnabled state of signUpButton based on checkbox state
                signUpButton.isEnabled = isChecked
            }

            setAgreementTV()

            agreementTV.setOnClickListener {
                dialogManager.showPrivacyPolicyDialog(this@SignUpFragment) { agreed ->
                    if (agreed) {
                        agreementRead.isChecked = true
                    }
                }
            }

            signUpButton.setOnClickListener {

                val email = emailEt.text.toString().trim()
                val pass = passEt.text.toString().trim()
                val confirmPass = confirmPassEt.text.toString().trim()

                if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {

                    if (isValidEmail(email)) {

                        if (pass == confirmPass) {

                            // Regex pattern for a strong password
                            val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"

                            if (pass.matches(Regex(passwordPattern))) {

                                progressBar.visibility = View.VISIBLE
                                signUpButton.text = ""

                                firebase.signUp(email, pass) { signUpSuccessful, errorMessage ->
                                    if (signUpSuccessful) {
                                        progressWithSignUp(email, pass)
                                    } else {
                                        toastManager.showShortToast(
                                            requireContext(),
                                            errorMessage
                                        )
                                    }
                                    progressBar.visibility = View.GONE
                                    signUpButton.text = getString(R.string.register)
                                }

                            } else {
                                toastManager.showLongToast(
                                    requireContext(),
                                    "Password must be at least 8 characters long, containing uppercase, lowercase, and numbers.",
                                )
                            }
                        } else {
                            toastManager.showShortToast(
                                requireContext(),
                                "Passwords do not match. Please try again.",
                            )
                        }
                    } else {
                        // Show an error message for invalid email format
                        toastManager.showShortToast(requireContext(), "Invalid email format.")
                    }
                } else {
                    toastManager.showShortToast(
                        requireContext(),
                        "Empty fields are not allowed !!"
                    )
                }
            }
        }
    }

    private fun setAgreementTV() {
        binding.apply {

            val fullText = resources.getString(R.string.privacy_agreement_instructions)

            val privacyPolicyStart = fullText.indexOf("Privacy Policy")
            val privacyPolicyEnd = privacyPolicyStart + "Privacy Policy".length

            val spannableString = SpannableString(fullText)
            spannableString.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.md_theme_light_secondary)),
                privacyPolicyStart,
                privacyPolicyEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            agreementTV.text = spannableString

        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    private fun progressWithSignUp(email: String, pass: String) {

        // Update the usernameSetStatus in the viewModel
        alteViewModel.updateUsernameSetStatus(false)
        findNavController().navigate(R.id.action_signUpFragment_to_completeRegistrationFragment)
    }



}