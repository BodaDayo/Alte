package com.rgbstudios.alte.ui.fragments

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.Chat
import com.rgbstudios.alte.data.model.Peep
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentPeepsBinding
import com.rgbstudios.alte.utils.DateTimeManager
import com.rgbstudios.alte.ui.adapters.SlideshowPagerAdapter
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class PeepsFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by activityViewModels {
        AlteViewModelFactory(
            requireActivity().application as AlteApplication,
            AlteRepository(firebase)
        )
    }

    private lateinit var binding: FragmentPeepsBinding
    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private val dateTimeManager = DateTimeManager()
    private var citizen: UserDetails? = null

    private var currentPage = 1
    private val handler = Handler(Looper.getMainLooper())
    private val delay = 5000L
    private var peepList = emptyList<Peep>()
    private var isSlideshowPaused = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPeepsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            val currentUserId = alteViewModel.currentUser.value?.uid ?: ""

            moreOptions.setOnClickListener { view ->
                showOverflowMenu(view)
            }

            popBack.setOnClickListener {
                activity?.supportFragmentManager?.popBackStack()
            }

            val peepPair = alteViewModel.peepItemSelected.value

            if (peepPair != null) {

                citizen = peepPair.first
                val citizenIsCurrentUser = citizen?.uid == currentUserId

                fullNamePeep.text = if (citizenIsCurrentUser) getString(R.string.you) else citizen?.name
                usernamePeep.text = getString(R.string.user_name_template, citizen?.username)

                peepList = peepPair.second ?: emptyList()

                val adapter = SlideshowPagerAdapter(
                    alteViewModel,
                    citizenIsCurrentUser,
                    this@PeepsFragment,
                    requireContext(),
                    peepList,
                    dialogManager,
                    object : SlideshowPagerAdapter.PagerClickListener {
                        override fun performAction(action: Int) {
                            when (action) {
                                FORWARD -> {
                                    viewPager.currentItem = currentPage++
                                }
                                BACKWARD -> {
                                    if (currentPage > 1) {
                                        viewPager.currentItem = currentPage - 2
                                    }
                                }
                                PAUSE -> {
                                    pauseSlideshow()
                                }
                                RESUME -> {
                                    resumeSlideshow()
                                }
                            }
                        }

                        override fun sendReply(reply: String, peep: Peep) {
                            pauseSlideshow()
                            if (citizen != null && currentUserId != ""){
                                val currentTime = dateTimeManager.getCurrentTimeFormatted()
                                val chat = Chat(
                                    senderId = currentUserId,
                                    receiverId = citizen!!.uid,
                                    message = reply,
                                    timeStamp = currentTime,
                                    imageUri = peep.peepUri
                                )

                                alteViewModel.sendChat(chat) { messageSent ->
                                    if (messageSent) {
                                        alteViewModel.startChatListener(currentUserId, citizen!!)
                                        navigateToChatFragment()
                                    } else {
                                        toastManager.showShortToast(requireContext(), "Failed to send reply, try again!")
                                        resumeSlideshow()
                                    }
                                }

                            }
                        }

                        override fun deletePeep(peep: Peep) {
                            //pauseSlideshow()
                            deleteProgressBar.visibility =View.VISIBLE

                            alteViewModel.deletePeep(currentUserId, peep) { successful ->
                                if (successful) {
                                    activity?.supportFragmentManager?.popBackStack()
                                } else {
                                    resumeSlideshow()
                                }
                                deleteProgressBar.visibility = View.GONE
                            }
                        }
                    })
                viewPager.adapter = adapter

                // Start the slideshow
                handler.postDelayed(runnable, delay)
            }

            // Add a delay before starting the initial animation
            handler.postDelayed({ animatePeepProgressBar() }, 100)

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    // Update currentPage when the page is changed manually
                    currentPage = position + 1

                    // Handle the onPageSelected event, you can call animatePeepProgressBar here
                    animatePeepProgressBar()

                    // Stop the existing handler and start a new one
                    handler.removeCallbacks(runnable)
                    handler.postDelayed(runnable, delay)
                }
            })

        }
    }

    private fun navigateToChatFragment() {
        findNavController().navigate(R.id.action_peepsFragment_to_chatFragment)
    }

    private val runnable = object : Runnable {
        override fun run() {
            if (!isSlideshowPaused) {
                if (currentPage >= peepList.size) {
                    activity?.supportFragmentManager?.popBackStack()
                }
                binding.viewPager.currentItem = currentPage++
                animatePeepProgressBar()

                // Continue running the handler for the next iteration
                handler.postDelayed(this, delay)
            } else {
                // Stop the existing handler
                handler.removeCallbacks(this)
            }
        }
    }

    private fun animatePeepProgressBar() {
        // Animate the width of peepProgressBar from 0 to peepProgressBackground's width
        val peepProgressBar = binding.peepProgressBar
        val peepProgressBackground = binding.peepProgressBackground
        val animator = ValueAnimator.ofInt(0, peepProgressBackground.width)
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            peepProgressBar.layoutParams.width = value
            peepProgressBar.requestLayout()
        }

        animator.duration = delay
        animator.interpolator = LinearInterpolator()
        animator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop the slideshow when the fragment is destroyed
        handler.removeCallbacks(runnable)
    }

    private fun pauseSlideshow() {
        isSlideshowPaused = true
    }

    private fun resumeSlideshow() {
        isSlideshowPaused = false

        // Handle the onPageSelected event, you can call animatePeepProgressBar here
        animatePeepProgressBar()

        // Restart the handler
        handler.postDelayed(runnable, delay)
    }

    private fun showOverflowMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)

        // Inflate the menu resource
        popupMenu.menuInflater.inflate(R.menu.peep_overflow_menu, popupMenu.menu)

        // Set an OnMenuItemClickListener for the menu items
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.mute_peep -> {
                    //TODO mute dialog
                    true
                }

                R.id.chat_peep -> {
                    val currentUserId = alteViewModel.currentUser.value?.uid ?: ""
                    citizen?.let { alteViewModel.startChatListener(currentUserId, it) }

                    findNavController().navigate(R.id.action_peepsFragment_to_chatFragment)
                    true
                }

                R.id.view_citizen_peep -> {
                    citizen?.let {
                        alteViewModel.setSelectedCitizen(it)
                        findNavController().navigate(R.id.action_peepsFragment_to_citizenProfileFragment)
                    }
                    true
                }

                R.id.report_peep -> {
                    //TODO Report Dialog
                    true
                }

                else -> {
                    false
                }
            }
        }

        // Show the popup menu
        popupMenu.show()
    }

    companion object {
        private const val FORWARD = 1
        private const val BACKWARD = 2
        private const val PAUSE = 3
        private const val RESUME = 4
    }

}