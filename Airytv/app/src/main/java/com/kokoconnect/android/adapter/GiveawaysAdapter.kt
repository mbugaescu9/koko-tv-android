package com.kokoconnect.android.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.ItemGiveawaysCardBinding
import com.kokoconnect.android.model.giveaways.GiveawaysItem
import com.kokoconnect.android.util.DateUtils
import com.kokoconnect.android.util.TextUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import timber.log.Timber

class GiveawaysAdapter(
    var listener: Listener? = null
) : RecyclerView.Adapter<GiveawaysAdapter.GiveawaysItemViewHolder>() {

    var items: List<GiveawaysItem> = emptyList()
        set(value) {
            if (value.isNotEmpty()) {
                field = value
                notifyDataSetChanged()
            }
        }

    fun updateAll() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GiveawaysItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemGiveawaysCardBinding.inflate(inflater)
        return GiveawaysItemViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: GiveawaysItemViewHolder, position: Int) {
        items.getOrNull(position)?.let {
            holder.bind(it)
        }
    }

    inner class GiveawaysItemViewHolder(val binding: ItemGiveawaysCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GiveawaysItem) {
            val context = binding.root.context
            val parsedDate = DateUtils.parseIsoDate(item.date)
            val formattedDate = DateUtils.formatDate(parsedDate, "MM/dd/yyyy")
//            val formatTime = DateUtils.formatDate(parsedDate, "hh:mma")
            var cardName = item.name ?: ""
            var cardPrice = ""
            TextUtils.getPricesStringsFrom(cardName).forEach {
                cardName = cardName.replace(it, "").trim()
                val currency = TextUtils.getPriceCurrency(it)
                val priceValue = TextUtils.getPriceValueString(it)
                cardPrice = "$priceValue$currency"
            }
            val cardEntered = item.entries ?: 0
            val cardUserEntered = item.userEntries ?: 0
            val cardEnteredString = cardEntered.toString()
            val cardImageUrl = item.imageUrl
            val cardWinnerAlias = item.aliasWinner ?: ""
            val isActive = item.isActive ?: true
            val isWinnerVisible = item.isWinnerVisible ?: false
            val isCardCodeVisible = item.isCardCodeVisible ?: false

            Timber.d("bind() card name ${cardName} card image ${cardImageUrl}")
            when {
                isActive -> {
                    binding.rlGiftCardCurrent.visibility = View.VISIBLE
                    binding.rlGiftCardEnded.visibility = View.GONE
                    binding.sivGiftCardWinnerAvatar.visibility = View.GONE
                    binding.flGiftCardWinnerAvatar.visibility = View.GONE
                    binding.rlGiftCardCode.visibility = View.GONE
                    binding.sivGiftCardImage.visibility = View.VISIBLE

                    binding.tvGiftCardTitle.text = cardName
                    binding.tvGiftCardText.text = formattedDate
                    binding.tvGiftCardPrice.text = cardPrice
                    binding.tvTicketsEnteredCount.text = cardEnteredString
                    binding.btnGiftCardAddTicket.setOnClickListener {
                        listener?.onAddTicket(item)
                    }
                    if (cardImageUrl != null) {
                        Glide.with(context)
                            .load(cardImageUrl)
                            .placeholder(R.drawable.giveaways_gift_placeholder)
                            .into(binding.sivGiftCardImage)
                    }
                    binding.pbTicketsEntered.max = cardEntered
                    binding.pbTicketsEntered.progress = cardUserEntered
                }
                isWinnerVisible -> {
                    binding.rlGiftCardCurrent.visibility = View.GONE
                    binding.rlGiftCardEnded.visibility = View.GONE
                    binding.sivGiftCardImage.visibility = View.GONE
                    binding.rlGiftCardCode.visibility = View.GONE
                    binding.sivGiftCardWinnerAvatar.visibility = View.VISIBLE
                    binding.flGiftCardWinnerAvatar.visibility = View.VISIBLE

                    binding.tvGiftCardTitle.text = context.getString(
                        R.string.congratulations_user,
                        cardWinnerAlias
                    )
                    binding.tvGiftCardText.text = context.getString(
                        R.string.please_check_you_profile_code
                    )
                    binding.tvGiftCardPrice.text = cardPrice
                    val userAvatarUrl = listener?.getUserAvatarUrl()
                    if (userAvatarUrl != null) {
                        Glide.with(context)
                            .load(userAvatarUrl)
                            .placeholder(R.drawable.ic_giveaway_user_avatar)
                            .into(binding.sivGiftCardImage)
                    }
                }
                isCardCodeVisible -> {
                    binding.rlGiftCardCurrent.visibility = View.GONE
                    binding.sivGiftCardWinnerAvatar.visibility = View.GONE
                    binding.flGiftCardWinnerAvatar.visibility = View.GONE
                    binding.rlGiftCardEnded.visibility = View.GONE
                    binding.sivGiftCardImage.visibility = View.VISIBLE
                    binding.rlGiftCardCode.visibility = View.VISIBLE

                    val cardCode = context.getString(R.string.gift_card_code, item.cardCode) ?: ""

                    binding.tvGiftCardTitle.text = cardName
                    binding.tvGiftCardText.text = formattedDate
                    binding.tvGiftCardPrice.text = cardPrice
                    binding.tvGiftCardCode.text = cardCode

                    if (cardImageUrl != null) {
                        Glide.with(context)
                            .load(cardImageUrl)
                            .placeholder(R.drawable.giveaways_gift_placeholder)
                            .into(binding.sivGiftCardImage)
                    }
                }
                else -> {
                    binding.rlGiftCardCurrent.visibility = View.GONE
                    binding.sivGiftCardWinnerAvatar.visibility = View.GONE
                    binding.flGiftCardWinnerAvatar.visibility = View.GONE
                    binding.rlGiftCardCode.visibility = View.GONE
                    binding.rlGiftCardEnded.visibility = View.VISIBLE
                    binding.sivGiftCardImage.visibility = View.VISIBLE

                    binding.tvGiftCardTitle.text = cardName
                    binding.tvGiftCardText.text = formattedDate
                    binding.tvGiftCardPrice.text = cardPrice
                    binding.btnGiftCardViewWinner.setOnClickListener {
                        listener?.onViewWinner(item)
                    }

                    if (cardImageUrl != null) {
                        Glide.with(context)
                            .asBitmap()
                            .placeholder(R.drawable.giveaways_gift_placeholder)
                            .load(cardImageUrl)
                            .apply(RequestOptions.fitCenterTransform())
                            .into(binding.sivGiftCardImage)
                    }
                }
            }
        }
    }

    interface Listener {
        fun onAddTicket(item: GiveawaysItem)
        fun onViewWinner(item: GiveawaysItem)
        fun getUserAvatarUrl(): String?
    }

}