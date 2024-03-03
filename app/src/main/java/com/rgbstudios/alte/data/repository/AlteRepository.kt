package com.rgbstudios.alte.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.Chat
import com.rgbstudios.alte.data.model.CurrentUserDetails
import com.rgbstudios.alte.data.model.Peep
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.model.UserUploadBundle
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.utils.DateTimeManager
import com.rgbstudios.alte.worker.PeepCleanUpWorker
import com.rgbstudios.alte.worker.StatusUpdateWorker
import java.io.ByteArrayOutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class AlteRepository(private val firebase: FirebaseAccess) {

    private val dateTimeManager = DateTimeManager()

    fun signIn(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        firebase.signIn(email, pass, callback)
    }

    fun signUp(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        firebase.signUp(email, pass, callback)
    }

    fun logOut(callback: (Boolean, String?) -> Unit) {
        firebase.logOut(callback)
    }

    fun changePassword(newPassword: String, callback: (Boolean, String?) -> Unit) {
        firebase.changePassword(newPassword, callback)
    }

    fun deleteAccountAndData(callback: (Boolean, String?) -> Unit) {
        firebase.deleteAccountAndData(callback)
    }

    /**
     * ----Creation---------------------------------------------------------------------------
     */

    fun saveUserDetailsToDatabase(
        bundle: UserUploadBundle,
        callback: (Boolean) -> Unit
    ) {
        val avatarStorageRef = firebase.getAvatarStorageRef(bundle.uid)

        try {
            if (bundle.avatar != null) {
                val avatarByteArray = convertBitmapToByteArray(bundle.avatar)

                avatarStorageRef.putBytes(avatarByteArray)
                    .addOnSuccessListener { taskSnapshot ->

                        taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                            val avatarUri: String = uri.toString()
                            uploadUserDetails(bundle, avatarUri) {
                                callback(it)
                            }
                        }
                            .addOnFailureListener { e ->
                                // Avatar image upload failed
                                callback(false)
                                firebase.recordCaughtException(e)
                            }
                    }
            } else {
                uploadUserDetails(bundle, "") {
                    callback(it)
                }
            }
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }

    }

    private fun uploadUserDetails(
        bundle: UserUploadBundle,
        avatarUri: String,
        callback: (Boolean) -> Unit
    ) {
        val usersDetailsRef = firebase.getUserListRef()

        val hashMap: HashMap<String, String> = HashMap()
        hashMap["name"] = bundle.name
        hashMap["username"] = bundle.username
        hashMap["gender"] = bundle.gender ?: ""
        hashMap["about"] = bundle.about ?: ""
        hashMap["dob"] = bundle.dob ?: ""
        hashMap["avatar"] = avatarUri
        hashMap["status"] = bundle.status
        hashMap["location"] = bundle.location ?: ""

        try {
            usersDetailsRef.child(bundle.uid).setValue(hashMap)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback(true)
                    } else {
                        callback(false)
                    }
                }
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            throw e
        }
    }

    fun sendMessage(chat: Chat, callback: (Boolean) -> Unit) {

        val senderChatRef =
            firebase.getChatListRef(chat.senderId).child(chat.receiverId).child(chat.timeStamp)
        val receiverChatRef =
            firebase.getChatListRef(chat.receiverId).child(chat.senderId).child(chat.timeStamp)

        try {

            val chatData = hashMapOf(
                "senderId" to chat.senderId,
                "receiverId" to chat.receiverId,
                "message" to chat.message,
                "timeStamp" to chat.timeStamp,
                "recipientTimestamp" to chat.recipientTimestamp,
                "imageUri" to chat.imageUri,
                "editTimeStamp" to chat.editTimeStamp,
                "isDelivered" to chat.isDelivered,
                "isRead" to chat.isRead
            )

            senderChatRef.setValue(chatData)
                .addOnCompleteListener { senderTask ->
                    if (senderTask.isSuccessful) {
                        receiverChatRef.setValue(chatData)
                            .addOnCompleteListener { receiverTask ->
                                if (receiverTask.isSuccessful) {

                                    callback(true)
                                } else {

                                    senderChatRef.removeValue()
                                    callback(false)
                                }
                            }
                    } else {
                        callback(false)
                    }
                }

        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }
    }

    fun uploadChatImage(
        senderId: String,
        receiverId: String,
        attachedBitMap: Bitmap,
        timeStamp: String,
        callback: (String, String?) -> Unit
    ) {

        val senderStorageRef = firebase.getChatStorageRef(senderId).child(timeStamp)
        val receiverStorageRef = firebase.getChatStorageRef(receiverId).child(timeStamp)

        val imageByteArray = convertBitmapToByteArray(attachedBitMap)

        try {
            senderStorageRef.putBytes(imageByteArray)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        val imageUri: String = uri.toString()
                        receiverStorageRef.putBytes(imageByteArray)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    callback(imageUri, null)
                                } else {
                                    val errorMessage = "Failed to upload image."
                                }
                            }
                    }
                        .addOnFailureListener {
                            val errorMessage = "Failed to upload image."
                            firebase.recordCaughtException(it)
                        }
                }
                .addOnFailureListener {
                    val errorMessage = "Failed to upload image."
                    firebase.recordCaughtException(it)
                }
        } catch (e: Exception) {
            val errorMessage = "Failed to upload image."
            // Handle exceptions if needed
            firebase.recordCaughtException(e)
        }
    }

    fun uploadUserPeep(
        uid: String,
        timeStamp: String,
        peep: Bitmap,
        peepCaption: String,
        callback: (Boolean) -> Unit
    ) {
        val peepStorageRef = firebase.getPeepStorageRef(uid).child(timeStamp)
        val peepListRef = firebase.getPeepListRef().child(uid).child(timeStamp)

        val peepByteArray = convertBitmapToByteArray(peep)
        try {
            peepStorageRef.putBytes(peepByteArray)
                .addOnSuccessListener { taskSnapshot ->

                    taskSnapshot.storage.downloadUrl
                        .addOnSuccessListener { uri ->
                            val peepUri: String = uri.toString()

                            val newPeep = hashMapOf(
                                "timeStamp" to timeStamp,
                                "peepUri" to peepUri,
                                "caption" to peepCaption
                            )

                            peepListRef.setValue(newPeep)
                                .addOnCompleteListener { listTask ->
                                    if (listTask.isSuccessful) {
                                        callback(false)
                                    } else {
                                        callback(false)
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            // Avatar image upload failed
                            callback(false)
                            firebase.recordCaughtException(e)
                        }
                }
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }
    }

    /**
     * ----Listeners---------------------------------------------------------------------------
     */

    fun observeCurrentUserDetails(
        uid: String,
        callback: (CurrentUserDetails?) -> Unit
    ) {
        val userListRef = firebase.getUserListRef()
        try {
            userListRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val userSnapshot = snapshot.children.find { it.key == uid }

                    if (userSnapshot != null) {
                        val userDetails = mapSnapshotToUserDetails(userSnapshot)

                        // Get the lists
                        val folks = userSnapshot.child("folks")
                            .getValue(object : GenericTypeIndicator<List<String>>() {})
                            ?: emptyList()
                        val invites = userSnapshot.child("invites")
                            .getValue(object : GenericTypeIndicator<List<String>>() {})
                            ?: emptyList()
                        val requests = userSnapshot.child("requests")
                            .getValue(object : GenericTypeIndicator<List<String>>() {})
                            ?: emptyList()
                        val exile = userSnapshot.child("exile")
                            .getValue(object : GenericTypeIndicator<List<String>>() {})
                            ?: emptyList()
                        val starredMessages = userSnapshot.child("starredMessages")
                            .children
                            .mapNotNull { Pair(it.key, it.getValue(Chat::class.java)) }

                        val currentUserDetails = CurrentUserDetails(
                            about = userDetails.about,
                            avatarUri = userDetails.avatarUri,
                            dob = userDetails.dob,
                            exile = exile,
                            folks = folks,
                            gender = userDetails.gender,
                            invites = invites,
                            location = userDetails.location,
                            name = userDetails.name,
                            typing = userDetails.typing,
                            requests = requests,
                            starredMessages = starredMessages,
                            status = userDetails.status,
                            uid = userDetails.uid,
                            username = userDetails.username
                        )
                        callback(currentUserDetails)

                    } else {
                        callback(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Do Nothing
                }
            })
        } catch (e: Exception) {
            // Handle exceptions if needed
            firebase.recordCaughtException(e)
        }
    }

    fun observeCurrentUserChats(
        uid: String,
        callback: (List<Triple<Chat, String, Pair<Int, Int>>>?) -> Unit
    ) {
        val userChatListRef = firebase.getChatListRef(uid)

        try {
            userChatListRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val result = mutableListOf<Triple<Chat, String, Pair<Int, Int>>>()

                    for (childSnapshot in snapshot.children) {
                        if (childSnapshot.key != null) {
                            val receiverId = childSnapshot.key!!

                            val lastItemKey = childSnapshot.children.lastOrNull()?.key
                            lastItemKey?.let {
                                val lastItemValue =
                                    childSnapshot.child(it).getValue(Chat::class.java)

                                // Count the number of children with "isDelivered" set to false
                                val undeliveredCount = childSnapshot.children.count { grandChild ->
                                    val isDelivered = grandChild.child("isDelivered")
                                        .getValue(Boolean::class.java)
                                    val senderId = grandChild.child("senderId")
                                        .getValue(String::class.java)

                                    isDelivered == false && senderId != uid
                                }

                                val unReadCount = childSnapshot.children.count { grandChild ->
                                    val isRead = grandChild.child("isRead")
                                        .getValue(Boolean::class.java)
                                    val senderId = grandChild.child("senderId")
                                        .getValue(String::class.java)

                                    isRead == false && senderId != uid
                                }

                                if (lastItemValue != null) {
                                    result.add(
                                        Triple(
                                            lastItemValue,
                                            receiverId,
                                            Pair(undeliveredCount, unReadCount)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Pass the result list to your callback
                    callback(result)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })

        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(null)
        }
    }

    fun observeAllUserList(callback: (List<UserDetails>) -> Unit) {
        val userListRef = firebase.getUserListRef()
        try {
            // Create a list to store Chat objects
            val userList = mutableListOf<UserDetails>()

            userListRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Clear the existing chatList
                    userList.clear()

                    // Iterate through the dataSnapshot to retrieve user data
                    for (dataSnapShot in snapshot.children) {
                        if (dataSnapShot.key != null) {
                            val userDetail = mapSnapshotToUserDetails(dataSnapShot)
                            userList.add(userDetail)
                        }
                    }

                    callback(userList)
                }

                override fun onCancelled(error: DatabaseError) {
                    firebase.recordCaughtException(error.toException())
                    callback(emptyList())
                }
            })
        } catch (e: Exception) {
            // Handle exceptions if needed
            firebase.recordCaughtException(e)
            callback(emptyList())
        }
    }

    fun observePeeps(
        folksPeepList: List<String>,
        callback: (List<Pair<String, List<Peep>>>?) -> Unit
    ) {
        val peepListRef = firebase.getPeepListRef()

        try {
            peepListRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val result = mutableListOf<Pair<String, List<Peep>>>()

                    for (childSnapshot in snapshot.children) {
                        if (childSnapshot.key != null) {
                            val uid = childSnapshot.key!!

                            if (folksPeepList.contains(uid)) {
                                val peeps = childSnapshot.children.mapNotNull {
                                    it.getValue(Peep::class.java)
                                }

                                val filteredList = filterExpiredList(peeps)

                                result.add(Pair(uid, filteredList))
                            }
                        }
                    }

                    callback(result)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })

        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(null)
        }

    }

    fun observeChatData(senderId: String, receiverId: String, callback: (List<Chat>) -> Unit) {
        val chatRef = firebase.getChatListRef(senderId).child(receiverId)

        try {
            chatRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatList = mutableListOf<Chat>()

                    for (childSnapshot in snapshot.children) {
                        val chat = childSnapshot.getValue(Chat::class.java)
                        chat?.let {
                            chatList.add(it)
                        }
                    }

                    callback(chatList)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
        } catch (e: Exception) {
            // Handle exceptions if needed
            firebase.recordCaughtException(e)
            callback(emptyList())
        }
    }

    fun observeRecipientTyping(
        recipientId: String,
        onStatusChanged: (String) -> Unit
    ) {
        val databaseReference = firebase.getUserListRef().child(recipientId).child("typing")

        try {

            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val typing = snapshot.child("typing").value as? String ?: ""
                    onStatusChanged(typing)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error
                }
            })
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            onStatusChanged("")
        }
    }

    /**
     * ----Connections---------------------------------------------------------------------------
     */

    private inline fun DatabaseReference.updateList(
        childName: String,
        toAdd: Boolean,
        crossinline callback: (Boolean) -> Unit
    ) {
        this.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentList =
                    snapshot.getValue(object : GenericTypeIndicator<List<String>>() {})
                        ?: emptyList()
                val updatedList = currentList.toMutableList()

                if (toAdd && !updatedList.contains(childName)) {
                    updatedList.add(childName)
                } else if (!toAdd && updatedList.contains(childName)) {
                    updatedList.remove(childName)
                }

                this@updateList.setValue(updatedList).addOnCompleteListener {
                    callback(it.isSuccessful)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }

    fun inviteUser(
        senderId: String,
        receiverId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val senderDetailsRef = firebase.getUserListRef().child(senderId)
        val receiverDetailsRef = firebase.getUserListRef().child(receiverId)

        try {
            receiverDetailsRef.child("exile")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val receiversExileList =
                            snapshot.getValue(object : GenericTypeIndicator<List<String>>() {})
                                ?: emptyList()

                        // If the sender is not on the receiver's "exile" list
                        if (receiversExileList.contains(senderId)) {

                            callback(false, "Sorry you cannot connect with this citizen")
                        } else {
                            senderDetailsRef.updateList(receiverId, true) { inviteTaskSuccessful ->
                                if (inviteTaskSuccessful) {
                                    receiverDetailsRef.updateList(
                                        senderId,
                                        true
                                    ) { receiverTaskSuccessful ->
                                        if (receiverTaskSuccessful) {
                                            callback(true, null)
                                        } else {
                                            senderDetailsRef.updateList(receiverId, false) {
                                                callback(false, "Error sending invite!")
                                            }
                                        }
                                    }
                                } else {
                                    callback(false, "Error sending invite, try again")
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback(false, "Error sending invite, try again")
                    }

                })

        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false, "Error sending invite, try again")
        }
    }

    fun handleConnectionRequest(
        toAccept: Boolean,
        receiverId: String,
        senderId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val receiverDetailsRef = firebase.getUserListRef().child(receiverId)
        val senderDetailsRef = firebase.getUserListRef().child(senderId)

        try {
            if (toAccept) {
                acceptConnectionRequest(
                    senderDetailsRef,
                    receiverDetailsRef,
                    receiverId,
                    senderId,
                    callback
                )
            } else {
                removeConnectionRequest(
                    senderDetailsRef,
                    receiverDetailsRef,
                    receiverId,
                    senderId,
                    callback
                )
            }
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false, "Error accepting connection request, try again")
        }
    }

    private fun acceptConnectionRequest(
        senderDetailsRef: DatabaseReference,
        receiverDetailsRef: DatabaseReference,
        receiverId: String,
        senderId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        receiverDetailsRef.child("folks").updateList(senderId, true) { rFTaskSuccess ->
            if (rFTaskSuccess) {
                senderDetailsRef.child("folks").updateList(receiverId, true) { sFTaskSuccess ->
                    if (sFTaskSuccess) {
                        removeConnectionRequest(
                            senderDetailsRef,
                            receiverDetailsRef,
                            receiverId,
                            senderId,
                            callback
                        )
                    } else {
                        handleAcceptFailure(
                            senderDetailsRef,
                            receiverDetailsRef,
                            receiverId,
                            senderId,
                            callback
                        )
                    }
                }
            } else {
                callback(false, "Error accepting connection request, try again")
            }
        }
    }

    private fun removeConnectionRequest(
        senderDetailsRef: DatabaseReference,
        receiverDetailsRef: DatabaseReference,
        receiverId: String,
        senderId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        senderDetailsRef.child("invites").updateList(receiverId, false) { iTSuccess ->
            if (iTSuccess) {
                receiverDetailsRef.child("requests").updateList(senderId, false) { rTSuccess ->
                    if (rTSuccess) {
                        callback(true, null)
                    } else {
                        handleDiscardFailure(senderDetailsRef, receiverId, callback)
                    }
                }
            } else {
                callback(false, "Error discarding connection request!")
            }
        }
    }

    private fun handleAcceptFailure(
        senderDetailsRef: DatabaseReference,
        receiverDetailsRef: DatabaseReference,
        receiverId: String,
        senderId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        senderDetailsRef.child("folks").updateList(senderId, false) {
            receiverDetailsRef.child("folks").updateList(receiverId, false) {
                callback(false, "Error accepting connection request!")
            }
        }
    }

    private fun handleDiscardFailure(
        senderDetailsRef: DatabaseReference,
        receiverId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        senderDetailsRef.child("invites").updateList(receiverId, true) {
            callback(false, "Error discarding connection request!")
        }
    }

    fun completeInvitation(senderId: String, invites: List<String>) {

        val senderDetailsRef = firebase.getUserListRef().child(senderId)

        // If you're not in exile list and you're not in request list update
        invites.forEach { receiverId ->
            val receiverDetailsRef = firebase.getUserListRef().child(receiverId)

            receiverDetailsRef.updateList(senderId, true) { receiverTaskSuccessful ->
                if (!receiverTaskSuccessful) {
                    senderDetailsRef.updateList(receiverId, false) {
                        // Completed
                    }
                }
            }
        }
    }


    /**
     * ----Updates----------------------------------------------------------------------------------
     */

    fun updateStarredStatus(
        toAdd: Boolean,
        currentUserId: String,
        chatToReply: Chat,
        callback: (Boolean) -> Unit
    ) {
        val node = chatToReply.timeStamp + chatToReply.senderId
        val starredListRef =
            firebase.getUserListRef().child(currentUserId)
                .child("starredMessages").child(node)
        try {
            if (toAdd) {
                starredListRef.setValue(chatToReply).addOnCompleteListener {
                    callback(it.isSuccessful)
                }
            } else {
                starredListRef.removeValue().addOnCompleteListener {
                    callback(it.isSuccessful)
                }
            }
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }

    }

    fun updateChatEditStatus(
        isStarred: Boolean,
        chat: Chat,
        callback: (Boolean) -> Unit
    ) {
        try {
            sendMessage(chat) { successful ->
                if (successful) {
                    if (isStarred) {
                        val node = chat.timeStamp + chat.senderId
                        val starredListRef =
                            firebase.getUserListRef().child(chat.senderId)
                                .child("starredMessages").child(node)

                        starredListRef.setValue(chat).addOnCompleteListener {
                            callback(it.isSuccessful)
                        }

                    } else callback(true)
                } else {
                    callback(false)
                }
            }
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            // Handle exception
        }
    }

    fun updateChatDeliveryStatus(
        currentUserId: String,
        recipientId: String,
        nodeToChange: Int,
        deliveryBoolean: Boolean,
        starredList: List<String>
    ) {
        val senderDetailsRef = firebase.getChatListRef(currentUserId).child(recipientId)
        val receiverDetailsRef = firebase.getChatListRef(recipientId).child(currentUserId)

        try {
            val detailsRefs = listOf(senderDetailsRef, receiverDetailsRef)

            for (detailsRef in detailsRefs) {
                detailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (childSnapshot in snapshot.children) {
                            val chat = childSnapshot.getValue(Chat::class.java)

                            val senderId =
                                childSnapshot.child("senderId").getValue(String::class.java) ?: ""

                            val statusKey = if (nodeToChange == 1) "isDelivered" else "isRead"

                            // Update isDelivered if senderId matches recipientId
                            if (senderId == recipientId) {
                                childSnapshot.ref.child(statusKey).setValue(deliveryBoolean)
                                    .addOnCompleteListener {

                                        if (chat != null) {

                                            val node = chat.timeStamp + chat.senderId

                                            if (starredList.contains(node)) {
                                                val starredListRef =
                                                    firebase.getUserListRef().child(senderId)
                                                        .child("starredMessages").child(node)
                                                        .child(statusKey)

                                                starredListRef.setValue(deliveryBoolean)
                                            }
                                        }
                                    }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
            }
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            // Handle exception
        }
    }

    fun updateTypingStatus(
        uid: String,
        recipientId: String
    ) {
        try {
            firebase.getUserListRef().child(uid).child("typing").setValue(recipientId)

        } catch (e: Exception) {
            firebase.recordCaughtException(e)
        }
    }

    fun updateUserStatus(uid: String, status: String, context: Context) {
        try {
            val userStatus =
                status.let { if (it == context.getString(R.string.online)) it else "Last Seen ${dateTimeManager.getStatusTimeStamp()}" }

            if (networkIsLive(context)) {

                // If there's network set status
                firebase.getUserListRef().child(uid).child("status").setValue(userStatus)

            } else {
                val data = Data.Builder()
                    .putString("uid", uid)
                    .putString("status", userStatus)
                    .build()

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // Requires network connection
                    .build()


                val statusUpdateWorkRequest = OneTimeWorkRequestBuilder<StatusUpdateWorker>()
                    .setConstraints(constraints)
                    .setInputData(data)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "statusUpdateWorker",
                        ExistingWorkPolicy.REPLACE,
                        statusUpdateWorkRequest
                    )
            }
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
        }
    }

    fun updateUserAvatar(avatarBitmap: Bitmap, callback: (Boolean) -> Unit) {
        val uid = firebase.currentUser?.uid

        if (uid != null) {
            val avatarStorageRef = firebase.getAvatarStorageRef(uid)

            try {
                val avatarByteArray = convertBitmapToByteArray(avatarBitmap)

                avatarStorageRef.putBytes(avatarByteArray)
                    .addOnSuccessListener { taskSnapshot ->

                        taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                            val avatarUri: String = uri.toString()
                            updateUserDetail(uid, "avatar", avatarUri, callback)
                        }
                            .addOnFailureListener { e ->
                                // Avatar image upload failed
                                callback(false)
                                firebase.recordCaughtException(e)
                            }
                    }

            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                callback(false)
            }
        } else callback(false)
    }

    fun updateUserDetails(
        about: String,
        dob: String?,
        gender: String?,
        location: String,
        name: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val uid = firebase.currentUser?.uid

        if (uid != null) {
            val userDetails = listOf(
                "about" to about,
                "dob" to dob,
                "gender" to gender,
                "location" to location,
                "name" to name
            )

            val validDetails = userDetails.filter { it.second != null }

            if (validDetails.isNotEmpty()) {
                updateUserDetailsRecursively(uid, validDetails, 0, callback)
            } else {
                // No valid details to update, callback true
                callback(false, "Profile update not successful")
            }

        } else callback(false, "You are not logged in")
    }

    private fun updateUserDetail(
        uid: String,
        key: String,
        value: String,
        callback: (Boolean) -> Unit
    ) {
        val nodeRef = firebase.getUserListRef().child(uid).child(key)
        try {
            nodeRef.setValue(value).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }
    }

    private fun updateUserDetailsRecursively(
        uid: String,
        validDetails: List<Pair<String, String?>>,
        index: Int,
        callback: (Boolean, String?) -> Unit
    ) {
        if (index < validDetails.size) {
            val (key, value) = validDetails[index]
            updateUserDetail(uid, key, value.orEmpty()) { success ->
                if (success) {
                    // If the current update is successful, proceed with the next one
                    updateUserDetailsRecursively(uid, validDetails, index + 1, callback)
                } else {
                    // If any update fails, callback false
                    callback(false, "Something went wrong, try again")
                }
            }
        } else {
            // All updates are successful, callback true
            callback(true, null)
        }
    }

    fun updatePeepViewed(peep: Peep, uid: String) {
        val peepListRef =
            firebase.getPeepListRef().child(uid).child(peep.timeStamp).child("viewList")

        // Use a transaction to safely update the viewList
        peepListRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                // Get the current viewList
                val currentViewList =
                    currentData.getValue(object : GenericTypeIndicator<List<String>>() {})
                        ?.toMutableList() ?: mutableListOf()

                // Check if uid is not already in the viewList
                if (!currentViewList.contains(uid)) {
                    // Add uid to the viewList
                    currentViewList.add(uid)
                    // Set the updated viewList back to the database
                    currentData.value = currentViewList
                }

                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                // Do Nothing
            }

        })
    }


    /**
     * ----Deletion----------------------------------------------------------------------------------
     */

    fun deleteMessage(chat: Chat, isStarred: Boolean, callback: (Boolean) -> Unit) {

        val senderChatRef =
            firebase.getChatListRef(chat.senderId).child(chat.receiverId).child(chat.timeStamp)
        val receiverChatRef =
            firebase.getChatListRef(chat.receiverId).child(chat.senderId).child(chat.timeStamp)

        try {

            senderChatRef.removeValue()
                .addOnCompleteListener { senderTask ->
                    if (senderTask.isSuccessful) {
                        receiverChatRef.removeValue()
                            .addOnCompleteListener { receiverTask ->
                                if (receiverTask.isSuccessful) {
                                    if (isStarred) {
                                        val node = chat.timeStamp + chat.senderId
                                        val starredListRef =
                                            firebase.getUserListRef().child(chat.senderId)
                                                .child("starredMessages").child(node)

                                        starredListRef.removeValue().addOnCompleteListener {
                                            callback(it.isSuccessful)
                                        }
                                    } else {
                                        callback(true)
                                    }
                                } else {

                                    senderChatRef.removeValue()
                                    callback(false)
                                }
                            }
                    } else {
                        callback(false)
                    }
                }

        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }
    }

    fun deleteUserPeep(uid: String, timeStamp: String, callback: (Boolean) -> Unit) {
        val peepStorageRef = firebase.getPeepStorageRef(uid).child(timeStamp)
        val peepListRef = firebase.getPeepListRef().child(uid).child(timeStamp)

        try {
            peepStorageRef.delete().addOnCompleteListener { storageTask ->
                if (storageTask.isSuccessful) {

                    peepListRef.removeValue()
                        .addOnCompleteListener { listTask ->
                            callback(listTask.isSuccessful)
                        }
                } else {
                    callback(false)
                }
            }
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }
    }

    fun startPeepCleanup(uid: String, list: List<Peep>, context: Context) {
        for (peep in list) {
            val timeStamp = peep.timeStamp

            val delayMillis = calculateExpiration(timeStamp)

            val data = Data.Builder()
                .putString("uid", uid)
                .putString("timeStamp", timeStamp)
                .build()

            if (delayMillis <= 0) {
                deleteUserPeep(uid, timeStamp) {
                    // Do Nothing
                }
            } else {
                enqueuePeepCleanUpWork(data, timeStamp, context, delayMillis)
            }
        }
    }

    private fun enqueuePeepCleanUpWork(
        data: Data,
        timeStamp: String,
        context: Context,
        delayMillis: Long
    ) {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requires network connection
            .build()

        val uniqueWorkName = "peepCleanUpWorker_${timeStamp}"

        val peepCleanWorkRequest = OneTimeWorkRequestBuilder<PeepCleanUpWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                peepCleanWorkRequest
            )
    }

    /**
     * ----Utils----------------------------------------------------------------------------------
     */

    private fun mapSnapshotToUserDetails(
        snapshot: DataSnapshot
    ): UserDetails {

        try {

            val uid = snapshot.key!!
            val name = snapshot.child("name").value as? String ?: ""
            val username = snapshot.child("username").value as? String ?: ""
            val typing = snapshot.child("typing").value as? String ?: ""
            val gender = snapshot.child("gender").value as? String
            val about = snapshot.child("about").value as? String
            val dob = snapshot.child("dob").value as? String ?: ""
            val status = snapshot.child("status").value as? String ?: ""
            val avatarPath = snapshot.child("avatar").value as? String
            val location = snapshot.child("location").value as? String


            return UserDetails(
                about = about,
                avatarUri = avatarPath,
                dob = dob,
                gender = gender,
                location = location,
                name = name,
                typing = typing,
                status = status,
                uid = uid,
                username = username
            )
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            throw e
        }
    }


    private fun convertTimeStampToMillis(timeStamp: String): Long {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            val date = dateFormat.parse(timeStamp)
            date?.time ?: 0L
        } catch (e: ParseException) {
            // Handle parsing exception
            0L
        }
    }

    private fun calculateExpiration(timeStamp: String): Long {

        // Convert timeStamp to milliseconds
        val timeStampMillis = convertTimeStampToMillis(timeStamp)

        // Calculate expiration time (timestamp + 24 hours)
        val expirationTimeMillis = timeStampMillis + (24 * 60 * 60 * 1000)

        // Get the current time
        val currentTimeMillis = System.currentTimeMillis()

        return expirationTimeMillis - currentTimeMillis
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }

    private fun filterExpiredList(list: List<Peep>): List<Peep> {
        val peepList = mutableListOf<Peep>()

        list.forEach {
            it.let {
                val timeStamp = it.timeStamp

                val difference = calculateExpiration(timeStamp)

                // Check if peep has not expired
                if (difference > 0) {
                    peepList.add(it)
                }
            }
        }

        return peepList
    }

    private fun networkIsLive(context: Context): Boolean {

        // Get the ConnectivityManager instance
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Get the network capabilities
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        // Check if the network capabilities have internet access
        return (networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
    }

}
