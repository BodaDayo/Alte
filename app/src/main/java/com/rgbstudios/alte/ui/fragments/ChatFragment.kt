package com.rgbstudios.alte.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.internal.ViewUtils.hideKeyboard
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.Chat
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentChatBinding
import com.rgbstudios.alte.ui.adapters.ChatAdapter
import com.rgbstudios.alte.utils.DateTimeManager
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ImageHandler
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class ChatFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

    private val auth = firebase.auth
    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private val imageHandler = ImageHandler()
    private val dateTimeManager = DateTimeManager()
    private lateinit var binding: FragmentChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private var currentRecipient: UserDetails? = null

    private var isMessageTyped = false
    private var isInSelectionMode = false
    private var toEdit = false
    private var selectedChats: List<Chat> = emptyList()
    private var starredList: List<String> = emptyList()
    private var isCropImageLayoutVisible = false
    private var senderId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

            // Observe the recipient
            alteViewModel.currentRecipient.observe(viewLifecycleOwner) { user ->
                if (user != null) {
                    currentRecipient = user
                    receiverUsernameTV.text = user.username
                    receiverStatusTV.text = user.status

                    Glide.with(requireContext())
                        .asBitmap()
                        .load(user.avatarUri)
                        .placeholder(R.drawable.user_icon)
                        .listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Bitmap>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                // Handle load failure if needed
                                receiverAVProgressBar.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Bitmap?,
                                model: Any?,
                                target: Target<Bitmap>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                // Handle resource ready (image loaded successfully)
                                receiverAVProgressBar.visibility = View.GONE
                                return false
                            }
                        })
                        .into(receiverAvatar)
                }
            }

            senderId = firebase.auth.currentUser?.uid ?: ""

            // Set up the defaultAvatarRecyclerView
            chatAdapter = ChatAdapter(
                requireContext(),
                senderId,
                alteViewModel,
                object : ChatAdapter.ChatClickListener {
                    override fun onReplyClick(position: Int) {
                        chatRecyclerView.scrollToPosition(position)
                    }

                    override fun onImageClick(imageUri: String) {
                        Glide.with(requireContext())
                            .asBitmap()
                            .load(imageUri)
                            .into(expandedImageView)

                        expandedImageLayout.visibility = View.VISIBLE
                    }
                }
            )

            chatRecyclerView.layoutManager = LinearLayoutManager(context)
            chatRecyclerView.adapter = chatAdapter

            closeExpandedImage.setOnClickListener {
                expandedImageLayout.visibility = View.GONE
            }

            downloadExpandedImage.setOnClickListener {
                // Download image
            }

            moreOptions.setOnClickListener {
                showOverflowMenu(it)
            }

            recipientLayout.setOnClickListener {
                currentRecipient?.let {
                    alteViewModel.setSelectedCitizen(it)
                    findNavController().navigate(R.id.action_chatFragment_to_citizenProfileFragment)
                }
            }

            messageEt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Not needed for this case
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Check if the text is empty
                    isMessageTyped = if (s.isNullOrEmpty()) {
                        // Set microphone icon when the text is empty
                        sendMessageVoiceType.setImageResource(R.drawable.mic)
                        false
                    } else {
                        // Set send icon when there is text
                        sendMessageVoiceType.setImageResource(R.drawable.send_filled)
                        true
                    }

                    currentRecipient?.let { alteViewModel.updateTypingStatus(it.uid) }
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s.isNullOrEmpty()) {
                        alteViewModel.updateTypingStatus("")
                    }
                }
            })

            sendMessageVoiceType.setOnClickListener {
                if (isMessageTyped) {
                    if (senderId.isNotEmpty() && currentRecipient != null) {

                        val message = messageEt.text.toString().trim()
                        messageEt.text.clear()
                        isMessageTyped = false

                        sendingAnimationView.visibility = View.VISIBLE

                        val currentTime = dateTimeManager.getCurrentTimeFormatted()

                        if (toEdit) {
                            editChat(message, currentTime)
                        } else {
                            sendChat(message, currentTime, "")
                        }

                    }
                }
            }

            addAttachment.setOnClickListener {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            replyMessageIV.setOnClickListener {
                if (selectedChats.isNotEmpty()) {
                    recipientTitleTV.text = selectedChats.first().senderId.let {
                        if (it == currentRecipient?.uid) {
                            currentRecipient?.username ?: getString(R.string.citizen)
                        } else {
                            getString(R.string.you)
                        }
                    }

                    recipientMessage.text = selectedChats.first().message.let {
                        if (it.length > 52) {
                            // If the caption is longer than 52 characters, truncate it and add ellipsis
                            it.substring(0, 49) + "..."
                        } else {
                            it
                        }
                    }

                    recipientMessageLayout.visibility = View.VISIBLE

                    recipientMessageLayout.setOnClickListener {
                        leaveSelectionMode()
                    }
                }
            }

            starMessageIV.setOnClickListener {
                selectedChats.forEach { chat ->
                    val node = chat.timeStamp + chat.senderId
                    val toAdd = !starredList.contains(node)

                    alteViewModel.updateChatStarredStatus(
                        toAdd,
                        senderId,
                        chat,
                    ) { success ->
                        if (success) {
                            leaveSelectionMode()
                        } else {
                            toastManager.showShortToast(
                                requireContext(),
                                "That was unsuccessful, check your network and try again!"
                            )
                        }
                    }

                }
            }

            forwardMessageIV.setOnClickListener {
                // Show dialog with recyclerview of folks to send message to
            }

            copyMessageIV.setOnClickListener {
                if (selectedChats.isNotEmpty()) {
                    val clipboardManager =
                        view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                    val message = if (selectedChats.size > 1) {
                        buildString {
                            selectedChats.forEach {
                                val senderUsername =
                                    alteViewModel.allUsersList.value?.find { user -> user.uid == it.senderId }?.username

                                senderUsername?.let { username ->
                                    append("$username: ${it.timeStamp} >\n")
                                    append("${it.message}\n\n")
                                }
                            }
                        }
                    } else {
                        selectedChats.first().message
                    }

                    val clipData = ClipData.newPlainText("Message", message)

                    clipboardManager.setPrimaryClip(clipData)

                    if (clipboardManager.hasPrimaryClip()) {
                        toastManager.showShortToast(requireContext(), "Message copied to clipboard")
                        leaveSelectionMode()
                    } else {
                        toastManager.showShortToast(requireContext(), "Copy to clipboard failed")
                    }
                }
            }

            deleteMessageIV.setOnClickListener {
                if (selectedChats.isNotEmpty()){
                    dialogManager.showDeleteConfirmationDialog(
                        selectedChats,
                        starredList,
                        this@ChatFragment,
                        alteViewModel
                    ) {
                        when (it) {
                            1 -> {
                                deleteProgressBar.visibility = View.VISIBLE
                            }
                            2 -> {
                                toastManager.showShortToast(requireContext(), "Deleted successfully")
                                deleteProgressBar.visibility = View.GONE
                                leaveSelectionMode()
                            }
                            else -> {
                                toastManager.showShortToast(requireContext(), "Failed to delete, try again!")
                                deleteProgressBar.visibility = View.GONE
                            }
                        }
                    }
                }
            }

            messageEditIV.setOnClickListener {
                if (selectedChats.isNotEmpty()) {
                    val chat = selectedChats.first()

                    if (chat.senderId == senderId) {

                        recipientTitleTV.text = getString(R.string.you)

                        recipientMessage.text = chat.message.let {
                            if (it.length > 52) {
                                // If the caption is longer than 52 characters, truncate it and add ellipsis
                                it.substring(0, 49) + "..."
                            } else {
                                it
                            }
                        }

                        messageEt.setText(chat.message)

                        recipientMessageLayout.visibility = View.VISIBLE
                        toEdit = true

                        recipientMessageLayout.setOnClickListener {
                            leaveSelectionMode()
                        }
                    }
                }
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                popBackStackManager()
            }

            popBackCF.setOnClickListener {
                popBackStackManager()
            }

            // Observe currentChat
            alteViewModel.currentChat.observe(viewLifecycleOwner) { chatList ->

                chatAdapter.updateChatList(chatList, currentRecipient)
                sendingAnimationView.visibility = View.GONE

                // Scroll to the last item to display it initially
                chatRecyclerView.post {
                    chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }

                val unreadChat =
                    chatList.find { it.senderId == currentRecipient?.uid && !it.isRead }

                if (unreadChat != null) {

                    alteViewModel.updateChatDeliveryStatus(
                        currentUserId = senderId,
                        recipientId = currentRecipient!!.uid,
                        nodeToChange = 2,
                        deliveryBoolean = true,
                        starredList
                    )
                }

            }

            // Observe selected chats
            alteViewModel.selectedChatItems.observe(viewLifecycleOwner) { chats ->
                val isChatNotEmpty = !chats.isNullOrEmpty()
                isInSelectionMode = isChatNotEmpty
                chatSelectedLayout.visibility = if (isChatNotEmpty) View.VISIBLE else View.GONE

                if (isChatNotEmpty) {
                    selectedChats = chats

                    val enableActions = chats.size == 1
                    replyMessageIV.isEnabled = enableActions
                    forwardMessageIV.isEnabled = enableActions

                    val iconColor =
                        if (enableActions) ContextCompat.getColor(requireContext(), R.color.black)
                        else ContextCompat.getColor(requireContext(), R.color.my_darker_grey)

                    replyMessageIV.setColorFilter(iconColor)
                    forwardMessageIV.setColorFilter(iconColor)

                    val isSender = chats.first().senderId == senderId
                    messageEditIV.isEnabled = isSender
                    messageEditIV.setColorFilter(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isSender) R.color.black else R.color.my_darker_grey
                        )
                    )
                }
            }

            // Observe currentUsers starred chats
            alteViewModel.currentUser.observe(viewLifecycleOwner) { sender ->
                if (sender != null) {
                    val starredChats = sender.starredMessages.mapNotNull { it.first }
                    chatAdapter.updateStarredList(starredChats)
                    starredList = starredChats
                }
            }

            // Observe the recipient typing status
            alteViewModel.recipientTypingStatus.observe(viewLifecycleOwner) { uid ->
                if (uid == senderId) {
                    typingStatusTV.visibility = View.VISIBLE
                    receiverStatusTV.visibility = View.GONE
                } else {
                    typingStatusTV.visibility = View.GONE
                    receiverStatusTV.visibility = View.VISIBLE
                }
            }


        }
    }

    private fun sendChat(
        message: String,
        timeStamp: String,
        imageUri: String
    ) {
        binding.apply {

            val recipientTimestamp =
                selectedChats.let { if (it.isNotEmpty()) it.first().timeStamp else "" }

            val chat = Chat(
                senderId = senderId,
                receiverId = currentRecipient!!.uid,
                message = message,
                timeStamp = timeStamp,
                recipientTimestamp = recipientTimestamp,
                imageUri = imageUri
            )

            sendMessageVoiceType.isEnabled = false

            alteViewModel.sendChat(chat) { messageSent ->
                if (!messageSent) {
                    toastManager.showShortToast(requireContext(), "Message not sent, try again!")
                    messageEt.setText(message)
                    isMessageTyped = true
                } else {
                    leaveSelectionMode()
                    sendingAnimationView.visibility = View.GONE
                }

                sendMessageVoiceType.isEnabled = true
            }
        }
    }

    private fun editChat(
        message: String,
        timeStamp: String
    ) {
        binding.apply {
            if (selectedChats.isNotEmpty()) {
                val chat = selectedChats.first().copy(message = message, editTimeStamp = timeStamp)

                sendMessageVoiceType.isEnabled = false

                val node = chat.timeStamp + chat.senderId
                val isStarred = starredList.contains(node)

                alteViewModel.updateChatEditStatus(isStarred, chat) { chatEdited ->
                    if (!chatEdited) {
                        toastManager.showShortToast(
                            requireContext(),
                            "Message edit failed, try again!"
                        )
                        messageEt.setText(message)
                        isMessageTyped = true
                    } else {
                        leaveSelectionMode()
                        sendingAnimationView.visibility = View.GONE
                    }

                    sendMessageVoiceType.isEnabled = true
                }
            }
        }
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

                    sendImage.setOnClickListener {

                        cropImageLayout.visibility = View.GONE
                        isCropImageLayoutVisible = false

                        val croppedBitmap = cropImageView.getCroppedImage()
                        val compressedBitmap =
                            croppedBitmap?.let { it1 -> imageHandler.compressBitmap(it1) }

                        // Set the attachedImageView and text
                        if (compressedBitmap != null && senderId.isNotEmpty() && currentRecipient != null) {

                            val currentTime = dateTimeManager.getCurrentTimeFormatted()
                            val message = captionCrop.text.toString().trim()
                            captionCrop.text.clear()

                            hideKeyboard()
                            sendingAnimationView.visibility = View.VISIBLE

                            alteViewModel.uploadChatImage(
                                senderId,
                                currentRecipient!!.uid,
                                compressedBitmap,
                                currentTime
                            ) { imageUri, errorMessage ->
                                if (imageUri != "") {
                                    sendChat(
                                        message,
                                        currentTime,
                                        imageUri
                                    )
                                } else {
                                    toastManager.showShortToast(requireContext(), errorMessage)
                                    sendingAnimationView.visibility = View.GONE
                                }
                            }

                        }
                    }

                    cancelCrop.setOnClickListener {
                        cropImageLayout.visibility = View.GONE
                        captionCrop.text.clear()
                        isCropImageLayoutVisible = false
                    }

                    rotateCrop.setOnClickListener {
                        cropImageView.rotateImage(90)
                    }
                }
            }
        }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.captionCrop.windowToken, 0)
    }

    private fun leaveSelectionMode() {
        binding.apply {
            chatAdapter.clearSelection()

            chatSelectedLayout.visibility = View.GONE
            isInSelectionMode = false

            recipientMessageLayout.visibility = View.GONE
            toEdit = false
        }
    }

    private fun showOverflowMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)

        // Inflate the menu resource
        popupMenu.menuInflater.inflate(R.menu.chat_overflow_menu, popupMenu.menu)

        // Set an OnMenuItemClickListener for the menu items
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {

                R.id.shared_media_chat -> {
                    true
                }

                R.id.search_chat -> {
                    true
                }

                R.id.report_chat -> {
                    true
                }

                R.id.exile_chat -> {
                    true
                }

                R.id.ditch_chat -> {
                    true
                }

                R.id.clear_chat -> {
                    true
                }

                else -> false
            }
        }

        // Show the popup menu
        popupMenu.show()
    }

    private fun popBackStackManager() {
        if (isCropImageLayoutVisible) {
            binding.cropImageLayout.visibility = View.GONE
            isCropImageLayoutVisible = false
        } else if (isInSelectionMode) {
            leaveSelectionMode()
        } else {
            // if message is typed, grab it and save in shared preferences to use later
            // If no changes, simply pop back stack
            alteViewModel.updateTypingStatus("")
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    override fun onPause() {
        super.onPause()
        // Update typing status to false when the fragment is not visible
        alteViewModel.updateTypingStatus("")

    }

}