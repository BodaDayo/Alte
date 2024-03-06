package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentCitizenProfileBinding
import com.rgbstudios.alte.utils.AvatarManager
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class CitizenProfileFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private val avatarManager = AvatarManager()
    private lateinit var binding: FragmentCitizenProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCitizenProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

            alteViewModel.selectedCitizen.observe(viewLifecycleOwner) { citizen ->
                if (citizen != null) {

                    namePF.text = citizen.name
                    usernamePF.text =
                        getString(R.string.user_name_template, citizen.username)
                    aboutPF.text =
                        citizen.about

                    locationPF.text = citizen.location
                    genderPF.text = citizen.gender


                    Glide.with(requireContext())
                        .asBitmap()
                        .load(citizen.avatarUri)
                        .placeholder(R.drawable.asset2)
                        .into(userAvatarPF)

                    val currentUser = alteViewModel.currentUser.value

                    val folksList = currentUser?.folks
                    val invitesList = currentUser?.invites
                    val requestsList = currentUser?.requests

                    val uid = citizen.uid

                    if (folksList != null) {
                        if (!folksList.contains(uid)) {
                            connectButtonLayout.visibility = View.VISIBLE

                            if (invitesList != null) {
                                if (invitesList.contains(uid)) {
                                    sentTextTV.visibility = View.VISIBLE
                                    connectButton.visibility = View.GONE
                                    requestedLayout.visibility = View.GONE
                                }
                            }
                            if (requestsList != null) {
                                if (requestsList.contains(uid)) {
                                    sentTextTV.visibility = View.GONE
                                    connectButton.visibility = View.GONE
                                    requestedLayout.visibility = View.VISIBLE
                                }
                            }

                            if (invitesList != null && requestsList != null) {
                                if (!invitesList.contains(uid) && !requestsList.contains(uid)) {
                                    sentTextTV.visibility = View.GONE
                                    connectButton.visibility = View.VISIBLE
                                    requestedLayout.visibility = View.GONE
                                }
                            }

                        }
                    }


                    connectButton.setOnClickListener {
                        if (currentUser != null) {
                            connectProgressBar.visibility = View.VISIBLE
                            connectButton.text = ""

                            alteViewModel.sendConnectRequest(
                                currentUser.uid,
                                citizen.uid
                            ) { successful, _ ->
                                if (successful) {
                                    toastManager.showShortToast(
                                        requireContext(),
                                        "Connect request sent!"
                                    )
                                } else {
                                    toastManager.showShortToast(
                                        requireContext(),
                                        "Something went wrong try again!"
                                    )
                                }
                                connectProgressBar.visibility = View.GONE
                                connectButton.text = resources.getString(R.string.connect)
                            }
                        }
                    }

                    acceptTV.setOnClickListener {
                        if (currentUser != null) {
                            alteViewModel.acceptRequest(currentUser.uid, citizen.uid) { isSuccessful, _ ->
                                if (isSuccessful) {
                                    toastManager.showLongToast(requireContext(), "Connection request accepted")
                                } else {
                                    toastManager.showLongToast(
                                        requireContext(),
                                        "Acceptance failed"
                                    )
                                }
                                alteViewModel.startUserListListener()
                            }
                        }
                    }

                    declineTV.setOnClickListener {
                        if (currentUser != null) {
                            alteViewModel.declineRequest(currentUser.uid, citizen.uid) { isSuccessful, _ ->
                                if (isSuccessful) {
                                    toastManager.showLongToast(requireContext(), "Connection request declined")
                                } else {
                                    toastManager.showLongToast(
                                        requireContext(),
                                        "Decline failed"
                                    )
                                }
                                alteViewModel.startUserListListener()
                            }
                        }
                    }

                }
            }
        }
    }
}