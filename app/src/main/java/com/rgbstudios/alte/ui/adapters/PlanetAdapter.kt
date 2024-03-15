package com.rgbstudios.alte.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.databinding.ItemPlanetBinding
import com.rgbstudios.alte.viewmodel.AlteViewModel
import java.io.File.separator

@SuppressLint("NotifyDataSetChanged")
class PlanetAdapter(
    private val isUserPlanet: Boolean,
    private val context: Context,
    private val viewModel: AlteViewModel,
    private val userClickListener: UserClickListener
) :
    RecyclerView.Adapter<PlanetAdapter.PlanetViewHolder>() {

    private var list: List<UserDetails> = emptyList()
    private var isExpanded = false

    fun updatePlanetList(planetUsers: List<UserDetails>) {
        list = planetUsers
        notifyDataSetChanged()
    }

    inner class PlanetViewHolder(val binding: ItemPlanetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanetViewHolder {
        val binding = ItemPlanetBinding.inflate(LayoutInflater.from(context), parent, false)
        return PlanetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanetViewHolder, position: Int) {
        val user = list[position]
        holder.binding.apply {

            fullNamePlanet.text = user.name
            usernamePlanet.text = context.getString(R.string.user_name_template, user.username)

            Glide.with(context)
                .asBitmap()
                .load(user.avatarUri)
                .placeholder(R.drawable.user_icon)
                .into(userAvatarPlanet)

            planetUserLayout.setOnClickListener {
                userClickListener.onUserClick(user)
            }

            userAvatarPlanet.setOnClickListener {
                userClickListener.onAvatarClick(user)
            }

            extendBtn.setOnClickListener {
                toggleExpand(holder.binding)
            }

            exileTV.setOnClickListener {
                viewModel.exileCitizen(user)
            }

            ditchTV.setOnClickListener {
                viewModel.ditchFolk(user)
            }

            if (isUserPlanet) {
                extendBtn.visibility = View.VISIBLE
            } else {
                extendBtn.visibility = View.GONE
            }
        }
    }

    private fun toggleExpand(binding: ItemPlanetBinding) {
        binding.apply {
            if (isExpanded) {
                extendedLayout.visibility = View.GONE
                mainBackground.visibility = View.GONE
                extendBtn.setImageResource(R.drawable.arrow_down)

                isExpanded = false
            } else {
                extendedLayout.visibility = View.VISIBLE
                mainBackground.visibility = View.VISIBLE
                extendBtn.setImageResource(R.drawable.arrow_up)

                isExpanded = true
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface UserClickListener {
        fun onUserClick(user: UserDetails)
        fun onAvatarClick(user: UserDetails)
    }
}