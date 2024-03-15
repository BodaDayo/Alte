package com.rgbstudios.alte.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.databinding.ItemInvitesBinding
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel

@SuppressLint("NotifyDataSetChanged")
class InvitesAdapter(private val context: Context, private val viewModel: AlteViewModel) :
    RecyclerView.Adapter<InvitesAdapter.InvitesViewHolder>() {

    private var list: List<UserDetails> = emptyList()
    private val toastManager = ToastManager()

    fun updateInvitesList(AlteUsers: List<UserDetails>) {
        list = AlteUsers
        notifyDataSetChanged()
    }

    inner class InvitesViewHolder(val binding: ItemInvitesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitesViewHolder {
        val binding = ItemInvitesBinding.inflate(LayoutInflater.from(context), parent, false)
        return InvitesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InvitesViewHolder, position: Int) {
        val user = list[position]
        holder.binding.apply {
            val currentUser = viewModel.currentUser.value

            fullNameInvites.text = user.name
            usernameInvites.text = context.getString(R.string.user_name_template, user.username)

            Glide.with(context)
                .asBitmap()
                .load(user.avatarUri)
                .placeholder(R.drawable.user_icon)
                .into(userAvatarInvites)

            revokeTextTV.setOnClickListener {
                if (currentUser != null) {
                    viewModel.withdrawRequest(user.uid, currentUser.uid) { isSuccessful, _ ->
                        if (isSuccessful) {
                            toastManager.showLongToast(context, "Connection request withdrawn")
                        } else {
                            toastManager.showLongToast(
                                context,
                                "Request withdrawal failed"
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