package com.rgbstudios.alte.data.firebase


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rgbstudios.alte.utils.SharedPreferencesManager

class FirebaseAccess {

    val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val crashlytics = Firebase.crashlytics

    val currentUser = auth.currentUser

    fun getToken(sharedPreferencesManager: SharedPreferencesManager) {
        FirebaseService.sharedPref = sharedPreferencesManager

        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                FirebaseService.token = token
            }
        }
    }

    fun signIn(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
            if (it.isSuccessful) {
                callback(true, null)
            } else {
                // Todo change wrong password error message
                val errorMessage =
                    it.exception?.message?.substringAfter(": ")
                        ?: "Error signing in!\nTry Again"
                callback(false, errorMessage)
            }
        }
    }

    fun signUp(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
            if (it.isSuccessful) {
                callback(true, null)
            } else {
                val errorMessage =
                    it.exception?.message?.substringAfter(": ")
                        ?: "Error signing up!\nTry Again"
                callback(false, errorMessage)
            }
        }
    }

    fun logOut(callback: (Boolean, String?) -> Unit) {
        try {
            auth.signOut()
            callback(true, null)
        } catch (e: Exception) {
            callback(false, e.message)
        }
    }


    fun changePassword(newPassword: String, callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser

        user?.updatePassword(newPassword)
            ?.addOnCompleteListener {
                if (it.isSuccessful) {
                    callback(true, null)
                } else {
                    val errorMessage = it.exception?.message?.substringAfter(": ")
                    callback(false, errorMessage ?: "Failed to update password.")
                }
            }
    }


    fun deleteAccountAndData(callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        val userId = user?.uid

        // Delete Firebase Authentication account
        user?.delete()
            ?.addOnCompleteListener { accountDeleteTask ->
                if (accountDeleteTask.isSuccessful) {
                    // Delete data from Realtime Database
                    if (userId != null) {
                        val userRef = database.reference.child("users").child(userId)
                        userRef.removeValue()
                    }

                    // Delete avatar from Firebase Storage
                    if (userId != null) {
                        val avatarRef = storage.reference.child("avatars").child(userId)
                        avatarRef.delete()
                    }

                    callback(true, null)
                } else {
                    callback(false, "Failed to delete account.")
                }
            }
    }

    fun getUserListRef(): DatabaseReference {

        return database.reference
            .child("users")
    }

    fun getPeepListRef(): DatabaseReference {

        return database.reference
            .child("peeps")
    }

    fun getAvatarStorageRef(uid: String): StorageReference {

        return storage.reference
            .child("avatars")
            .child(uid)
    }

    fun getPeepStorageRef(uid: String): StorageReference {

        return storage.reference
            .child("peeps")
            .child(uid)
    }

    fun getChatStorageRef(uid: String): StorageReference {

        return storage.reference
            .child("chats")
            .child(uid)
    }

    fun getChatListRef(uid: String): DatabaseReference {

        return database.reference
            .child("chats")
            .child(uid)
    }

    fun addLog(message: String) {
        crashlytics.log(message)
    }

    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }

    fun recordCaughtException(e: Exception) {
        crashlytics.recordException(e)
    }

}