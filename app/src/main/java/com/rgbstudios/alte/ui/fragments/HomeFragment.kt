package com.rgbstudios.alte.ui.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.databinding.FragmentHomeBinding
import com.rgbstudios.alte.ui.adapters.PeepAdapter
import com.rgbstudios.alte.utils.AvatarManager
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.viewmodel.AlteViewModelFactory

class HomeFragment : Fragment() {
    private val firebase = FirebaseAccess()

    private val alteViewModel: AlteViewModel by viewModels {
        AlteViewModelFactory(requireActivity().application, AlteRepository(firebase))
    }

    private val auth = firebase.auth
    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private val avatarManager = AvatarManager()
    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

            // Set up the planet adapter
            val peepAdapter = PeepAdapter(requireContext(), alteViewModel)

            // Set up the planetRecyclerView to scroll horizontally
            peepRecyclerView.setHasFixedSize(true)
            peepRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            peepRecyclerView.adapter = peepAdapter

            val peepList = avatarManager.defaultAvatarsList
            val peeps = convertResourceIdsToBitmaps(peepList)

            peepAdapter.updatePeeps(peeps)

            showAnim.setOnClickListener {
                texty.visibility = View.GONE
                texty2.visibility = View.GONE
                showAnim.visibility = View.GONE
                lottieAnimationView.visibility = View.VISIBLE
                closer.visibility = View.VISIBLE
            }

            closer.setOnClickListener {
                texty.visibility = View.VISIBLE
                texty2.visibility = View.VISIBLE
                showAnim.visibility = View.VISIBLE

                lottieAnimationView.visibility = View.GONE
                closer.visibility = View.GONE
            }

            moreOptions.setOnClickListener { view ->
                showOverflowMenu(view)
            }

            fab.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_alteVerseFragment)
            }

            logoutBtn.setOnClickListener {
                alteViewModel.logOut { logOutSuccessful, errorMessage ->

                    if (logOutSuccessful) {
                        findNavController().navigate(R.id.action_homeFragment_to_signInFragment)

                    } else {

                        errorMessage?.let { message ->
                            val output = message.substringAfter(": ")
                            toastManager.showLongToast(requireContext(), output)
                        }
                    }
                }
            }
        }
    }

    private fun showOverflowMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)

        // Inflate the menu resource
        popupMenu.menuInflater.inflate(R.menu.home_overflow_menu, popupMenu.menu)

        // Set an OnMenuItemClickListener for the menu items
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.new_circle -> {

                    true
                }

                R.id.new_broadcast -> {
                    true
                }

                R.id.starred_messages -> {
                    true
                }

                R.id.settings -> {
                    true
                }

                else -> false
            }
        }

        // Show the popup menu
        popupMenu.show()
    }
    fun convertResourceIdsToBitmaps(resourceIds: List<Int>): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()

        for (resourceId in resourceIds) {
            val bitmap = BitmapFactory.decodeResource(resources, resourceId)
            bitmaps.add(bitmap)
        }

        return bitmaps
    }
}