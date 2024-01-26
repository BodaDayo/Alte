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
import com.rgbstudios.alte.databinding.ItemRequestsBinding
import com.rgbstudios.alte.databinding.ItemVerseBinding
import com.rgbstudios.alte.utils.AvatarManager
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel

class RequestsAdapter(private val context: Context, private val viewModel: AlteViewModel) :
    RecyclerView.Adapter<RequestsAdapter.RequestsViewHolder>() {

    private var list: List<UserDetails> = emptyList()
    private val toastManager = ToastManager()

    fun updateRequestList(AlteUsers: List<UserDetails>) {
        list = AlteUsers
        notifyDataSetChanged()
    }

    inner class RequestsViewHolder(val binding: ItemRequestsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestsViewHolder {
        val binding = ItemRequestsBinding.inflate(LayoutInflater.from(context), parent, false)
        return RequestsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestsViewHolder, position: Int) {
        val user = list[position]
        holder.binding.apply {
            val currentUser = viewModel.currentUser.value

            usernameRequests.text = user.username
            fullnameRequestsTV.text = user.status.replaceFirstChar { it.uppercase() }

            Glide.with(context)
                .asBitmap()
                .load(user.avatarUri)
                .placeholder(R.drawable.user_icon)
                .into(userAvatarRequests)

            acceptTV.setOnClickListener {
                if (currentUser != null) {
                    viewModel.acceptRequest(currentUser.uid, user.uid) {
                        toastManager.showLongToast(context, "Connection request accepted")
                    }
                }
            }

            declineTV.setOnClickListener {
                if (currentUser != null) {
                    viewModel.declineRequest(currentUser.uid, user.uid) {
                        toastManager.showLongToast(context, "Connection request declined")
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}