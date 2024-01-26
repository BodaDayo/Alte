package com.rgbstudios.alte.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rgbstudios.alte.data.model.CurrentUserDetails
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.model.UserUploadBundle
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.utils.SharedPreferencesManager
import kotlinx.coroutines.launch

class AlteViewModel(application: Application, private val alteRepository: AlteRepository) :
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
    private val _currentUser = MutableLiveData<CurrentUserDetails>()
    val currentUser: LiveData<CurrentUserDetails> = _currentUser

    // LiveData to hold the slider status
    private val _closeSlider = MutableLiveData<Boolean>()
    val closeSlider: LiveData<Boolean> = _closeSlider

    // LiveData to hold the connections layout item opener
    private val _connectionsItemSelected = MutableLiveData<Int>()
    val connectionsItemSelected: LiveData<Int> = _connectionsItemSelected

    // LiveData to hold allTasksList
    private val _allUsersList = MutableLiveData<List<UserDetails>>()
    val allUsersList: LiveData<List<UserDetails>> = _allUsersList

    init {
        _isFirstLaunch.value = getIsFirstLaunch()
        _isUsernameSet.value = getIsUsernameSet()
        startDatabaseListeners()
    }

    private fun startDatabaseListeners() {
        startUserListListener()
        // Get the user ID from Firebase Auth
        val uid = firebase.auth.currentUser?.uid ?: ""
        startCurrentUserListener(uid)
    }

    fun startCurrentUserListener(uid: String) {
        viewModelScope.launch {
            alteRepository.getCurrentUserFromDatabase(uid) { userDetails ->
                _currentUser.postValue(userDetails)
            }
        }
    }

    private fun startUserListListener() {
        viewModelScope.launch {
            alteRepository.getUserListFromDatabase { newList ->
                if (newList.isNotEmpty()) {
                    val currentUser = firebase.currentUser
                    if (currentUser != null) {
                        val allUsersList = newList.filterNot { currentUser.uid == it.uid }
                        _allUsersList.postValue(allUsersList)
                    }
                }
            }
        }
    }

    private fun getIsFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean("isFirstLaunch", true)
    }

    private fun getIsUsernameSet(): Boolean {
        return sharedPreferences.getBoolean("isUsernameSet", false)
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

    fun logOut(callback: (Boolean, String?) -> Unit) {

        firebase.logOut { logOutSuccessful, errorMessage ->
            if (logOutSuccessful) {
                firebase.addLog("sign out successful")


                callback(true, null)
            } else {
                firebase.addLog("sign out failed")
                callback(false, errorMessage)
            }
        }
    }

    fun sendConnectRequest(senderId: String, receiverId: String, callback: (Boolean) -> Unit) {
        alteRepository.addUserToInviteList(senderId, receiverId) {
            callback(it)
        }
    }

    fun acceptRequest(receiverId: String, senderId: String, callback: (Boolean) -> Unit) {
        alteRepository.acceptConnectionRequest(receiverId, senderId) {
            callback(it)
        }
    }
    fun declineRequest(receiverId: String, senderId: String, callback: (Boolean) -> Unit) {
        alteRepository.cancelConnectionRequest(receiverId, senderId) {
            callback(it)
        }
    }

    fun withdrawRequest(receiverId: String, senderId: String, callback: (Boolean) -> Unit) {
        alteRepository.cancelConnectionRequest(receiverId, senderId) {
            callback(it)
        }
    }

    fun toggleSlider(toClose: Boolean) {
        _closeSlider.value = toClose
    }

    fun setConnectionsItem(item: Int) {
        _connectionsItemSelected.value = item
    }

}