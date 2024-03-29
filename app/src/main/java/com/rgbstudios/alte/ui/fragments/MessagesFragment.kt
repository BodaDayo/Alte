package com.rgbstudios.alte.ui.fragments

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseUser
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.Chat
import com.rgbstudios.alte.data.model.Peep
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.firebase.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentMessagesBinding
import com.rgbstudios.alte.ui.adapters.ConvoAdapter
import com.rgbstudios.alte.ui.adapters.PeepAdapter
import com.rgbstudios.alte.utils.DateTimeManager
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ImageHandler
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class MessagesFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private val imageHandler = ImageHandler()
    private val dateTimeManager = DateTimeManager()
    private lateinit var binding: FragmentMessagesBinding
    private lateinit var convoAdapter: ConvoAdapter
    private var currentUser: FirebaseUser? = null
    private var isCropImageLayoutVisible = false
    private lateinit var callback: OnBackPressedCallback

    private var isSearchResultsShowing = false
    private var isSearchViewVisible = false
    private var selectedConvoList: List<UserDetails> = emptyList()
    private var allConvoList: List<Triple<Chat, String, Pair<Int, Int>>> = emptyList()
    private var isInSelectionMode = false
    private var isFilledList = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            currentUser = firebase.auth.currentUser

            if (currentUser != null) {
                alteViewModel.startDatabaseListeners(requireActivity().applicationContext)
            }

            // Set up the planet adapter
            val peepAdapter = PeepAdapter(requireContext(),
                object : PeepAdapter.PeepClickListener {
                    override fun onPeepClick(userPeepPair: Pair<UserDetails, List<Peep>>) {
                        alteViewModel.setPeepItem(userPeepPair)
                        findNavController().navigate(R.id.action_messagesFragment_to_peepsFragment)
                    }
                })

            // Set up the planetRecyclerView to scroll horizontally
            peepRecyclerView.setHasFixedSize(true)
            peepRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            peepRecyclerView.adapter = peepAdapter

            // Set up the Convo adapter
            convoAdapter = ConvoAdapter(requireContext(), alteViewModel,
                object : ConvoAdapter.ConvoClickListener {
                    override fun onConvoClick(user: UserDetails) {
                        val currentUserId = alteViewModel.currentUser.value?.uid ?: ""
                        alteViewModel.startChatListener(currentUserId, user)
                        navigateToChatFragment()
                    }

                    override fun onAvatarClick(user: UserDetails) {
                        alteViewModel.setSelectedCitizen(user)
                        findNavController().navigate(R.id.action_messagesFragment_to_citizenProfileFragment)
                    }
                })

            // Set up the convoRecyclerView
            convoRecyclerView.layoutManager = LinearLayoutManager(context)
            convoRecyclerView.adapter = convoAdapter

            myPeepLayout.setOnClickListener {
                // TODO Change to open camera
                // TODO Change to open dialog of list of peeps and view plus a add new peep button
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            moreOptions.setOnClickListener { view ->
                showOverflowMenu(view)
            }

            fab.setOnClickListener {
                findNavController().navigate(R.id.action_messagesFragment_to_alteVerseFragment)
            }

            /**
             * ----chatSelectedLayout buttons-----------------------------
             */

            selectAllIV.setOnClickListener {
                if (isFilledList) {
                    convoAdapter.clearSelection()
                } else {
                    convoAdapter.fillSelection()
                }
            }

            markUnreadIV.setOnClickListener {
                // TODO mark unread
                leaveSelectionMode()
            }

            clearChatIV.setOnClickListener {
                // TODO clear Chat
                leaveSelectionMode()
            }

            /**
             * ----------------------------------------------------------
             */

            swipeRefreshLayout.setOnRefreshListener {
                // Update data from database when the user performs the pull-to-refresh action
                if (currentUser != null) {
                    updateLists()
                }
            }

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(query: String?): Boolean {
                    isSearchResultsShowing = if (query.isNullOrEmpty()) {
                        convoAdapter.updateConvoList(allConvoList.reversed())
                        false
                    } else {
                        val searchResults =
                            allConvoList.filter { it.first.message.lowercase().contains(query) }
                        convoAdapter.updateConvoList(searchResults)
                        true
                    }
                    return true
                }
            })

            /**
             * ----observers---------------------------------------------
             */

            alteViewModel.allUsersList.observe(viewLifecycleOwner) { users ->
                if (users != null) {
                    alteViewModel.folksPeeps.observe(viewLifecycleOwner) { uidListPair ->
                        if (uidListPair != null) {
                            val updatePeepsList =
                                mutableListOf<Triple<UserDetails, List<Peep>, String>>()

                            uidListPair.forEach { pair ->
                                val folkUid = pair.first
                                val folkDetails = users.find { it.uid == folkUid }

                                val peepList = pair.second

                                if (folkDetails != null && peepList.isNotEmpty()) {
                                    val currentUsername = alteViewModel.currentUser.value?.username
                                    if (currentUsername != null) {
                                        val peepTriple =
                                            Triple(folkDetails, pair.second, currentUsername)

                                        updatePeepsList.add(peepTriple)
                                    }
                                }
                            }

                            updatePeepsList.find { it.first.uid == currentUser?.uid }
                                ?.let { foundPair ->
                                    // Remove your peep from its current position
                                    updatePeepsList.remove(foundPair)

                                    // Add it to the top of the list
                                    updatePeepsList.add(0, foundPair)
                                }

                            peepAdapter.updatePeeps(updatePeepsList)
                        }
                    }

                }
            }

            alteViewModel.convoList.observe(viewLifecycleOwner) { latestConvo ->
                val currentUsername = alteViewModel.currentUser.value?.username
                if (currentUsername != null) {
                    welcomeTV.text = getString(R.string.welcome_message, currentUsername)
                }

                if (latestConvo != null) {
                    if (latestConvo.isEmpty()) {
                        emptyConvoLayout.visibility = View.VISIBLE
                    } else {
                        emptyConvoLayout.visibility = View.GONE
                        convoAdapter.updateConvoList(latestConvo.reversed())
                        allConvoList = latestConvo
                    }
                }
                lottieAnimationView.visibility = View.GONE
            }

            alteViewModel.selectedConvoItems.observe(viewLifecycleOwner) { convoList ->
                if (convoList != null) {
                    selectedConvoList = convoList

                    isInSelectionMode = if (convoList.isNotEmpty()) {
                        chatSelectedLayout.visibility = View.VISIBLE
                        true
                    } else {
                        chatSelectedLayout.visibility = View.GONE
                        false
                    }

                    callback.isEnabled = isInSelectionMode

                    isFilledList = convoList.size == allConvoList.size

                    selectionCounter.text = selectedConvoList.size.toString()
                }
            }

            //Set up back pressed call back
            callback =
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                    if (isInSelectionMode) {
                        leaveSelectionMode()
                    } else if (isCropImageLayoutVisible) {
                        cropImageLayout.visibility = View.GONE
                        isCropImageLayoutVisible = false
                    }
                    callback.isEnabled = false
                }

            // Disable the callback by default
            callback.isEnabled = false

        }
    }

    private fun navigateToChatFragment() {
        findNavController().navigate(R.id.action_messagesFragment_to_chatFragment)
    }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {

                binding.apply {
                    cropImageView.setImageUriAsync(uri)
                    cropImageView.setAspectRatio(1, 1)

                    // show the cropping layout
                    cropImageLayout.visibility = View.VISIBLE
                    isCropImageLayoutVisible = true
                    callback.isEnabled = isCropImageLayoutVisible

                    doneCrop.setOnClickListener {

                        // Show the loading animation
                        myPeepProgressBar.visibility = View.VISIBLE

                        cropImageLayout.visibility = View.GONE
                        isCropImageLayoutVisible = false

                        val croppedBitmap = cropImageView.getCroppedImage()
                        val compressedBitmap =
                            croppedBitmap?.let { it1 -> imageHandler.compressBitmap(it1) }

                        // Upload the Bitmap
                        if (currentUser != null && compressedBitmap != null) {
                            val currentTime = dateTimeManager.getCurrentTimeFormatted()
                            val peepCaption = captionCrop.text.toString().trim()
                            captionCrop.text.clear()

                            alteViewModel.uploadPeep(
                                currentUser!!.uid,
                                currentTime,
                                compressedBitmap,
                                peepCaption,
                                requireActivity().applicationContext
                            ) { uploadSuccessful ->
                                if (uploadSuccessful) {
                                    toastManager.showShortToast(
                                        requireContext(),
                                        "Peep uploaded successfully"
                                    )
                                } else {
                                    toastManager.showShortToast(
                                        requireContext(),
                                        "Peep upload failed"
                                    )
                                }
                                myPeepProgressBar.visibility = View.GONE
                            }
                        }

                    }

                    cancelCrop.setOnClickListener {
                        cropImageLayout.visibility = View.GONE
                        captionCrop.text.clear()
                        isCropImageLayoutVisible = false
                        callback.isEnabled = isCropImageLayoutVisible
                    }

                    rotateCrop.setOnClickListener {
                        cropImageView.rotateImage(90)
                    }

                    binding.captionCrop.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
                        // Replace newline characters with an empty string
                        source?.toString()?.replace("\n", "") ?: ""
                    })

                    binding.captionCrop.setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            // Perform your custom action when the "Done" action is triggered
                            return@setOnEditorActionListener true
                        }
                        false
                    }

                }
            }
        }

    private fun updateLists() {
        alteViewModel.startDatabaseListeners(requireActivity().applicationContext)
        stopRefreshing()
    }

    private fun stopRefreshing() {
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun showOverflowMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)

        // Inflate the menu resource
        popupMenu.menuInflater.inflate(R.menu.messages_overflow_menu, popupMenu.menu)

        // Set an OnMenuItemClickListener for the menu items
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.search_messages -> {

                    if (allConvoList.isNotEmpty()) {
                        binding.searchView.visibility = View.VISIBLE

                        isSearchViewVisible = true
                    }
                    true
                }

                R.id.new_circle -> {
                    findNavController().navigate(R.id.action_messagesFragment_to_circlesFragment)
                    true
                }

                R.id.new_broadcast -> {
                    findNavController().navigate(R.id.action_messagesFragment_to_broadcastFragment)
                    true
                }

                R.id.starred_messages -> {
                    findNavController().navigate(R.id.action_messagesFragment_to_starredMessagesFragment)
                    true
                }

                R.id.profile_messages -> {
                    findNavController().navigate(R.id.action_messagesFragment_to_profileFragment)
                    true
                }

                R.id.settings -> {
                    findNavController().navigate(R.id.action_messagesFragment_to_settingsFragment)
                    true
                }

                R.id.logout -> {

                    // Show confirm dialog
                    dialogManager.showLogoutConfirmationDialog(
                        this,
                        alteViewModel
                    ) { isSuccessful ->
                        if (isSuccessful) {
                            // Navigate to SignInFragment
                            findNavController().navigate(R.id.action_messagesFragment_to_signInFragment)
                        }
                    }
                    true
                }

                else -> {
                    false
                }
            }
        }

        // Show the popup menu
        popupMenu.show()
    }

    private fun leaveSelectionMode() {
        binding.apply {
            convoAdapter.clearSelection()

            chatSelectedLayout.visibility = View.GONE
            isInSelectionMode = false

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the state of flags
        outState.putBoolean("isCropImageLayoutVisible", isCropImageLayoutVisible)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Restore the state of flags
        isCropImageLayoutVisible =
            savedInstanceState?.getBoolean("isCropImageLayoutVisible") ?: false

        binding.apply {
            // Reset cropImage layout visibility
            if (isCropImageLayoutVisible) {
                cropImageLayout.visibility = View.VISIBLE
            }
        }
    }

}