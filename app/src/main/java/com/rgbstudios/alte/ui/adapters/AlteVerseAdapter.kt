package com.rgbstudios.alte.ui.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.databinding.ItemVerseBinding
import com.rgbstudios.alte.utils.AvatarManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel

class AlteVerseAdapter(private val context: Context, private val viewModel: AlteViewModel) :
    RecyclerView.Adapter<AlteVerseAdapter.AlteViewHolder>() {

    private var list: List<UserDetails> = emptyList()
    private val toastManager = ToastManager()

    fun updateAlteVerseList(AlteUsers: List<UserDetails>) {
        list = AlteUsers
        notifyDataSetChanged()
    }

    inner class AlteViewHolder(val binding: ItemVerseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlteViewHolder {
        val binding = ItemVerseBinding.inflate(LayoutInflater.from(context), parent, false)
        return AlteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlteViewHolder, position: Int) {
        val user = list[position]
        holder.binding.apply {
            val currentUser = viewModel.currentUser.value

            usernameVerse.text = user.username
            fullNameVerse.text = user.name

            Glide.with(context)
                .asBitmap()
                .load(user.avatarUri)
                .placeholder(R.drawable.user_icon)
                .into(userAvatarVerse)


            connectButton.setOnClickListener {
                if (currentUser != null) {
                    connectProgressBar.visibility = View.VISIBLE
                    connectButton.text = ""

                    viewModel.sendConnectRequest(currentUser.uid, user.uid) { successful ->
                        if (successful) {
                            toastManager.showShortToast(context, "Connect request sent!")
                        } else {
                            toastManager.showShortToast(context, "Something went wrong try again!")
                        }
                    }
                }
            }

            // Check if user.uid is in the current user's invites list
            val isInvited = currentUser?.invites?.contains(user.uid) == true

            if (isInvited) {
                connectProgressBar.visibility = View.GONE
                connectButton.visibility = View.GONE
                sentTextTV.visibility = View.VISIBLE
            } else {
                connectProgressBar.visibility = View.GONE
                sentTextTV.visibility = View.GONE
                connectButton.text = context.resources.getString(R.string.connect)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}