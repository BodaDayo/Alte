package com.rgbstudios.alte.utils

import android.app.DatePickerDialog
import android.content.Context
import android.text.Html
import androidx.core.content.res.TypedArrayUtils.getText
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.databinding.DialogPrivacyPolicyBinding
import java.util.Calendar

class DialogManager {

    private val toastManager = ToastManager()
    private val firebase = FirebaseAccess()

    fun showPrivacyPolicyDialog(
        fragment: Fragment,
        callback: (Boolean) -> Unit
    ) {
        val context = fragment.context ?: return
        val layoutInflater = fragment.layoutInflater

        val dialogBinding = DialogPrivacyPolicyBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.agreeButton.setOnClickListener {
            // Dismiss the dialog
            dialog.dismiss()

            callback(true)
        }

        dialog.show()
    }

    fun showDatePickerDialog(
        fragment: Fragment,
        callback: (Calendar?) -> Unit
    ) {
        val context = fragment.context ?: return
        val calendar = Calendar.getInstance()


        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                try {
                    // Handle selected date
                    calendar.set(year, month, dayOfMonth)

                    // Return selectedDate
                    callback(calendar)
                } catch (e: Exception) {
                    callback(null)
                    toastManager.showShortToast(context, "Pick a valid Date")
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set the minimum date to the current date
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.show()
    }
/*

    fun showDiscardDialog(
        fragment: Fragment,
        callback: (Boolean) -> Unit
    ) {
        val context = fragment.context ?: return
        val layoutInflater = fragment.layoutInflater

        val dialogBinding = DialogDiscardTaskBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDiscardConfirm.setOnClickListener {

            // Dismiss the dialog
            dialog.dismiss()

            callback(true)

        }

        dialogBinding.btnDiscardCancel.setOnClickListener {
            // Dismiss the dialog
            dialog.dismiss()

            callback(false)
        }

        dialog.show()
    }

    fun showLogoutConfirmationDialog(
        fragment: Fragment,
        viewModel: TodoViewModel,
        callback: (Boolean) -> Unit
    ) {
        val context = fragment.context ?: return
        val layoutInflater = fragment.layoutInflater

        val dialogBinding = DialogRemoveConfirmationBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnConfirm.setOnClickListener {
            // Call the ViewModel's logout method to sign out the user
            viewModel.logOut { logOutSuccessful, errorMessage ->

                if (logOutSuccessful) {
                    // Dismiss the dialog
                    dialog.dismiss()

                    callback(true)

                } else {
                    // Dismiss the dialog
                    dialog.dismiss()

                    errorMessage?.let { message ->
                        val output = message.substringAfter(": ")
                        toastManager.showLongToast(context, output)
                    }
                }
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            // Dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showForgotPasswordDialog(
        fragment: Fragment,
        auth: FirebaseAuth
    ) {
        val context = fragment.context ?: return
        val layoutInflater = fragment.layoutInflater

        val dialogBinding = DialogForgotPasswordBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.resetPasswordButton.setOnClickListener {

            val email = dialogBinding.emailEditText.text.toString().trim()

            if (email.isNotEmpty()) {
                dialogBinding.progressBar.visibility =View.VISIBLE

                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            toastManager.showLongToast(
                                context,
                                "Password reset link sent to your email"
                            )
                        } else {
                            toastManager.showShortToast(
                                context,
                                "Failed to send password reset email"
                            )
                        }
                        // Dismiss the dialog
                        dialog.dismiss()
                    }
            } else {
                toastManager.showShortToast(
                    context,
                    "Please enter your email"
                )
            }
        }

        dialog.show()
    }

    fun showFeedbackDialog(
        fragment: Fragment,
        userEmail: String,
    ) {
        val context = fragment.context ?: return
        val layoutInflater = fragment.layoutInflater

        val dialogBinding = DialogFeedbackBinding.inflate(layoutInflater)

        // Create a dialog using MaterialAlertDialogBuilder and set the custom ViewBinding layout
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        // Variable to store the selected emoji details
        var selectedEmojiTriple: Triple<String, Int, Int>? = null


        // Get the emojis
        val emojiList = iconManager.getEmojiList()

        dialogBinding.apply {

            feedbackImageView.visibility = View.VISIBLE
            emojiSelectedBack.visibility = View.INVISIBLE
            emojiSelectedView.visibility = View.INVISIBLE
            feedbackThanks.visibility = View.INVISIBLE

            val emojiAdapter =
                EmojiAdapter(
                    emojiList,
                    object : EmojiAdapter.EmojiClickListener {
                        override fun onEmojiClick(emojiTriple: Triple<String, Int, Int>) {
                            selectedEmojiTriple = emojiTriple
                        }

                    }
                )
            emojiRecyclerView.setHasFixedSize(true)
            emojiRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

            // Set the adapter for the emojiRecyclerView
            emojiRecyclerView.adapter = emojiAdapter

            submitButton.setOnClickListener {
                try {// Check if an emoji has been selected before proceeding
                    if (selectedEmojiTriple == null) {
                        toastManager.showLongToast(
                            context,
                            "Please rate your user experience using one of the emojis"
                        )
                        return@setOnClickListener
                    }
                    val emojiIdentifier = selectedEmojiTriple!!.first
                    val emojiIconResource = selectedEmojiTriple!!.second
                    val emojiColor = selectedEmojiTriple!!.third

                    val userRating =
                        emojiIdentifier.replaceFirstChar { it.uppercase() }.replace('_', ' ')

                    val userComment =
                        editCommentEt.text.toString().ifEmpty { "No additional comments" }

                    emojiSelectedView.setImageResource(emojiIconResource)

                    emojiSelectedBack.setColorFilter(
                        ContextCompat.getColor(context, emojiColor),
                        PorterDuff.Mode.SRC_IN
                    )

                    feedbackImageView.visibility = View.INVISIBLE
                    emojiSelectedBack.visibility = View.VISIBLE
                    emojiSelectedView.visibility = View.VISIBLE
                    feedbackThanks.visibility = View.VISIBLE

                    // Remove any previously posted callbacks to avoid multiple executions
                    delayedFeedbackHandler?.removeCallbacksAndMessages(null)

                    // Create a new Handler for delayed navigation
                    delayedFeedbackHandler = Handler(Looper.myLooper()!!)

                    delayedFeedbackHandler?.postDelayed({

                        val deviceModel = Build.MODEL
                        val androidVersion = Build.VERSION.RELEASE

                        // Get the current date and time
                        val currentTime = Calendar.getInstance()
                        val dateFormat = SimpleDateFormat(
                            "EEE, MMM dd, yyyy 'At' hh:mm a",
                            Locale.getDefault()
                        )
                        val formattedTime = dateFormat.format(currentTime.time)

                        // Create a StringBuilder to build the feedback message
                        val feedbackMessage = StringBuilder()
                        feedbackMessage.append("UserDetails Rating: $userRating\n")
                        feedbackMessage.append("UserDetails Comment: $userComment\n")
                        feedbackMessage.append("Device Model: $deviceModel\n")
                        feedbackMessage.append("Android Version: $androidVersion\n")
                        feedbackMessage.append("Feedback sent by: $userEmail on: $formattedTime")

                        val supportMail = context.getString(R.string.support_email)

                        // Create an Intent to send feedback
                        val emailIntent = Intent(Intent.ACTION_SEND)
                        emailIntent.type = "text/plain"
                        emailIntent.putExtra(
                            Intent.EXTRA_EMAIL, supportMail)
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "UserDetails Feedback")
                        emailIntent.putExtra(Intent.EXTRA_TEXT, feedbackMessage.toString())

                        // Start the email client or any other app that can handle this intent
                        context.startActivity(
                            Intent.createChooser(
                                emailIntent,
                                "Send Feedback"
                            )
                        )

                        // Clear the EditText field
                        editCommentEt.text = null

                        // Dismiss the dialog
                        dialog.dismiss()
                    }, 1500)
                } catch (e: Exception) {
                    firebase.recordCaughtException(e)

                    // Dismiss the dialog
                    dialog.dismiss()

                    toastManager.showLongToast(
                        context,
                        "Feedback Sending failed, something went wrong!"
                    )
                }

            }
        }
        dialog.show()

    }


 */


    /**
     *-----------------------------------------------------------------------------------------------
     */



    /**
     *-----------------------------------------------------------------------------------------------
     */

}