package com.rgbstudios.alte.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.firebase.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentSettingsDetailsBinding
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory


class SettingsDetailsFragment() : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

    private lateinit var binding: FragmentSettingsDetailsBinding
    private lateinit var fragmentContext: Context
    private val dialogManager = DialogManager()
    private val toastManager = ToastManager()
    private val thisFragment = this

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsDetailsBinding.inflate(inflater, container, false)
        fragmentContext = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val item = alteViewModel.settingsItemSelected.value

        binding.apply {
            when (item) {
                getString(R.string.account) -> {
                    accountsLayout.visibility = View.VISIBLE

                }

                getString(R.string.notifications) -> {
                    notificationsLayout.visibility = View.VISIBLE


                }

                getString(R.string.privacy) -> {
                    privacyLayout.visibility = View.VISIBLE

                }
            }

        }
    }

}