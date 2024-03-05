package com.rgbstudios.alte.utils

import android.app.DatePickerDialog
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.Chat
import com.rgbstudios.alte.databinding.DialogEditProfileBinding
import com.rgbstudios.alte.databinding.DialogForgotPasswordBinding
import com.rgbstudios.alte.databinding.DialogPeepDetailBinding
import com.rgbstudios.alte.databinding.DialogPrivacyPolicyBinding
import com.rgbstudios.alte.databinding.DialogRemoveConfirmationBinding
import com.rgbstudios.alte.viewmodel.AlteViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DialogManager {

    private val toastManager = ToastManager()

    fun showDeleteConfirmationDialog(
        chatList: List<Chat>,
        starredList: List<String>,
        fragment: Fragment,
        viewModel: AlteViewModel,
        callback: (Int) -> Unit
    ) {
        val context = fragment.context ?: return
        val layoutInflater = fragment.layoutInflater

        val dialogBinding = DialogRemoveConfirmationBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.apply {

            removeTitle.text = context.getString(R.string.confirm_delete)
            val deleteCount = chatList.size
            removeBody.text = context.resources.getQuantityString(
                R.plurals.delete_count,
                deleteCount,
                deleteCount
            )

            btnConfirm.text = context.getString(R.string.delete)

            var successfulCallbacks = 0

            btnConfirm.setOnClickListener {
                dialog.dismiss()
                callback(1)

                chatList.forEach { chat ->
                    val node = chat.timeStamp + chat.senderId
                    val isStarred = starredList.contains(node)

                    viewModel.deleteMessage(chat, isStarred) { isSuccess ->
                        if (isSuccess) {
                            successfulCallbacks++
                            if (successfulCallbacks == chatList.size) {
                                // All callbacks are successful
                                callback(2)
                            }
                        } else {
                            callback(3)
                        }
                    }
                }
            }

            btnCancel.setOnClickListener {
                // Dismiss the dialog
                dialog.dismiss()
            }
        }

        dialog.show()
    }

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

            val email = dialogBinding.emailEt.text.toString().trim()

            if (email.isNotEmpty()) {
                dialogBinding.progressBar.visibility = View.VISIBLE

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

    fun showLogoutConfirmationDialog(
        fragment: Fragment,
        viewModel: AlteViewModel,
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
            viewModel.logOut(context) { logOutSuccessful, errorMessage ->

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

    fun showProfileEditDialog(
        name: String?,
        about: String?,
        dob: String?,
        location: String?,
        gender: String?,
        fragment: Fragment,
        viewModel: AlteViewModel
    ) {
        val context = fragment.context ?: return
        val layoutInflater = fragment.layoutInflater

        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.apply {
            var selectedGender: String? = null
            var selectedDate: String? = null

            // Set up the gender spinner
            val genderOptions = context.resources.getStringArray(R.array.gender_options)
            val genderAdapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_item, genderOptions)
            genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            genderSpinnerPE.adapter = genderAdapter

            // Find the position of the gender in the array
            val genderPosition = genderOptions.indexOf(gender.toString())

            // Set the selection for the spinner
            if (genderPosition != -1) {
                genderSpinnerPE.setSelection(genderPosition)
            }

            genderSpinnerPE.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parentView: AdapterView<*>?,
                        selectedItemView: View?,
                        position: Int,
                        id: Long
                    ) {
                        // Handle the selected item (gender)
                        selectedGender = genderOptions[position]
                    }

                    override fun onNothingSelected(parentView: AdapterView<*>?) {
                        // Do nothing here
                    }
                }

            dobPE.setOnClickListener {
                showDatePickerDialog(fragment) { pickedDate ->
                    if (pickedDate != null) {
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val formattedDate = dateFormat.format(pickedDate.time)
                        selectedDate = formattedDate

                        dobPE.text = formattedDate
                    }
                }
            }

            saveButtonPE.setOnClickListener {
                val nameNew = namePE.text.toString().trim()
                val genderNew = selectedGender
                val aboutNew = aboutPE.text.toString().trim()
                val dobNew = selectedDate
                val locationNew = locationPE.text.toString().trim()

                if (nameNew.isEmpty()) {
                    // Show error message
                    namePE.error = context.getString(R.string.please_enter_name)
                } else {
                    progressBarPE.visibility = View.VISIBLE
                    saveButtonPE.text = context.getString(R.string.empty_text)
                    saveButtonPE.isEnabled = false

                    viewModel.updateUserDetails(
                        aboutNew, dobNew, genderNew, locationNew, nameNew
                    ) { updateSuccessful, errorMessage ->
                        if (updateSuccessful) {
                            toastManager.showShortToast(
                                context,
                                context.getString(R.string.profile_update_success)
                            )
                            dialog.dismiss()
                        } else {
                            toastManager.showShortToast(context, errorMessage)
                        }

                        progressBarPE.visibility = View.GONE
                        saveButtonPE.text = context.getString(R.string.save)
                        saveButtonPE.isEnabled = true
                    }
                }
            }

            popBackPE.setOnClickListener {
                dialog.dismiss()
            }

            if (name != null) namePE.setText(name)
            if (about != null) aboutPE.setText(about)
            if (dob != null) dobPE.text = dob
            if (dob != null) locationPE.setText(location)
        }

        dialog.show()
    }

    fun showPeepDetailDialog(
        viewedList: List<String>,
        fragment: Fragment,
        callback: (Int) -> Unit
    ) {
        val context = fragment.context ?: return
        val layoutInflater = fragment.layoutInflater

        val dialogBinding = DialogPeepDetailBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        var isDismissedByButton = false

        val viewCount = viewedList.size

        dialogBinding.apply {
            titleVD.text = context.getString(R.string.views_template, viewCount)

            if (viewCount > 0) {
                viewedListSp.visibility = View.VISIBLE
                emptyViewed.visibility = View.GONE

                val adapter =
                    ArrayAdapter(context, android.R.layout.simple_list_item_1, viewedList)
                viewedListSp.adapter = adapter

            } else {
                viewedListSp.visibility = View.GONE
                emptyViewed.visibility = View.VISIBLE
            }

            forwardPeepVD.setOnClickListener {
                callback(1)
                isDismissedByButton = true
                dialog.dismiss()
            }

            deletePeepVD.setOnClickListener {
                callback(2)
                isDismissedByButton = true
                dialog.dismiss()
            }
        }

        dialog.setOnDismissListener {
            if (!isDismissedByButton) {
                callback(0) // Only invoke callback(0) if dismissed by clicking outside the dialog
            }
        }

        dialog.show()
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