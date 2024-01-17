package com.rgbstudios.alte.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.databinding.ItemVerseBinding
import com.rgbstudios.alte.viewmodel.AlteViewModel

class AlteVerseAdapter(private val context: Context, private val viewModel: AlteViewModel) :
    RecyclerView.Adapter<AlteVerseAdapter.AlteViewHolder>() {

    private var list: List<UserDetails> = emptyList()

    fun updateAlteVerseList(AlteUsers: List<UserDetails>) {
        list = AlteUsers
        notifyDataSetChanged()
    }

    inner class AlteViewHolder(val binding: ItemVerseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlteViewHolder {
        val binding = ItemVerseBinding.inflate(LayoutInflater.from(context), parent, false)
        return AlteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlteViewHolder, position: Int) {
        val user = list[position]
        holder.binding.apply {
            usernamePlanet.text = user.username
            fullNamePlanet.text = user.name
            userAvatarPlanet.setImageBitmap(user.avatar)

            connectButton.setOnClickListener {
                connectProgressBar.visibility = View.VISIBLE
                connectButton.text = ""
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}