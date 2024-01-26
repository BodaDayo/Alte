package com.rgbstudios.alte.data.repository

import android.graphics.Bitmap
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.rgbstudios.alte.data.model.CurrentUserDetails
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.model.UserUploadBundle
import com.rgbstudios.alte.data.remote.FirebaseAccess
import java.io.ByteArrayOutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlteRepository(private val firebase: FirebaseAccess) {

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

    fun saveUserDetailsToDatabase(
        bundle: UserUploadBundle,
        callback: (Boolean) -> Unit
    ) {
        val avatarStorageRef = firebase.getAvatarStorageRef(bundle.uid)

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

    }

    private fun uploadUserDetails(
        bundle: UserUploadBundle,
        avatarUri: String,
        callback: (Boolean) -> Unit
    ) {
        val usersDetailsRef = firebase.getUserListRef()

        val hashMap: HashMap<String, String> = HashMap()
        hashMap["name"] = bundle.name ?: ""
        hashMap["username"] = bundle.username
        hashMap["gender"] = bundle.gender ?: ""
        hashMap["about"] = bundle.about ?: ""
        hashMap["dob"] = bundle.dob ?: ""
        hashMap["avatar"] = avatarUri
        hashMap["status"] = bundle.status

        usersDetailsRef.child(bundle.uid).setValue(hashMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
    }

    fun getCurrentUserFromDatabase(
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

                        val currentUserDetails = CurrentUserDetails(
                            about = userDetails.about,
                            avatarUri = userDetails.avatarUri,
                            dob = userDetails.dob,
                            folks = folks,
                            gender = userDetails.gender,
                            invites = invites,
                            name = userDetails.name,
                            requests = requests,
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

    fun getUserListFromDatabase(callback: (List<UserDetails>) -> Unit) {
        val userListRef = firebase.getUserListRef()
        try {
            userListRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userList = mutableListOf<UserDetails>()
                    for (dataSnapShot in snapshot.children) {
                        if (dataSnapShot.key != null) {
                            val userDetail = mapSnapshotToUserDetails(dataSnapShot)
                            userList.add(userDetail)
                        }
                    }

                    callback(userList)
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

    private fun mapSnapshotToUserDetails(
        snapshot: DataSnapshot
    ): UserDetails {

        val uid = snapshot.key!!
        val name = snapshot.child("name").value as? String
        val username = snapshot.child("username").value as? String ?: ""
        val gender = snapshot.child("gender").value as? String
        val about = snapshot.child("about").value as? String
        val dob = snapshot.child("dob").value as? String ?: ""
        val status = snapshot.child("status").value as? String ?: ""
        val avatarPath = snapshot.child("avatar").value as? String


        return UserDetails(
            about = about,
            avatarUri = avatarPath,
            dob = dob,
            gender = gender,
            name = name,
            status = status,
            uid = uid,
            username = username
        )
    }

    fun addUserToInviteList(senderId: String, receiverId: String, callback: (Boolean) -> Unit) {
        val senderDetailsRef = firebase.getUserListRef().child(senderId)
        val receiverDetailsRef = firebase.getUserListRef().child(receiverId)

        try {
            // Read the sender's current invites list
            senderDetailsRef.child("invites")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentInvitesList =
                            snapshot.getValue(object : GenericTypeIndicator<List<String>>() {})

                        // If the "invites" child doesn't exist or is null, create an empty list
                        val invitesList =
                            currentInvitesList?.toMutableList()
                                ?: emptyList<String>().toMutableList()

                        // Check if receiverId is not in the list, add it
                        if (!invitesList.contains(receiverId)) {
                            invitesList.add(receiverId)

                            // Set the updated invites list back to the database
                            senderDetailsRef.child("invites").setValue(invitesList)
                                .addOnCompleteListener { senderTask ->
                                    if (senderTask.isSuccessful) {

                                        // Proceed to update receiver's requests list
                                        updateReceiversRequestList(
                                            true,
                                            receiverDetailsRef,
                                            senderId
                                        ) {
                                            callback(it)
                                        }

                                    } else {
                                        callback(false)
                                    }
                                }
                        } else {
                            // If receiverId is already in the list, no need to update
                            callback(true)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle onCancelled if needed
                        callback(false)
                    }
                })
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }
    }

    fun acceptConnectionRequest(
        receiverId: String,
        senderId: String,
        callback: (Boolean) -> Unit
    ) {
        val receiverDetailsRef = firebase.getUserListRef().child(receiverId)
        val senderDetailsRef = firebase.getUserListRef().child(senderId)

        try {
            // Read the current folks list
            receiverDetailsRef.child("folks")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentFolksList =
                            snapshot.getValue(object : GenericTypeIndicator<List<String>>() {})

                        // If the "invites" child doesn't exist or is null, create an empty list
                        val folksList =
                            currentFolksList?.toMutableList()
                                ?: emptyList<String>().toMutableList()

                        // Check if senderId is not in the list, add it
                        if (!folksList.contains(senderId)) {
                            folksList.add(senderId)

                            // Set the updated folks list back to the database
                            receiverDetailsRef.child("folks").setValue(folksList)
                                .addOnCompleteListener { folkSetterTask ->
                                    if (folkSetterTask.isSuccessful) {

                                        // Proceed to update receiver's requests list
                                        updateReceiversRequestList(
                                            false,
                                            senderDetailsRef,
                                            senderId
                                        ) { removalTaskSuccessful ->
                                            if (removalTaskSuccessful) {
                                                removeUserFromInvitesList(senderId, receiverId) {
                                                    callback(it)
                                                }
                                            } else {
                                                callback(false) // requestListRemoval unsuccessful
                                            }
                                        }

                                    } else {
                                        callback(false) // folkSetterTask unsuccessful
                                    }
                                }
                        } else {
                            // If receiverId is already in the list, no need to update
                            callback(true)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle onCancelled if needed
                        callback(false)
                    }
                })
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }
    }
    
    fun cancelConnectionRequest(
        receiverId: String,
        senderId: String,
        callback: (Boolean) -> Unit
    ) {
        val receiverDetailsRef = firebase.getUserListRef().child(receiverId)
        val senderDetailsRef = firebase.getUserListRef().child(senderId)

        try {
            // Read the sender's current invites list
            senderDetailsRef.child("invites")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentInvitesList =
                            snapshot.getValue(object : GenericTypeIndicator<List<String>>() {})

                        val invitesList =
                            currentInvitesList?.toMutableList()
                                ?: emptyList<String>().toMutableList()

                        // Check if receiverId is in the list, remove it
                        if (invitesList.contains(receiverId)) {
                            invitesList.remove(receiverId)

                            // Set the updated invites list back to the database
                            senderDetailsRef.child("invites").setValue(invitesList)
                                .addOnCompleteListener { senderRemovalTask ->
                                    if (senderRemovalTask.isSuccessful) {

                                        // Proceed to update receiver's requests list
                                        updateReceiversRequestList(
                                            false,
                                            receiverDetailsRef,
                                            senderId
                                        ) {
                                            callback(it)
                                        }

                                    } else {
                                        callback(false) // sender's invites list removal unsuccessful
                                    }
                                }
                        } else {
                            // If receiverId is no longer in the list, no need to update
                            callback(true)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle onCancelled if needed
                        callback(false)
                    }
                })
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }
    }

    private fun updateReceiversRequestList(
        toAdd: Boolean,
        receiverDetailsRef: DatabaseReference,
        senderId: String,
        callback: (Boolean) -> Unit
    ) {
        if (toAdd) {
            receiverDetailsRef.child("requests")
                .addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentRequestsList =
                            snapshot.getValue(object :
                                GenericTypeIndicator<List<String>>() {})

                        // If the "requests" child doesn't exist or is null, create an empty list
                        val requestsList =
                            currentRequestsList?.toMutableList()
                                ?: emptyList<String>().toMutableList()

                        // Check if senderId is not in the list, add it
                        if (!requestsList.contains(senderId)) {
                            requestsList.add(senderId)

                            // Set the updated requests list back to the database
                            receiverDetailsRef.child("requests").setValue(requestsList)
                                .addOnCompleteListener { receiverTask ->
                                    if (receiverTask.isSuccessful) {
                                        callback(true)
                                    } else {
                                        callback(false)
                                    }

                                }
                        } else {
                            // If senderId is already in the list, no need to update
                            callback(true)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle onCancelled if needed
                        callback(false)
                    }
                })
        } else {
            receiverDetailsRef.child("requests")
                .addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentRequestsList =
                            snapshot.getValue(object :
                                GenericTypeIndicator<List<String>>() {})

                        // Get the "requests" child
                        val requestsList =
                            currentRequestsList?.toMutableList()
                                ?: emptyList<String>().toMutableList()

                        // Check if senderId is in the list, remove it
                        if (requestsList.contains(senderId)) {
                            requestsList.remove(senderId)

                            // Set the updated requests list back to the database
                            receiverDetailsRef.child("requests").setValue(requestsList)
                                .addOnCompleteListener { receiverTask ->
                                    if (receiverTask.isSuccessful) {
                                        callback(true)
                                    } else {
                                        callback(false)
                                    }

                                }
                        } else {
                            // If senderId is already not in the list, no need to update
                            callback(true)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle onCancelled if needed
                        callback(false)
                    }
                })
        }
    }

    fun removeUserFromInvitesList(
        senderId: String,
        receiverId: String,
        callback: (Boolean) -> Unit
    ) {
        val senderDetailsRef = firebase.getUserListRef().child(senderId)

        try {
            // Read the sender's current invites list
            senderDetailsRef.child("invites")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentInvitesList =
                            snapshot.getValue(object : GenericTypeIndicator<List<String>>() {})

                        // If the "invites" child doesn't exist or is null, create an empty list
                        val invitesList =
                            currentInvitesList?.toMutableList()
                                ?: emptyList<String>().toMutableList()

                        // Check if receiverId is in the list, remove it
                        if (invitesList.contains(receiverId)) {
                            invitesList.remove(receiverId)

                            // Set the updated invites list back to the database
                            senderDetailsRef.child("invites").setValue(invitesList)
                                .addOnCompleteListener { senderTask ->
                                    if (senderTask.isSuccessful) {
                                        callback(true)
                                    } else {
                                        callback(false)
                                    }
                                }
                        } else {
                            // If receiverId is no longer in the list, no need to update
                            callback(true)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle onCancelled if needed
                        callback(false)
                    }
                })
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }

    private fun parseDobToString(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun parseDobFromString(dobString: String): Calendar? {
        if (dobString.isEmpty()) {
            return null
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        return try {
            calendar.time = dateFormat.parse(dobString) ?: Date()
            calendar
        } catch (e: ParseException) {
            // Handle parsing exception
            null
        }
    }
}
