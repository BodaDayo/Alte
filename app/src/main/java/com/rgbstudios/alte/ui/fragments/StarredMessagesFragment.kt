package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.Chat
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentStarredMessagesBinding
import com.rgbstudios.alte.ui.adapters.StarredMessagesAdapter
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class StarredMessagesFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

    private lateinit var binding: FragmentStarredMessagesBinding
    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private var isSearchResultsShowing = false
    private var isSearchViewVisible = false
    private lateinit var starredMessagesAdapter: StarredMessagesAdapter
    private var selectedChats: List<Chat> = emptyList()
    private var allStarredMessages: List<Chat> = emptyList()
    private var isInHighlightMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStarredMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            alteViewModel.clearStarredSelection()

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(query: String?): Boolean {
                    isSearchResultsShowing = if (query.isNullOrEmpty()) {
                        starredMessagesAdapter.updateConvoList(allStarredMessages.reversed())
                        false
                    } else {
                        val searchResults = allStarredMessages.filter { it.message.lowercase().contains(query) }
                        starredMessagesAdapter.updateConvoList(searchResults)
                        true
                    }
                    return true
                }
            })

            searchIV.setOnClickListener {
                searchView.visibility = View.VISIBLE
                isSearchViewVisible = true
                searchIV.visibility = View.GONE
                titleTV.visibility = View.GONE
            }

            popBack.setOnClickListener {
                popBackStackManager()
            }

            unstarMessageIV.setOnClickListener {
                 selectedChats.forEach { chat ->
                    val currentUserId = alteViewModel.currentUser.value?.uid
                    currentUserId?.let { uid ->

                        val receiverId = chat.let { if (it.senderId == currentUserId) it.receiverId else it.senderId }
                        alteViewModel.updateChatStarredStatus(
                            false,
                            uid,
                            chat,
                        ) { success ->
                            if (!success) {
                                toastManager.showShortToast(requireContext(), "That was unsuccessful, check your network and try again!")
                            }
                        }
                    }
                }
            }

            // Set up the StarredMessages adapter
            starredMessagesAdapter = StarredMessagesAdapter(requireContext(), alteViewModel,
                object : StarredMessagesAdapter.MessageClickListener {
                    override fun onMessageClick(chat: Chat) {
                        val currentUserId = alteViewModel.currentUser.value?.uid ?: ""
                        val users = alteViewModel.allUsersList.value
                        val recipientUid = chat.let { if (it.senderId == currentUserId) it.receiverId else it.senderId }

                        val recipient = users?.find { it.uid == recipientUid }
                        if (recipient != null) {
                            alteViewModel.startChatListener(currentUserId, recipient)
                            findNavController().navigate(R.id.action_starredMessagesFragment_to_chatFragment)
                        }

                    }

                })

            // Set up the starredMessagesRecyclerView
            starredMessagesRecyclerView.layoutManager = LinearLayoutManager(context)
            starredMessagesRecyclerView.adapter = starredMessagesAdapter

            alteViewModel.currentUser.observe(viewLifecycleOwner) { user ->
                if (user != null) {
                    allStarredMessages = user.starredMessages.mapNotNull { it.second }
                    starredMessagesAdapter.updateConvoList(allStarredMessages.reversed())
                }
            }

            alteViewModel.selectedStarredItems.observe(viewLifecycleOwner) { chats ->
                if (chats != null) {
                    selectedChats = chats

                    isInHighlightMode = if (chats.isNotEmpty()) {
                        chatSelectedLayout.visibility = View.VISIBLE
                        true
                    } else {
                        chatSelectedLayout.visibility = View.GONE
                        false
                    }

                    selectionCounter.text = selectedChats.size.toString()
                }
            }

            // Customize onBackPressed method
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                popBackStackManager()
            }

        }
    }

    private fun popBackStackManager() {
        binding.apply {
            if (isInHighlightMode) {

                starredMessagesAdapter.clearSelection()
                chatSelectedLayout.visibility = View.GONE

                isInHighlightMode = false
            } else if (isSearchResultsShowing) {

                searchView.setQuery(null, false)
                isSearchResultsShowing = false
            } else if (isSearchViewVisible) {

                searchView.visibility = View.INVISIBLE
                isSearchViewVisible = false
                searchIV.visibility = View.VISIBLE
                titleTV.visibility = View.VISIBLE
            } else {
                // If no changes, simply pop back stack
                activity?.supportFragmentManager?.popBackStack()
            }
        }
    }
}