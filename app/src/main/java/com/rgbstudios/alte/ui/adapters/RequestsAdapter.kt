package com.rgbstudios.alte.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.databinding.ItemRequestsBinding
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel

@SuppressLint("NotifyDataSetChanged")
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

            fullNameRequests.text = user.name
            usernameRequests.text = context.getString(R.string.user_name_template, user.username)

            Glide.with(context)
                .asBitmap()
                .load(user.avatarUri)
                .placeholder(R.drawable.user_icon)
                .into(userAvatarRequests)

            acceptTV.setOnClickListener {
                if (currentUser != null) {
                    viewModel.acceptRequest(currentUser.uid, user.uid) { isSuccessful, _ ->
                        if (isSuccessful) {
                            toastManager.showLongToast(context, "Connection request accepted")
                        } else {
                            toastManager.showLongToast(
                                context,
                                "Acceptance failed"
                            )
                        }
                        viewModel.startUserListListener()
                    }
                }
            }

            declineTV.setOnClickListener {
                if (currentUser != null) {
                    viewModel.declineRequest(currentUser.uid, user.uid) { isSuccessful, _ ->
                        if (isSuccessful) {
                            toastManager.showLongToast(context, "Connection request declined")
                        } else {
                            toastManager.showLongToast(
                                context,
                                "Decline failed"
                            )
                        }
                        viewModel.startUserListListener()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}