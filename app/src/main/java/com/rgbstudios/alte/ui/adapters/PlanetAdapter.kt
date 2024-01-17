package com.rgbstudios.alte.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.databinding.ItemPlanetBinding
import com.rgbstudios.alte.viewmodel.AlteViewModel

class PlanetAdapter(private val context: Context, private val viewModel: AlteViewModel) :
    RecyclerView.Adapter<PlanetAdapter.PlanetViewHolder>() {

    private var list: List<UserDetails> = emptyList()

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
            usernamePlanet.text = user.username
            fullNamePlanet.text = user.name
            userAvatarPlanet.setImageBitmap(user.avatar)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}