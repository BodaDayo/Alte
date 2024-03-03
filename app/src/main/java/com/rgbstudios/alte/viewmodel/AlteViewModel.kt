package com.rgbstudios.alte.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.Chat
import com.rgbstudios.alte.data.model.CurrentUserDetails
import com.rgbstudios.alte.data.model.Peep
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.model.UserUploadBundle
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.utils.SharedPreferencesManager
import kotlinx.coroutines.launch

class AlteViewModel(application: AlteApplication, private val alteRepository: AlteRepository) :
    ViewModel() {

    private val firebase = FirebaseAccess()
    private val sharedPreferences = SharedPreferencesManager(application)

    // LiveData to hold the isFirstLaunch status
    private val _isFirstLaunch = MutableLiveData<Boolean>()
    val isFirstLaunch: LiveData<Boolean> = _isFirstLaunch

    // LiveData to hold the isUsernameSet status
    private val _isUsernameSet = MutableLiveData<Boolean>()
    val isUsernameSet: LiveData<Boolean> = _isUsernameSet

    // LiveData to hold current user
    private val _currentUser = MutableLiveData<CurrentUserDetails?>()
    val currentUser: LiveData<CurrentUserDetails?> = _currentUser

    // LiveData to hold convo list
    private val _convoList = MutableLiveData<List<Triple<Chat, String, Pair<Int, Int>>>>()
    val convoList: LiveData<List<Triple<Chat, String, Pair<Int, Int>>>> = _convoList

    // LiveData to hold the slider status
    private val _closeSlider = MutableLiveData<Boolean>()
    val closeSlider: LiveData<Boolean> = _closeSlider

    // LiveData to hold the connections layout item opener
    private val _connectionsItemSelected = MutableLiveData<Int?>()
    val connectionsItemSelected: LiveData<Int?> = _connectionsItemSelected

    // LiveData to hold the condition to sort the tasks
    private val _settingsItemSelected = MutableLiveData<String?>()
    val settingsItemSelected: LiveData<String?> = _settingsItemSelected

    // LiveData to hold the peep
    private val _peepItemSelected = MutableLiveData<Pair<UserDetails, List<Peep>?>?>()
    val peepItemSelected: LiveData<Pair<UserDetails, List<Peep>?>?> = _peepItemSelected

    // LiveData to hold the peep
    private val _selectedConvoItems = MutableLiveData<MutableList<UserDetails>>()
    val selectedConvoItems: LiveData<MutableList<UserDetails>> = _selectedConvoItems

    // LiveData to hold the peep
    private val _selectedChatItems = MutableLiveData<MutableList<Chat>>()
    val selectedChatItems: LiveData<MutableList<Chat>> = _selectedChatItems

    // LiveData to hold the peep
    private val _selectedStarredItems = MutableLiveData<MutableList<Chat>>()
    val selectedStarredItems: LiveData<MutableList<Chat>> = _selectedStarredItems

    // LiveData to hold allTasksList
    private val _allUsersList = MutableLiveData<List<UserDetails>>()
    val allUsersList: LiveData<List<UserDetails>> = _allUsersList

    // LiveData to hold current chat
    private val _currentChat = MutableLiveData<List<Chat>>()
    val currentChat: LiveData<List<Chat>> = _currentChat

    // LiveData to hold current parties
    private val _currentRecipient = MutableLiveData<UserDetails?>()
    val currentRecipient: LiveData<UserDetails?> = _currentRecipient

    // LiveData to hold current user
    private val _recipientTypingStatus = MutableLiveData<String>()
    val recipientTypingStatus: LiveData<String> = _recipientTypingStatus

    // LiveData to hold folksPeep
    private val _folksPeeps = MutableLiveData<List<Pair<String, List<Peep>>>>()
    val folksPeeps: LiveData<List<Pair<String, List<Peep>>>> = _folksPeeps

    // LiveData to hold circles selection
    private val _selectionList = MutableLiveData<List<UserDetails>>()
    val selectionList: LiveData<List<UserDetails>> = _selectionList

    // LiveData to hold circles selection
    private val _selectedCitizen = MutableLiveData<UserDetails?>()
    val selectedCitizen: LiveData<UserDetails?> = _selectedCitizen

    init {
        _isFirstLaunch.value = getIsFirstLaunch()
        _isUsernameSet.value = getIsUsernameSet()
        startDatabaseListeners(application.applicationContext)
        _selectedStarredItems.value = mutableListOf()
        _selectedConvoItems.value = mutableListOf()
        _selectedChatItems.value = mutableListOf()
    }

    /**
     * ----Listeners---------------------------------------------------------------------------
     */

    fun startDatabaseListeners(context: Context) {
        startUserListListener()
        // Get the user ID from Firebase Auth
        val uid = firebase.auth.currentUser?.uid ?: ""
        startCurrentUserListener(uid, context)
        startPeepListener(uid, context)
        startMessagesListener(uid)
    }

    private fun startCurrentUserListener(uid: String, context: Context) {
        viewModelScope.launch {
            alteRepository.observeCurrentUserDetails(uid) { userDetails ->
                _currentUser.postValue(userDetails)
                if (userDetails != null) {
                    updateUserStatus(userDetails.uid, context.getString(R.string.online), context)
                }

                // Ensure connection requests are completed
                val inviteeList = userDetails?.invites
                if (inviteeList != null) {
                    alteRepository.completeInvitation(uid, inviteeList)
                }
            }
        }
    }

    fun startUserListListener() {
        viewModelScope.launch {
            alteRepository.observeAllUserList { newList ->
                if (newList.isNotEmpty()) {
                    _allUsersList.postValue(newList)
                }
            }
        }
    }

    private fun startPeepListener(uid: String, context: Context) {
        if (uid != "") {
            viewModelScope.launch {
                val folksList = _currentUser.value?.folks ?: emptyList()
                val folksPeepList = folksList.toMutableList()
                folksPeepList.add(uid)

                alteRepository.observePeeps(folksPeepList) { peepPairList ->

                    if (!peepPairList.isNullOrEmpty()) {

                        // Set up cleanup of current user's expired peeps
                        val currentUserPeeps = peepPairList.find { it.first == uid }
                        if (currentUserPeeps != null) {
                            val peepList = currentUserPeeps.second

                            setUpPeepCleanUp(uid, peepList, context)
                        }

                        _folksPeeps.postValue(peepPairList)
                    }
                }
            }
        }

    }

    fun startMessagesListener(uid: String) {
        viewModelScope.launch {
            alteRepository.observeCurrentUserChats(uid) { latestChatsList ->
                _convoList.postValue(latestChatsList)

                if (latestChatsList != null) {

                    for (chatTriple in latestChatsList) {
                        val recipientId = chatTriple.second
                        val counters = chatTriple.third
                        val isUndeliveredPresent = counters.first > 0

                        if (isUndeliveredPresent){

                            currentUser.value?.starredMessages?.let { starredMessages ->
                                updateChatDeliveryStatus(
                                    currentUserId = uid,
                                    recipientId = recipientId,
                                    nodeToChange = 1,
                                    deliveryBoolean = true,
                                    starredMessages.mapNotNull { it.first }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun startChatListener(senderId: String, receiver: UserDetails) {
        _currentRecipient.value = receiver

        viewModelScope.launch {
            alteRepository.observeChatData(senderId, receiver.uid) { chat ->
                _currentChat.postValue(chat)
            }

            alteRepository.observeRecipientTyping(receiver.uid) { uid ->
                _recipientTypingStatus.postValue(uid)
            }
        }
    }

    private fun getIsFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean("isFirstLaunch", true)
    }

    private fun getIsUsernameSet(): Boolean {
        return sharedPreferences.getBoolean("isUsernameSet", false)
    }

    /**
     * ----Creation---------------------------------------------------------------------------
     */

    fun saveUserDetails(
        bundle: UserUploadBundle,
        callback: (Boolean) -> Unit
    ) {
        alteRepository.saveUserDetailsToDatabase(
            bundle
        ) {
            callback(it)
        }
    }

    fun sendChat(chat: Chat, callback: (Boolean) -> Unit) {
        alteRepository.sendMessage(chat, callback)
    }

    fun uploadChatImage(
        senderId: String,
        receiverId: String,
        attachedBitMap: Bitmap,
        timeStamp: String,
        callback: (String, String?) -> Unit
    ) {
        alteRepository.uploadChatImage(senderId, receiverId, attachedBitMap, timeStamp, callback)
    }

    fun uploadAvatar(avatarBitmap: Bitmap, callback: (Boolean) -> Unit) {
        alteRepository.updateUserAvatar(avatarBitmap, callback)
    }

    fun uploadPeep(
        uid: String,
        timeStamp: String,
        peep: Bitmap,
        peepCaption: String,
        context: Context,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            alteRepository.uploadUserPeep(uid, timeStamp, peep, peepCaption) { isSuccessful ->
                if (isSuccessful) {
                    startPeepListener(uid, context)
                }

                callback(isSuccessful)
            }
        }
    }

    /**
     * ----Updates---------------------------------------------------------------------------
     */

    fun updateUserDetails(
        about: String,
        dob: String?,
        gender: String?,
        location: String,
        name: String,
        callback: (Boolean, String?) -> Unit
    ) {
        alteRepository.updateUserDetails(about, dob, gender, location, name, callback)
    }

    fun updateChatStarredStatus(
        toAdd: Boolean,
        currentUserId: String,
        chatToReply: Chat,
        callback: (Boolean) -> Unit
    ) {
        alteRepository.updateStarredStatus(
            toAdd,
            currentUserId,
            chatToReply,
            callback
        )
    }

    fun updateChatEditStatus(
        isStarred: Boolean,
        chat: Chat,
        callback: (Boolean) -> Unit
    ) {
        alteRepository.updateChatEditStatus(
            isStarred,
            chat,
            callback
        )
    }

    fun updateChatDeliveryStatus(
        currentUserId: String,
        recipientId: String,
        nodeToChange: Int,
        deliveryBoolean: Boolean,
        starredList: List<String>
    ) {
        viewModelScope.launch {

            alteRepository.updateChatDeliveryStatus(
                currentUserId = currentUserId,
                recipientId = recipientId,
                nodeToChange = nodeToChange,
                deliveryBoolean = deliveryBoolean,
                starredList
            )
        }
    }

    // Function to update the isFirstLaunch status
    fun updateFirstLaunchStatus(isFirstLaunch: Boolean) {
        _isFirstLaunch.value = isFirstLaunch

        // Store the updated isFirstLaunch status in SharedPreferences
        sharedPreferences.putBoolean("isFirstLaunch", isFirstLaunch)
    }

    // Function to update the isUsernameSet status
    fun updateUsernameSetStatus(isUsernameSet: Boolean) {
        _isUsernameSet.value = isUsernameSet

        // Store the updated isUsernameSet status in SharedPreferences
        sharedPreferences.putBoolean("isUsernameSet", isUsernameSet)
    }

    fun updateTypingStatus(recipient: String) {
        viewModelScope.launch {
            val uid = currentUser.value?.uid
            if (uid != null) {
                alteRepository.updateTypingStatus(uid, recipient)
            }
        }
    }

    private fun updateUserStatus(uid: String, status: String, context: Context) {
        alteRepository.updateUserStatus(uid, status, context)
    }

    // Function to update the selection list
    fun updateSelectionList(toAdd: Boolean, user: UserDetails) {
        val currentList = _selectionList.value ?: emptyList()
        if (toAdd) {
            val theList = currentList.toMutableList()
            if (theList.contains(user)) {
                theList.remove(user)
            } else {
                theList.add(user)
            }
            _selectionList.value = theList
        } else {
            val theList = currentList.toMutableList()
            theList.remove(user)
            _selectionList.value = theList
        }
    }

    fun updateConvoSelection(user: UserDetails) {
        val selectedConvoList =
            _selectedConvoItems.value ?: emptyList<UserDetails>().toMutableList()

        if (selectedConvoList.contains(user)) {
            selectedConvoList.remove(user)
        } else {
            selectedConvoList.add(user)
        }

        _selectedConvoItems.value = selectedConvoList
    }

    fun updateChatSelection(chat: Chat) {
        val selectedChats = _selectedChatItems.value ?: emptyList<Chat>().toMutableList()

        if (selectedChats.contains(chat)) {
            selectedChats.remove(chat)
        } else {
            selectedChats.add(chat)
        }

        _selectedChatItems.value = selectedChats
    }

    fun updateStarredSelection(chat: Chat) {
        val selectedChats = _selectedStarredItems.value ?: emptyList<Chat>().toMutableList()

        if (selectedChats.contains(chat)) {
            selectedChats.remove(chat)
        } else {
            selectedChats.add(chat)
        }

        _selectedStarredItems.value = selectedChats
    }

    fun updatePeepViewed(peep: Peep) {
        val uid = currentUser.value?.uid ?: ""
        alteRepository.updatePeepViewed(peep, uid)
    }

    /**
     * ----Deletion----------------------------------------------------------------------------------
     */

    fun deleteMessage(chat: Chat, isStarred: Boolean, callback: (Boolean) -> Unit) {
        alteRepository.deleteMessage(chat, isStarred, callback)
    }

    private fun setUpPeepCleanUp(uid: String, list: List<Peep>, context: Context) {
        alteRepository.startPeepCleanup(uid, list, context)
    }

    fun deletePeep(uid: String, peep: Peep, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            alteRepository.deleteUserPeep(uid, peep.timeStamp, callback)
        }
    }

    /**
     * ----Connections---------------------------------------------------------------------------
     */

    fun sendConnectRequest(
        senderId: String,
        receiverId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        alteRepository.inviteUser(senderId, receiverId, callback)
    }

    fun acceptRequest(receiverId: String, senderId: String, callback: (Boolean, String?) -> Unit) {
        alteRepository.handleConnectionRequest(true, receiverId, senderId, callback)
    }

    fun declineRequest(receiverId: String, senderId: String, callback: (Boolean, String?) -> Unit) {
        alteRepository.handleConnectionRequest(false, receiverId, senderId, callback)
    }

    fun withdrawRequest(
        receiverId: String,
        senderId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        alteRepository.handleConnectionRequest(false, receiverId, senderId, callback)
    }

    fun exileCitizen(user: UserDetails) {
        TODO("Not yet implemented")
    }

    fun ditchFolk(user: UserDetails) {
        TODO("Not yet implemented")
    }


    /**
     * ----Auth----------------------------------------------------------------------------------
     */

    fun signIn(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        alteRepository.signIn(email, pass, callback)
    }
    fun signUp(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        alteRepository.signUp(email, pass, callback)
    }

    fun logOut(context: Context, callback: (Boolean, String?) -> Unit) {
        val uid = currentUser.value?.uid
        if (uid != null) {
            updateUserStatus(uid, context.getString(R.string.offline), context)

            viewModelScope.launch {
                alteRepository.logOut { logOutSuccessful, errorMessage ->
                    if (logOutSuccessful) {
                        resetLists()

                        callback(true, null)
                    } else {

                        updateUserStatus(uid, context.getString(R.string.online), context)
                        firebase.addLog("sign out failed")
                        callback(false, errorMessage)
                    }
                }
            }
        }
    }

    /**
     * ----Utils----------------------------------------------------------------------------------
     */

    fun toggleSlider(toClose: Boolean) {
        _closeSlider.value = toClose
    }

    fun setConnectionsItem(item: Int) {
        _connectionsItemSelected.value = item
    }

    fun setSettingsItem(item: String) {
        _settingsItemSelected.value = item
    }

    fun setPeepItem(userPeepPair: Pair<UserDetails, List<Peep>>) {
        _peepItemSelected.value = userPeepPair
    }

    fun setSelectedCitizen(user: UserDetails) {
        _selectedCitizen.value = user
    }

    fun clearConvoSelection() {
        _selectedConvoItems.value?.clear()
    }

    fun clearChatSelection() {
        _selectedChatItems.value?.clear()
    }
    fun clearStarredSelection() {
        _selectedStarredItems.value?.clear()
    }

    fun clearSelectionList() {
        _selectionList.value = emptyList()
    }

    private fun resetLists() {
        toggleSlider(true)
        clearSelectionList()
        _convoList.value = emptyList()
        _connectionsItemSelected.value = null
        _settingsItemSelected.value = null
        _peepItemSelected.value = null
        _allUsersList.value = emptyList()
        _currentChat.value = emptyList()
        _currentRecipient.value = null
        _recipientTypingStatus.value = ""
        _folksPeeps.value = emptyList()
        _selectedCitizen.value = null
        _currentUser.value = null
    }

    /**
     * ----System----------------------------------------------------------------------------------
     */

    override fun onCleared() {
        super.onCleared()
        val context = AlteApplication.applicationContext()

        val uid = currentUser.value?.uid
        if (uid != null) {
            updateUserStatus(uid, context.getString(R.string.offline), context)
        }
    }

}