package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rgbstudios.alte.databinding.FragmentAlteConnectionsBinding
import com.rgbstudios.alte.ui.adapters.InvitesAdapter
import com.rgbstudios.alte.ui.adapters.RequestsAdapter
import com.rgbstudios.alte.viewmodel.AlteViewModel

class AlteConnectionsFragment(private val alteViewModel: AlteViewModel) : Fragment() {

    private lateinit var binding: FragmentAlteConnectionsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlteConnectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectedItem = alteViewModel.connectionsItemSelected.value

        binding.apply {

            when (selectedItem) {
                1 -> {
                    // Open Requests Layout
                    requestsLayout.visibility = View.VISIBLE

                    // Set up the requests adapter
                    val requestsAdapter = RequestsAdapter(requireContext(), alteViewModel)

                    // Set up the requestsRecyclerView
                    requestsRecyclerView.layoutManager = LinearLayoutManager(context)
                    requestsRecyclerView.adapter = requestsAdapter

                    // Observe the users List
                    alteViewModel.allUsersList.observe(viewLifecycleOwner) { userList ->
                        if (userList != null) {
                            // Get the current user's requests list
                            val currentUser = alteViewModel.currentUser.value
                            val userRequests = currentUser?.requests.orEmpty()

                            // List of users that sent connection requests
                            val requesters = userList.filter { userRequests.contains(it.uid) }

                            requestsAdapter.updateRequestList(requesters)

                            // Toggle visibility based on whether the requesters list is empty
                            if (requesters.isEmpty()) {
                                requestsRecyclerView.visibility = View.GONE
                                emptyRequests.visibility = View.VISIBLE
                            } else {
                                requestsRecyclerView.visibility = View.VISIBLE
                                emptyRequests.visibility = View.GONE
                            }
                        }
                    }
                }

                2 -> {
                    // Open Invites Layout
                    invitesLayout.visibility = View.VISIBLE

                    // Set up the invites adapter
                    val invitesAdapter = InvitesAdapter(requireContext(), alteViewModel)

                    // Set up the invitesRecyclerView
                    invitesRecyclerView.layoutManager = LinearLayoutManager(context)
                    invitesRecyclerView.adapter = invitesAdapter

                    // Observe the users List
                    alteViewModel.allUsersList.observe(viewLifecycleOwner) { userList ->
                        if (userList != null) {
                            // Get the current user's invites list
                            val currentUser = alteViewModel.currentUser.value
                            val userInvites = currentUser?.invites.orEmpty()

                            // List of users that that have ben invited
                            val invitees = userList.filter { userInvites.contains(it.uid) }

                            invitesAdapter.updateInvitesList(invitees)

                            // Toggle visibility based on whether the requesters list is empty
                            if (invitees.isEmpty()) {
                                invitesRecyclerView.visibility = View.GONE
                                emptyInvites.visibility = View.VISIBLE
                            } else {
                                invitesRecyclerView.visibility = View.VISIBLE
                                emptyInvites.visibility = View.GONE
                            }
                        }
                    }

                }
            }

            closeRequests.setOnClickListener {
                closeConnectionsPane()
            }

            closeInvites.setOnClickListener {
                closeConnectionsPane()
            }
        }
    }

    private fun closeConnectionsPane() {
        alteViewModel.toggleSlider(true)
    }

}