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
import com.google.firebase.auth.FirebaseAuth
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentSignInBinding
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class SignInFragment : Fragment() {

    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }
    private lateinit var binding: FragmentSignInBinding
    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            signUpTv.setOnClickListener {
                findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
            }

            forgotPasswordTV.setOnClickListener {
                showForgotPasswordDialog()
            }

            loginButton.setOnClickListener {
                val email = emailEt.text.toString().trim()
                val pass = passEt.text.toString().trim()

                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    progressBar.visibility = View.VISIBLE
                    loginButton.text = ""

                    alteViewModel.signIn(email, pass) { signInSuccessful, errorMessage ->
                        if (signInSuccessful) {
                            navigateToMessagesFragment()
                        } else {
                            toastManager.showShortToast(
                                requireContext(),
                                errorMessage
                            )
                        }
                        progressBar.visibility = View.GONE
                        loginButton.text = getString(R.string.login)
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

    private fun showForgotPasswordDialog() {
        val auth = FirebaseAuth.getInstance()
        dialogManager.showForgotPasswordDialog(this, auth)
    }

    private fun navigateToMessagesFragment() {
        Handler(Looper.getMainLooper()).postDelayed({
            findNavController().navigate(R.id.action_signInFragment_to_messagesFragment)
        }, 1000)
    }

}