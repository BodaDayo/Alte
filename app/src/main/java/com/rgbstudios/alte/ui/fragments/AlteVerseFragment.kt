package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseUser
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentAlteVerseBinding
import com.rgbstudios.alte.ui.adapters.AlteVerseAdapter
import com.rgbstudios.alte.ui.adapters.PlanetAdapter
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class AlteVerseFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

    private lateinit var binding: FragmentAlteVerseBinding
    private var planetIsVisible = true
    private var isSlidingPaneLayoutOpen = false
    private var currentUser: FirebaseUser? = null

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
            currentUser = firebase.currentUser

            if (currentUser != null) {
                alteViewModel.startMessagesListener(currentUser!!.uid)
            }

            // Lock the SlidingPaneLayout
            sliderAlteVerse.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED

            // Set up the planet adapter
            val planetAdapter = PlanetAdapter(
                true,
                requireContext(),
                alteViewModel,
                object : PlanetAdapter.UserClickListener {
                    override fun onUserClick(user: UserDetails) {
                        val currentUserId = alteViewModel.currentUser.value?.uid ?: ""
                        alteViewModel.startChatListener(currentUserId, user)
                        navigateToChatFragment()
                    }

                    override fun onAvatarClick(user: UserDetails) {
                        alteViewModel.setSelectedCitizen(user)
                        findNavController().navigate(R.id.action_alteVerseFragment_to_citizenProfileFragment)
                    }
                })

            // Set up the planetRecyclerView
            planetRecyclerView.layoutManager = LinearLayoutManager(context)
            planetRecyclerView.adapter = planetAdapter

            // Set up the alte verse adapter
            val alteVerseAdapter = AlteVerseAdapter(requireContext(), alteViewModel,
                object : AlteVerseAdapter.UserClickListener {
                    override fun onUserClick(user: UserDetails) {
                        if (user.uid == alteViewModel.currentUser.value?.uid) {
                            findNavController().navigate(R.id.action_alteVerseFragment_to_profileFragment)
                        } else {
                            alteViewModel.setSelectedCitizen(user)
                            findNavController().navigate(R.id.action_alteVerseFragment_to_citizenProfileFragment)
                        }
                        planetIsVisible = true
                    }
                })

            // Set up the alteVerseRecyclerView
            alteVerseRecyclerView.layoutManager = LinearLayoutManager(context)
            alteVerseRecyclerView.adapter = alteVerseAdapter

            userDetailLayout.setOnClickListener {
                findNavController().navigate(R.id.action_alteVerseFragment_to_profileFragment)
            }

            verseFab.setOnClickListener {
                toggleVerseVisibility()
            }

            requestsTV.setOnClickListener {
                openConnectionsPane(1)
            }

            invitesTV.setOnClickListener {
                openConnectionsPane(2)
            }

            userAVProgressBar.visibility = View.VISIBLE

            // Observe the currentUser data
            alteViewModel.currentUser.observe(viewLifecycleOwner) { user ->
                if (user != null) {
                    fullNameVerse.text = user.name

                    userNameVerse.text = getString(R.string.user_name_template, user.username)

                    Glide.with(requireContext())
                        .asBitmap()
                        .load(user.avatarUri)
                        .placeholder(R.drawable.user_icon)
                        .into(userAvatarVerse)

                    userAVProgressBar.visibility = View.GONE

                    val userRequests = user.requests.orEmpty()

                    // Toggle visibility based on whether the requesters list is empty
                    if (userRequests.isEmpty()) {
                        requestsTV.visibility = View.GONE
                    } else {
                        requestsTV.visibility = View.VISIBLE
                    }
                }
            }

            // Observe the users List
            alteViewModel.allUsersList.observe(viewLifecycleOwner) { userList ->
                if (userList != null) {

                    // Get the current user's folks list
                    val currentUsersFolks = alteViewModel.currentUser.value?.folks.orEmpty()

                    // Filter the userList based on whether user.uid is in the currentUser's folks list
                    val folks = userList.filter { currentUsersFolks.contains(it.uid) }

                    // Update the adapters with the filtered lists
                    planetAdapter.updatePlanetList(folks)

                    val allUsers = userList.toMutableList()

                    allUsers.find { it.uid == currentUser?.uid }?.let { foundTriple ->
                        // Remove the item from its current position
                        allUsers.remove(foundTriple)

                        // Add it to the top of the list
                        allUsers.add(0, foundTriple)
                    }

                    alteVerseAdapter.updateAlteVerseList(allUsers)

                    // Toggle visibility based on whether the folks list is empty
                    if (folks.isEmpty()) {
                        planetRecyclerView.visibility = View.GONE
                        emptyConnections.visibility = View.VISIBLE
                        arrowAnimationView.visibility = View.VISIBLE
                        folksCountTV.visibility = View.GONE
                    } else {
                        planetRecyclerView.visibility = View.VISIBLE
                        emptyConnections.visibility = View.GONE
                        arrowAnimationView.visibility = View.GONE

                        val folksCount = folks.size
                        val folksCountText = when {
                            folksCount == 1 -> resources.getQuantityString(
                                R.plurals.folks_count,
                                folksCount,
                                folksCount
                            )

                            folksCount < 1000 -> resources.getQuantityString(
                                R.plurals.folks_count,
                                folksCount,
                                folksCount
                            )

                            else -> String.format("%,d Folks", folksCount)
                        }

                        folksCountTV.text = folksCountText
                        folksCountTV.visibility = View.VISIBLE
                    }
                }
            }

            // Observe slider toggle
            alteViewModel.closeSlider.observe(viewLifecycleOwner) { toClose ->
                isSlidingPaneLayoutOpen = if (toClose) {
                    // Close the connections pane
                    sliderAlteVerse.closePane()
                    false
                } else {
                    // Open the connections pane
                    binding.sliderAlteVerse.openPane()
                    true
                }
            }

            popBackAV.setOnClickListener {
                popBackStackManager()
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                popBackStackManager()
            }

        }
    }

    private fun navigateToChatFragment() {
        findNavController().navigate(R.id.action_alteVerseFragment_to_chatFragment)
    }

    private fun toggleVerseVisibility() {
        binding.apply {
            if (planetIsVisible) {
                planetLayout.visibility = View.GONE
                arrowAnimationView.visibility = View.GONE
                planetIsVisible = false
                verseFab.setImageResource(R.drawable.people)
                verseFab.backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_dark_secondary
                )
            } else {
                planetLayout.visibility = View.VISIBLE
                planetIsVisible = true
                verseFab.setImageResource(R.drawable.logo)
                verseFab.backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.md_theme_light_primary
                )
            }
        }
    }

    private fun openConnectionsPane(item: Int) {
        alteViewModel.setConnectionsItem(item)

        // Replace the connections pane with the AlteConnectionsFragment
        val alteConnectionsFragment = AlteConnectionsFragment(alteViewModel)
        childFragmentManager.beginTransaction()
            .replace(R.id.connectionsLayout, alteConnectionsFragment)
            .commit()

        alteViewModel.toggleSlider(false)
    }

    private fun popBackStackManager() {
        if (isSlidingPaneLayoutOpen) {
            alteViewModel.toggleSlider(true)
        } else {
            // If no changes, simply pop back stack
            activity?.supportFragmentManager?.popBackStack()
        }
    }
}