package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.firebase.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentBroadcastBinding
import com.rgbstudios.alte.ui.adapters.PlanetAdapter
import com.rgbstudios.alte.ui.adapters.SelectionAdapter
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class BroadcastFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

    private lateinit var binding: FragmentBroadcastBinding
    private var folksListSize = 0
    private var isInSelectionMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBroadcastBinding.inflate(inflater, container, false)
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

            broadcastFab.setOnClickListener {
                val recipientList = alteViewModel.selectionList.value
                if (recipientList != null) {
                    // TODO Proceed with recipientList
                    //findNavController().navigate(R.id.action_broadcastFragment_to_chatFragment)

                    clearSelectionList()
                }
            }

            popBack.setOnClickListener {
                popBackStackManager()
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                popBackStackManager()
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
                        broadcastFab.visibility = View.GONE
                        separator.visibility = View.GONE
                        selectionCounter.visibility = View.GONE
                        folksTV.visibility = View.GONE

                        verseFragmentBtn.setOnClickListener {
                            findNavController().navigate(R.id.action_circlesFragment_to_alteVerseFragment)
                        }
                    } else {
                        planetRecyclerView.visibility = View.VISIBLE
                        emptyConnections.visibility = View.GONE
                        broadcastFab.visibility = View.VISIBLE
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

                    val selectedCount = list.size
                    val totalCount = folksListSize

                    selectionCounter.text =
                        getString(R.string.selected_count_template, selectedCount, totalCount)

                    isInSelectionMode = true

                } else {
                    selectionCounter.text = getString(R.string.selects_folks_to_add)

                    selectionAdapter.updateList(emptyList())

                    isInSelectionMode = false
                }
            }
        }
    }

    private fun updateList(toAdd: Boolean, user: UserDetails) {
        alteViewModel.updateSelectionList(toAdd, user)
    }

    private fun clearSelectionList() {
        alteViewModel.clearSelectionList()
    }

    private fun popBackStackManager() {
        if (isInSelectionMode) {
            clearSelectionList()
            isInSelectionMode = false
        } else {
            // If no changes, simply pop back stack
            activity?.supportFragmentManager?.popBackStack()
        }
    }

}