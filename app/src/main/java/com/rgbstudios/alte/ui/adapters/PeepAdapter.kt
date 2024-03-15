package com.rgbstudios.alte.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.Peep
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.databinding.ItemPeepBinding
import com.rgbstudios.alte.utils.OvalStrokeCounterView

@SuppressLint("NotifyDataSetChanged")
class PeepAdapter(
    private val context: Context,
    private val peepClickListener: PeepClickListener
) :
    RecyclerView.Adapter<PeepAdapter.AlteViewHolder>() {

    private var list: List<Triple<UserDetails, List<Peep>, String>> = emptyList()
    private val firebase = FirebaseAccess()

    fun updatePeeps(peeps: List<Triple<UserDetails, List<Peep>, String>>) {
        list = peeps
        notifyDataSetChanged()
    }

    inner class AlteViewHolder(val binding: ItemPeepBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlteViewHolder {
        val binding = ItemPeepBinding.inflate(LayoutInflater.from(context), parent, false)
        return AlteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlteViewHolder, position: Int) {
        val peep = list[position]

        holder.binding.apply {
            val userDetails = peep.first
            val listSize = peep.second.size
            val userPeeps = peep.second
            val latestPeep = userPeeps.lastOrNull()?.peepUri
            val currentUsername = peep.third

            try {

                userDetails.username.let {

                    if (currentUsername == it) {
                        userNamePeep.text = context.getString(R.string.your_peep)

                        if (list.size > 1) {
                            myPeepSeparator.visibility = View.VISIBLE
                        } else {
                            myPeepSeparator.visibility = View.GONE
                        }
                    } else {
                        userNamePeep.text = it
                    }
                }

                Glide.with(context)
                    .asBitmap()
                    .load(latestPeep)
                    .into(imagePeep)

                // Dynamically set the number of dashes for the dashedBorder
                val dashedBorderDrawable =
                    OvalStrokeCounterView(context, listSize)
                peepCounterView.background = dashedBorderDrawable

            } catch (e: Exception) {
                // Handle exceptions if needed
                firebase.recordCaughtException(e)
            }

            peepLayout.setOnClickListener {
                peepClickListener.onPeepClick(Pair(userDetails, userPeeps))
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface PeepClickListener {
        fun onPeepClick(userPeepPair: Pair<UserDetails, List<Peep>>)
    }
}