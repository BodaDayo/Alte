package com.rgbstudios.alte.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.remote.FirebaseAccess

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

    fun saveUserDetailsToDatabase(user: UserDetails, callback: (Boolean) -> Unit) {
        val usersListRef = firebase.getUsersListRef(user.uid)
        val userDetails = convertUserDetailsToJson(user)

        usersListRef.child(user.uid).setValue(userDetails)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
    }

    private fun convertUserDetailsToJson(user: UserDetails): String {
        val gson = Gson()
        return gson.toJson(user)
    }

    private fun convertJsonToUserDetails(json: String): UserDetails {
        val gson = Gson()
        val type = object : TypeToken<UserDetails>() {}.type
        return gson.fromJson(json, type)
    }
}
