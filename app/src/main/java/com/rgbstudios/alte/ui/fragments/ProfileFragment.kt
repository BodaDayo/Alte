package com.rgbstudios.alte.ui.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.firebase.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentProfileBinding
import com.rgbstudios.alte.ui.adapters.AvatarAdapter
import com.rgbstudios.alte.utils.AvatarManager
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ImageHandler
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private val avatarManager = AvatarManager()
    private val imageHandler = ImageHandler()
    private lateinit var binding: FragmentProfileBinding
    private var isAvatarButtonExpanded = false
    private var avatarBitmapHolder: Bitmap? = null
    private var selectedDefaultAvatar: Bitmap? = null
    private var isSampleImageLayoutVisible = false
    private var isCropImageLayoutVisible = false
    private var isExpandedImageLayoutVisible = false
    private var nameNew: String? = null
    private var aboutNew: String? = null
    private var dobNew: String? = null
    private var locationNew: String? = null
    private var genderNew: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

            pickFromDefaultPF.animate().scaleXBy(-1f).scaleYBy(-1f)
            pickFromGalleryPF.animate().scaleXBy(-1f).scaleYBy(-1f)

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


            changeAvatarPF.setOnClickListener {
                toggleAvatarButtonExpand()
            }

            pickFromDefaultPF.setOnClickListener {

                // Update imageViews
                sampleImageView.setImageBitmap(avatarBitmapHolder)
                expandedImageView.setImageBitmap(avatarBitmapHolder)

                // Open the sampleImageLayout
                sampleImageLayout.visibility = View.VISIBLE

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
                        uploadBitmap()
                    }

                    // Close the sampleImageLayout
                    sampleImageLayout.visibility = View.GONE
                }

                isSampleImageLayoutVisible = true
            }

            pickFromGalleryPF.setOnClickListener {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            editProfileIv.setOnClickListener {

                dialogManager.showProfileEditDialog(
                    nameNew,
                    aboutNew,
                    dobNew,
                    locationNew,
                    genderNew,
                    this@ProfileFragment,
                    alteViewModel
                )
            }

            popBack.setOnClickListener {
                popBackStackManager()
            }

            // Customize onBackPressed method
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                popBackStackManager()
            }

            alteViewModel.currentUser.observe(viewLifecycleOwner) { currentUserDetails ->
                if (currentUserDetails != null) {
                    progressBarPF.visibility = View.VISIBLE

                    namePF.text = currentUserDetails.name.let {
                        nameNew = it
                        it
                    }
                    usernamePF.text =
                        getString(R.string.user_name_template, currentUserDetails.username)
                    aboutPF.text =
                        currentUserDetails.about.let {
                            aboutNew = it
                            if (!it.isNullOrEmpty()) it else getString(R.string.not_set_yet)
                        }

                    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                    birthdayPF.text = currentUserDetails.dob.let {
                        dobNew = it
                        if (!it.isNullOrEmpty()) {
                            try {
                                val date =
                                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it)
                                date?.let { formattedDate ->
                                    dateFormat.format(formattedDate)
                                } ?: getString(R.string.not_set_yet)
                            } catch (e: Exception) {
                                firebase.recordCaughtException(e)
                                getString(R.string.not_set_yet)
                            }
                        } else getString(R.string.not_set_yet)
                    }

                    locationPF.text =
                        currentUserDetails.location.let {
                            locationNew = it
                            if (!it.isNullOrEmpty()) it else getString(
                                R.string.not_set_yet
                            )
                        }
                    genderPF.text =
                        currentUserDetails.gender.let {
                            genderNew = it
                            if (!it.isNullOrEmpty()) it else getString(R.string.not_set_yet)
                        }


                    Glide.with(requireContext())
                        .asBitmap()
                        .load(currentUserDetails.avatarUri)
                        .placeholder(R.drawable.asset2)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                avatarBitmapHolder = resource

                                // Update profileImage
                                Glide.with(requireContext())
                                    .load(resource)
                                    .into(userAvatarPF)

                                // stop loading animation
                                progressBarPF.visibility = View.GONE
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                // stop loading animation
                                progressBarPF.visibility = View.GONE
                            }
                        })

                }
            }
        }
    }

    private fun popBackStackManager() {
        binding.apply {
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

    private fun toggleAvatarButtonExpand() {
        binding.apply {
            if (isAvatarButtonExpanded) {
                // Rotate the ImageView by 180 degrees
                changeAvatarPF.animate().rotationBy(-180f).scaleXBy(0.2f).scaleYBy(0.2f)
                    .withEndAction { changeAvatarPF.setImageResource(R.drawable.camera) }
                    .start()

                pickFromDefaultPF.animate().scaleXBy(-1f).scaleYBy(-1f)
                pickFromGalleryPF.animate().scaleXBy(-1f).scaleYBy(-1f)

                isAvatarButtonExpanded = false

            } else {
                // Rotate the ImageView by 180 degrees
                changeAvatarPF.animate().rotationBy(180f).scaleXBy(-0.2f).scaleYBy(-0.2f)
                    .withEndAction { changeAvatarPF.setImageResource(R.drawable.close) }
                    .start()

                pickFromDefaultPF.animate().scaleXBy(1f).scaleYBy(1f)
                pickFromGalleryPF.animate().scaleXBy(1f).scaleYBy(1f)

                pickFromDefaultPF.visibility = View.VISIBLE
                pickFromGalleryPF.visibility = View.VISIBLE

                isAvatarButtonExpanded = true
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
                        progressBarPF.visibility = View.VISIBLE

                        cropImageLayout.visibility = View.GONE
                        sampleImageLayout.visibility = View.GONE

                        val croppedBitmap = cropImageView.getCroppedImage()
                        val compressedBitmap = croppedBitmap?.let { it1 -> imageHandler.compressBitmap(it1) }

                        avatarBitmapHolder = compressedBitmap!!

                        // Update profileImage
                        uploadBitmap()

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

    private fun uploadBitmap() {
        if (avatarBitmapHolder != null) {

            // show the loading animation
            binding.progressBarPF.visibility = View.VISIBLE

            alteViewModel.uploadAvatar(avatarBitmapHolder!!) { uploadTaskSuccessful ->
                if (uploadTaskSuccessful) {
                    toastManager.showShortToast(
                        requireContext(),
                        "Profile image uploaded successfully!"
                    )
                } else {
                    toastManager.showShortToast(
                        requireContext(),
                        "Profile image upload failed!"
                    )
                }
            }

            // close the loading animation
            binding.progressBarPF.visibility = View.GONE
        }
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