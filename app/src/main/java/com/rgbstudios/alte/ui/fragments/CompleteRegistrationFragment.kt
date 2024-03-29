package com.rgbstudios.alte.ui.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.UserUploadBundle
import com.rgbstudios.alte.data.firebase.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentCompleteRegistrationBinding
import com.rgbstudios.alte.ui.adapters.AvatarAdapter
import com.rgbstudios.alte.utils.AvatarManager
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ImageHandler
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CompleteRegistrationFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(requireActivity().application as AlteApplication, AlteRepository(firebase))
    }

    private val auth = firebase.auth
    private lateinit var binding: FragmentCompleteRegistrationBinding
    private var selectedGender: String? = null
    private var selectedDate: String? = null
    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private val imageHandler = ImageHandler()
    private val avatarManager = AvatarManager()
    private var avatarBitmapHolder: Bitmap? = null
    private var selectedDefaultAvatar: Bitmap? = null
    private var isCropImageLayoutVisible = false
    private var isSampleImageLayoutVisible = false
    private var isExpandedImageLayoutVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCompleteRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            // Get random avatar drawable resource
            val randomAvatar = avatarManager.getDefaultAvatar()

            // Convert the avatar drawable resource to a bitmap
            avatarBitmapHolder = BitmapFactory.decodeResource(resources, randomAvatar)

            // Update profileImage
            profileImageView.setImageBitmap(avatarBitmapHolder)

            // Set up the gender spinner
            val genderOptions = resources.getStringArray(R.array.gender_options)
            val genderAdapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genderOptions)
            genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            genderSpinner.adapter = genderAdapter
            genderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

            // Set up the defaultAvatarRecyclerView
            val defaultAvatarList = avatarManager.defaultAvatarsList
            val avatarAdapter = AvatarAdapter(defaultAvatarList,
                object : AvatarAdapter.AvatarClickListener {
                    override fun onAvatarClick(avatar: Int) {
                        selectedDefaultAvatar = BitmapFactory.decodeResource(resources, avatar)

                        sampleImageView.setImageBitmap(selectedDefaultAvatar)
                        expandedImageView.setImageBitmap(selectedDefaultAvatar)
                    }
                }
            )
            defaultAvatarRecyclerView.setHasFixedSize(true)
            defaultAvatarRecyclerView.layoutManager = GridLayoutManager(context, 4)
            defaultAvatarRecyclerView.adapter = avatarAdapter

            profileImageView.setOnClickListener {

                // Show the loading animation
                overlayView.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE

                // Update imageViews
                sampleImageView.setImageBitmap(avatarBitmapHolder)
                expandedImageView.setImageBitmap(avatarBitmapHolder)

                // Open the sampleImageLayout
                sampleImageLayout.visibility = View.VISIBLE

                // Close the loading animation
                overlayView.visibility = View.GONE
                progressBar.visibility = View.GONE

                imageSamplePopBack.setOnClickListener {

                    // Close the sampleImageLayout
                    sampleImageLayout.visibility = View.GONE
                    isSampleImageLayoutVisible = false
                }

                editAvatar.setOnClickListener {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }

                // Expand the avatar
                sampleImageView.setOnClickListener {
                    expandedImageLayout.visibility = View.VISIBLE

                    // close expanded image layout
                    closeExpandedImage.setOnClickListener {
                        expandedImageLayout.visibility = View.GONE

                        isExpandedImageLayoutVisible = false
                    }

                    isExpandedImageLayoutVisible = true
                }

                // Save avatar selection
                continueToProfileButton.setOnClickListener {
                    if (selectedDefaultAvatar != null) {
                        avatarBitmapHolder = selectedDefaultAvatar

                        // Update profileImage
                        profileImageView.setImageBitmap(avatarBitmapHolder)
                    }

                    // Close the sampleImageLayout
                    sampleImageLayout.visibility = View.GONE
                }

                isSampleImageLayoutVisible = true
            }

            changeAvatar.setOnClickListener {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            dobEt.setOnClickListener {
                showDatePickerDialog()
            }

            saveButton.setOnClickListener {
                saveUserDetails()
            }

            // Customize onBackPressed method
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                if (isCropImageLayoutVisible) {

                    cropImageLayout.visibility = View.GONE
                    isCropImageLayoutVisible = false
                } else if (isExpandedImageLayoutVisible) {

                    expandedImageLayout.visibility = View.GONE
                    isExpandedImageLayoutVisible = false
                } else if (isSampleImageLayoutVisible) {

                    sampleImageLayout.visibility = View.GONE
                    isSampleImageLayoutVisible = false
                } else {
                    // If no changes, simply pop back stack
                    activity?.supportFragmentManager?.popBackStack()
                }
            }

        }
    }

    private fun saveUserDetails() {
        binding.apply {

            val name = fullNameEt.text.toString().trim()
            val username = usernameEt.text.toString().trim()
            val gender = selectedGender
            val about = aboutEt.text.toString().trim()
            val dob = selectedDate
            val avatar = avatarBitmapHolder
            val status = getString(R.string.online)
            val location = locationEt.text.toString().trim()

            if (name.isEmpty()) {
                // Show error message
                fullNameEt.error = getString(R.string.please_enter_name)
            } else {

                if (usernameIsValid(username)) {
                    val currentUser = firebase.currentUser
                    val uid = currentUser?.uid

                    if (uid != null) {
                        saveProgressBar.visibility = View.VISIBLE
                        saveButton.text = getString(R.string.empty_text)
                        saveButton.isEnabled = false
                        val bundle = UserUploadBundle(
                            about, avatar, dob, gender, location, name, status, uid, username
                        )

                        alteViewModel.saveUserDetails(
                            bundle,
                        ) { successful ->
                            if (successful) {
                                navigateToMessagesFragment()

                                // Update the usernameSetStatus in the viewModel
                                alteViewModel.updateUsernameSetStatus(true)
                            }
                            saveProgressBar.visibility = View.GONE
                            saveButton.text = getString(R.string.save)
                            saveButton.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun navigateToMessagesFragment() {
        findNavController().navigate(R.id.action_completeRegistrationFragment_to_messagesFragment)
    }

    private fun usernameIsValid(username: String): Boolean {
        binding.apply {
            // Define the regex pattern
            val regexPattern = "^[a-zA-Z0-9_]{4,20}$"

            if (username.isEmpty()) {
                // Show error message
                usernameEt.error = "Please enter a username"
                return false
            } else if (!username.matches(Regex(regexPattern))) {
                // Check specific conditions and show more detailed error messages
                if (username.length < 4) {
                    usernameEt.error = "Username must be at least 4 characters long"
                } else if (username.length > 20) {
                    usernameEt.error = "Username must be at most 20 characters long"
                } else {
                    // If the username doesn't match the general format
                    usernameEt.error = "Invalid username format. Use only letters and numbers."
                }
                return false
            } else {
                return true
            }
        }
    }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {

                binding.apply {
                    cropImageView.setImageUriAsync(uri)
                    cropImageView.setAspectRatio(1, 1)

                    // show the cropping layout
                    cropImageLayout.visibility = View.VISIBLE
                    isCropImageLayoutVisible = true

                    doneCrop.setOnClickListener {

                        // Show the loading animation
                        overlayView.visibility = View.VISIBLE
                        progressBar.visibility = View.VISIBLE

                        cropImageLayout.visibility = View.GONE
                        sampleImageLayout.visibility = View.GONE

                        val croppedBitmap = cropImageView.getCroppedImage()
                        val compressedBitmap = croppedBitmap?.let { it1 -> imageHandler.compressBitmap(it1) }

                        avatarBitmapHolder = compressedBitmap!!

                        // Update profileImage
                        profileImageView.setImageBitmap(avatarBitmapHolder)

                        // Close the loading animation
                        overlayView.visibility = View.GONE
                        progressBar.visibility = View.GONE

                        isSampleImageLayoutVisible = false
                        isCropImageLayoutVisible = false
                    }

                    cancelCrop.setOnClickListener {
                        cropImageLayout.visibility = View.GONE
                        isCropImageLayoutVisible = false
                    }

                    rotateCrop.setOnClickListener {
                        cropImageView.rotateImage(90)
                    }
                }
            }
        }

    private fun showDatePickerDialog() {
        dialogManager.showDatePickerDialog(this) { pickedDate ->
            if (pickedDate != null) {
                updateDOBETV(pickedDate)
            }
        }
    }

    private fun updateDOBETV(pickedDate: Calendar) {
        val formattedDate = parseDobToString(pickedDate)

        selectedDate = formattedDate

        binding.dobEt.text = formattedDate
    }

    private fun parseDobToString(pickedDate: Calendar): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(pickedDate.time)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the state of flags
        outState.putBoolean("isCropImageLayoutVisible", isCropImageLayoutVisible)
        outState.putBoolean("isSampleImageLayoutVisible", isSampleImageLayoutVisible)
        outState.putBoolean("isExpandedImageLayoutVisible", isExpandedImageLayoutVisible)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Restore the state of flags
        isCropImageLayoutVisible =
            savedInstanceState?.getBoolean("isCropImageLayoutVisible") ?: false
        isSampleImageLayoutVisible =
            savedInstanceState?.getBoolean("isSampleImageLayoutVisible") ?: false
        isExpandedImageLayoutVisible =
            savedInstanceState?.getBoolean("isExpandedImageLayoutVisible") ?: false

        binding.apply {
            // Reset cropImage layout visibility
            if (isCropImageLayoutVisible) {
                cropImageLayout.visibility = View.VISIBLE
            }
            // Reset imageSample layout visibility
            if (isSampleImageLayoutVisible) {
                sampleImageLayout.visibility = View.VISIBLE
            }
            // Reset imageExpand layout visibility
            if (isExpandedImageLayoutVisible) {
                expandedImageLayout.visibility = View.VISIBLE
            }
        }
    }

}