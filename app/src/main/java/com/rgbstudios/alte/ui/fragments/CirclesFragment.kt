package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.firebase.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentCirclesBinding
import com.rgbstudios.alte.ui.adapters.PlanetAdapter
import com.rgbstudios.alte.ui.adapters.SelectionAdapter
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class CirclesFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

    private lateinit var binding: FragmentCirclesBinding
    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private var isCDLayoutOpen = false
    private var folksListSize = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCirclesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            // Set up the selectionAdapter
            val selectionAdapter = SelectionAdapter(true, requireContext(), alteViewModel,
                object : SelectionAdapter.ItemClickListener {
                    override fun onItemClick(user: UserDetails) {
                        updateList(false, user)

                    }
                }
            )

            // Set up the selectionRecyclerView
            selectionRecyclerView.setHasFixedSize(true)
            selectionRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            selectionRecyclerView.adapter = selectionAdapter


            // Set up the planet adapter
            val planetAdapter = PlanetAdapter(
                false,
                requireContext(),
                alteViewModel,
                object : PlanetAdapter.UserClickListener {
                    override fun onUserClick(user: UserDetails) {
                        updateList(true, user)
                    }

                    override fun onAvatarClick(user: UserDetails) {
                        updateList(true, user)
                    }
                })

            // Set up the planetRecyclerView
            planetRecyclerView.layoutManager = LinearLayoutManager(context)
            planetRecyclerView.adapter = planetAdapter

            // Set up the selectedAdapter
            val selectedAdapter = SelectionAdapter(false, requireContext(), alteViewModel,
                object : SelectionAdapter.ItemClickListener {
                    override fun onItemClick(user: UserDetails) {
                        // Do Nothing
                    }
                }
            )

            // Set up the selectedFolksRecyclerViewCD
            selectedFolksRecyclerViewCD.setHasFixedSize(true)
            selectedFolksRecyclerViewCD.layoutManager = GridLayoutManager(context, 4)
            selectedFolksRecyclerViewCD.adapter = selectedAdapter

            circlesFab.setOnClickListener {
                if (isCDLayoutOpen) {
                    val circleName = circleNameCD.toString().trim()
                    if (circleName.isEmpty()) {
                        circleNameCD.error = getString(R.string.please_enter_circle_name)
                    } else {
                        val finalList = alteViewModel.selectionList.value
                        if (finalList != null) {
                            // TODO use circle name and selection list and navigate}

                            clearSelectionList()
                        }
                    }
                } else {
                    if (folksListSize < 1) {
                        toastManager.showShortToast(
                            requireContext(),
                            "Select at least one member for your circle"
                        )
                    } else {
                        circleDetailLayout.visibility = View.VISIBLE
                        isCDLayoutOpen = true
                        circlesFab.setImageResource(R.drawable.check)

                    }
                }
            }

            userDetailLayout.setOnClickListener {
                findNavController().navigate(R.id.action_circlesFragment_to_profileFragment)
            }



            backToSelectionBtn.setOnClickListener {
                circleDetailLayout.visibility = View.GONE
                isCDLayoutOpen = false
                binding.circlesFab.setImageResource(R.drawable.forward_arrow)
            }

            popBackCircles.setOnClickListener {
                popBackStackManager()
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                popBackStackManager()
            }

            // Observe the currentUser data
            alteViewModel.currentUser.observe(viewLifecycleOwner) { user ->
                if (user != null) {
                    fullNameCD.text = user.name

                    userNameVerse.text = getString(R.string.user_name_template, user.username)

                    Glide.with(requireContext())
                        .asBitmap()
                        .load(user.avatarUri)
                        .placeholder(R.drawable.user_icon)
                        .into(userAvatarCircle)

                    userAVProgressBar.visibility = View.GONE
                }
            }

            // Observe the users List
            alteViewModel.allUsersList.observe(viewLifecycleOwner) { userList ->
                if (userList != null) {
                    // Get the current user's folks list
                    val currentUserFolks = alteViewModel.currentUser.value?.folks.orEmpty()

                    // Filter the userList based on whether user.uid is in the currentUser's folks list
                    val folks = userList.filter { currentUserFolks.contains(it.uid) }
                    folksListSize = folks.size

                    // Update the planetAdapter with the filtered list
                    planetAdapter.updatePlanetList(folks)

                    // Toggle visibility based on whether the folks list is empty
                    if (folks.isEmpty()) {
                        planetRecyclerView.visibility = View.GONE
                        emptyConnections.visibility = View.VISIBLE
                        circlesFab.visibility = View.GONE
                        separator.visibility = View.GONE
                        selectionCounter.visibility = View.GONE
                        folksTV.visibility = View.GONE

                        verseFragmentBtn.setOnClickListener {
                            findNavController().navigate(R.id.action_circlesFragment_to_alteVerseFragment)
                        }
                    } else {
                        planetRecyclerView.visibility = View.VISIBLE
                        emptyConnections.visibility = View.GONE
                        circlesFab.visibility = View.VISIBLE
                        separator.visibility = View.VISIBLE
                        selectionCounter.visibility = View.VISIBLE
                        folksTV.visibility = View.VISIBLE
                    }
                }
            }

            // Observe selection List
            alteViewModel.selectionList.observe(viewLifecycleOwner) { list ->
                if (list.isNotEmpty()) {
                    selectionAdapter.updateList(list)
                    selectedAdapter.updateList(list)

                    val selectedCount = list.size
                    val totalCount = folksListSize

                    selectionCounter.text =
                        getString(R.string.selected_count_template, selectedCount, totalCount)

                    val memberCount = list.size
                    membersCounterCD.text = getString(R.string.members_template, memberCount)
                } else {
                    selectionCounter.text = getString(R.string.selects_folks_to_add)
                    membersCounterCD.text = getString(R.string.members_template, 0)

                    selectionAdapter.updateList(emptyList())
                    selectedAdapter.updateList(emptyList())
                }
            }

        }
    }

    private fun clearSelectionList() {
        alteViewModel.clearSelectionList()
    }

    private fun updateList(toAdd: Boolean, user: UserDetails) {
        alteViewModel.updateSelectionList(toAdd, user)
    }

    private fun popBackStackManager() {
        if (isCDLayoutOpen) {

            binding.circleDetailLayout.visibility = View.GONE
            isCDLayoutOpen = false
            binding.circlesFab.setImageResource(R.drawable.forward_arrow)
        } else {
            clearSelectionList()
            // If no changes, simply pop back stack
            activity?.supportFragmentManager?.popBackStack()
        }
    }

}