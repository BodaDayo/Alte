package com.rgbstudios.alte.ui.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rgbstudios.alte.data.model.Peep
import com.rgbstudios.alte.databinding.ItemPagerBinding
import com.rgbstudios.alte.utils.DialogManager
import com.rgbstudios.alte.viewmodel.AlteViewModel

class SlideshowPagerAdapter(
    private val viewModel: AlteViewModel,
    private val citizenIsCurrentUser: Boolean,
    private val fragment: Fragment,
    private val context: Context,
    private val peepList: List<Peep>,
    private val dialogManager: DialogManager,
    private val pagerClickListener: PagerClickListener
) : RecyclerView.Adapter<SlideshowPagerAdapter.PagerViewHolder>() {

    inner class PagerViewHolder(val binding: ItemPagerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val binding = ItemPagerBinding.inflate(LayoutInflater.from(context), parent, false)
        return PagerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        val peep = peepList[position]
        holder.binding.apply {

            Glide.with(context)
                .load(peep.peepUri)
                .into(imageViewPagerItem)

            peep.caption.let {
                if (it.isNotEmpty()) {
                    if (it.length > 52) {
                        // If the caption is longer than 52 characters, truncate it and add ellipsis
                        val truncatedCaption = it.substring(0, 49) + "..."
                        peepCaption.text = truncatedCaption

                    } else {
                        peepCaption.text = it
                    }

                    peepCaptionExpanded.text = it

                    peepCaption.visibility =
                        View.VISIBLE
                    peepCaptionExpanded.visibility =
                        View.GONE
                }
            }

            sendReply.setOnClickListener {
                val reply = replyPeep.text.toString().trim()
                pagerClickListener.sendReply(reply, peep)
            }

            nextBtn.setOnClickListener {
                pagerClickListener.performAction(FORWARD)
            }

            previousBtn.setOnClickListener {
                pagerClickListener.performAction(BACKWARD)
            }

            imageViewPagerItem.setOnLongClickListener {
                pagerClickListener.performAction(PAUSE)
                true
            }

            imageViewPagerItem.setOnClickListener {
                pagerClickListener.performAction(RESUME)
            }

            peepCaption.setOnClickListener {
                peepCaption.visibility = View.GONE
                peepCaptionExpanded.visibility = View.VISIBLE

                pagerClickListener.performAction(PAUSE)
            }

            peepCaptionExpanded.setOnClickListener {
                peepCaption.visibility = View.VISIBLE
                peepCaptionExpanded.visibility = View.GONE

                pagerClickListener.performAction(RESUME)
            }

            replyPeep.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Not needed for your case
                }

                override fun onTextChanged(
                    charSequence: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    // Called when text is being typed
                    if (count > 0) {
                        // Text is being entered, so pause the slideshow
                        pagerClickListener.performAction(3)
                    }
                }

                override fun afterTextChanged(editable: Editable?) {
                    // Not needed for your case
                }
            })

            peepDetails.setOnClickListener {
                pagerClickListener.performAction(PAUSE)

                val allUsers = viewModel.allUsersList.value
                val viewedList: List<String> = peep.viewList.map { uid ->
                    allUsers?.find { it.uid == uid }?.username.orEmpty()
                }

                dialogManager.showPeepDetailDialog(
                    viewedList,
                    fragment
                ) { choice ->
                    when (choice) {
                        1 -> pagerClickListener.performAction(FORWARD)
                        2 -> pagerClickListener.deletePeep(peep)
                        else -> pagerClickListener.performAction(RESUME)
                    }
                }
            }

            if (citizenIsCurrentUser) {

                viewCount.text = peep.viewList.size.toString()

                peepDetails.visibility = View.VISIBLE
                replyLayout.visibility = View.GONE
            } else {
                peepDetails.visibility = View.GONE
                replyLayout.visibility = View.VISIBLE

                viewModel.updatePeepViewed(peep)
            }

        }
    }

    override fun getItemCount(): Int {
        return peepList.size
    }

    interface PagerClickListener {
        fun performAction(action: Int)

        fun sendReply(reply: String, peep: Peep)

        fun deletePeep(peep: Peep)
    }

    companion object {
        private const val FORWARD = 1
        private const val BACKWARD = 2
        private const val PAUSE = 3
        private const val RESUME = 4
    }

}
