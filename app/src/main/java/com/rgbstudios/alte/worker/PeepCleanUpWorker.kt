package com.rgbstudios.alte.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.rgbstudios.alte.data.firebase.FirebaseAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PeepCleanUpWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    private val firebase = FirebaseAccess()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val uid = inputData.getString("uid")
            val timeStamp = inputData.getString("timeStamp")

            if (uid != null && timeStamp != null) {
                val peepStorageRef = firebase.getPeepStorageRef(uid).child(timeStamp)

                // Use suspendCoroutine to wait for the asynchronous operation
                return@withContext suspendCoroutine<Result> { continuation ->
                    peepStorageRef.delete().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val peepListRef = firebase.getPeepListRef().child(uid)

                            peepListRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (node in snapshot.children) {
                                        if (node.key == timeStamp) {
                                            val nodeRef = node.ref

                                            // Remove the node from the database
                                            nodeRef.removeValue()
                                                .addOnCompleteListener { removeTask ->
                                                    if (removeTask.isSuccessful) {
                                                        continuation.resume(Result.success())
                                                    } else {
                                                        continuation.resume(Result.failure())
                                                    }
                                                }
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    continuation.resume(Result.failure())
                                }
                            })
                        } else {
                            continuation.resume(Result.failure())
                        }
                    }
                }
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            Result.retry()
        }
    }
}
