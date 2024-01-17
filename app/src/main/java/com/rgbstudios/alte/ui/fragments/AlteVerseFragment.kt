package com.rgbstudios.alte.ui.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentAlteVerseBinding
import com.rgbstudios.alte.ui.adapters.AlteVerseAdapter
import com.rgbstudios.alte.ui.adapters.PlanetAdapter
import com.rgbstudios.alte.utils.AvatarManager
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class AlteVerseFragment : Fragment() {
    private val firebase = FirebaseAccess()
    private val avatarManager = AvatarManager()

    private val alteViewModel: AlteViewModel by viewModels {
        AlteViewModelFactory(requireActivity().application, AlteRepository(firebase))
    }

    private val auth = firebase.auth
    private lateinit var binding: FragmentAlteVerseBinding
    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private var planetIsVisible = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlteVerseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            // Set up the planet adapter
            val planetAdapter = PlanetAdapter(requireContext(), alteViewModel)

            // Set up the planetRecyclerView
            planetRecyclerView.setHasFixedSize(true)
            planetRecyclerView.layoutManager = LinearLayoutManager(context)
            planetRecyclerView.adapter = planetAdapter

            val userList = listOf(
                UserDetails(
                    uid = "1",
                    name = "Ade Ugbonna",
                    username = "Ade01",
                    about = "Gentle Soul",
                    gender = "",
                    dob = null,
                    avatar = getRandomAvatar(avatarManager.defaultAvatarsList)
                ),
                UserDetails(
                    uid = "2",
                    name = "Ola Johnson",
                    username = "BigBoss",
                    about = "DOn",
                    gender = "",
                    dob = null,
                    avatar = getRandomAvatar(avatarManager.defaultAvatarsList)
                ),
                UserDetails(
                    uid = "3",
                    name = "Olu",
                    username = "MaintainFlow",
                    about = "Crazy Soul",
                    gender = "",
                    dob = null,
                    avatar = getRandomAvatar(avatarManager.defaultAvatarsList)
                ),
                UserDetails(
                    uid = "4",
                    name = "Bola",
                    username = "Blaise",
                    about = "Gentle Soul",
                    gender = "",
                    dob = null,
                    avatar = getRandomAvatar(avatarManager.defaultAvatarsList)
                ),
                UserDetails(
                    uid = "5",
                    name = "Jon",
                    username = "JonSnow",
                    about = "Gentle Soul",
                    gender = "",
                    dob = null,
                    avatar = getRandomAvatar(avatarManager.defaultAvatarsList)
                ),
                UserDetails(
                    uid = "6",
                    name = "Bimpe",
                    username = "DaintyFl0095",
                    about = "Gentle Soul",
                    gender = "",
                    dob = null,
                    avatar = getRandomAvatar(avatarManager.defaultAvatarsList)
                ),
                UserDetails(
                    uid = "7",
                    name = "Shittu Alimi",
                    username = "King1",
                    about = "Gentle Soul",
                    gender = "",
                    dob = null,
                    avatar = getRandomAvatar(avatarManager.defaultAvatarsList)
                ),
                UserDetails(
                    uid = "8",
                    name = "Ade Mikel",
                    username = "Flows676",
                    about = "Gentle Soul",
                    gender = "",
                    dob = null,
                    avatar = getRandomAvatar(avatarManager.defaultAvatarsList)
                ),
                UserDetails(
                    uid = "9",
                    name = "Onana",
                    username = "Onana01",
                    about = "Gentle Soul",
                    gender = "",
                    dob = null,
                    avatar = getRandomAvatar(avatarManager.defaultAvatarsList)
                ),

                )

            planetAdapter.updatePlanetList(userList)

            // Set up the planet adapter
            val alteVerseAdapter = AlteVerseAdapter(requireContext(), alteViewModel)

            // Set up the planetRecyclerView
            alteVerseRecyclerView.setHasFixedSize(true)
            alteVerseRecyclerView.layoutManager = LinearLayoutManager(context)
            alteVerseRecyclerView.adapter = alteVerseAdapter

            alteVerseAdapter.updateAlteVerseList(userList)

            verseFab.setOnClickListener {
                toggleVerseVisibility()
            }
        }

    }

    private fun toggleVerseVisibility() {
        binding.apply {
            if (planetIsVisible) {
                planetLayout.visibility = View.GONE
                planetIsVisible = false
                verseFab.setImageResource(R.drawable.people)
                verseFab.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
            } else {
                planetLayout.visibility = View.VISIBLE
                planetIsVisible = true
                verseFab.setImageResource(R.drawable.logo)
                verseFab.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.md_theme_light_secondary)
            }
        }
    }

    private fun getRandomAvatar(avatarList: List<Int>): Bitmap {
        val randomAvatar = avatarList.random()
        return BitmapFactory.decodeResource(resources, randomAvatar)
    }

}