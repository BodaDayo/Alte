package com.rgbstudios.alte.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rgbstudios.alte.data.firebase.FirebaseAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StatusUpdateWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    private val firebase = FirebaseAccess()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val uid = inputData.getString("uid")
            val status = inputData.getString("status")

            if (uid != null) {
                val usersDetailsRef = firebase.getUserListRef().child(uid).child("status")

                if (status != null) {
                    usersDetailsRef.setValue(status)

                    Result.success()
                } else {
                    Result.failure()
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
