package com.kokoconnect.android.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.ItemTransactionBinding
import com.kokoconnect.android.databinding.ItemTransactionHeaderBinding
import com.kokoconnect.android.model.giveaways.TransactionItem
import com.kokoconnect.android.model.giveaways.TransactionItemData
import com.kokoconnect.android.model.giveaways.TransactionItemHeader
import com.kokoconnect.android.model.giveaways.TransactionItemType
import com.kokoconnect.android.util.DateUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlin.math.absoluteValue

class TransactionsAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: List<TransactionItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return items.getOrNull(position)?.type?.ordinal
            ?: TransactionItemType.TRANSACTION_DATA.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val type = TransactionItemType.values().find {
            it.ordinal == viewType
        } ?: TransactionItemType.TRANSACTION_DATA
        val inflater = LayoutInflater.from(parent.context)
        return when(type) {
            TransactionItemType.TRANSACTION_DATA -> {
                val binding = ItemTransactionBinding.inflate(inflater)
                TransactionDataViewHolder(binding)
            }
            else -> {
                val binding = ItemTransactionHeaderBinding.inflate(inflater)
                TransactionHeaderViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items.getOrNull(position)
        if (item is TransactionItemData && holder is TransactionDataViewHolder) {
            holder.bind(item, position)
        } else if (item is TransactionItemHeader && holder is TransactionHeaderViewHolder) {
            holder.bind(item, position)
        }
    }

}


class TransactionDataViewHolder(
    val binding: ItemTransactionBinding
): RecyclerView.ViewHolder(binding.root) {
    companion object {
        const val DATE_FORMAT = "MM/dd/yy"
    }

    fun bind(item: TransactionItemData, position: Int) {
        val context = binding.root.context

        val transactionIcon = if (item.transaction.amount < 0) {
            ContextCompat.getDrawable(context, R.drawable.ic_giveaways_ticket_spent)
        } else {
            ContextCompat.getDrawable(context, R.drawable.shape_giveaways_ticket_gained)
        }
        val amountAbsolute = item.transaction.amount.absoluteValue
        val transactionText = if (item.transaction.amount < 0) {
            context.getString(R.string.spent_ticket_on_giveaway, amountAbsolute.toString())
        } else {
            context.getString(R.string.gained_ticket_for_watching_ads, amountAbsolute.toString())
        }
        val transactionDateMillis = DateUtils.parseIsoDate(item.transaction.date)
        val transactionDateFormatted = DateUtils.formatDate(transactionDateMillis, DATE_FORMAT)

        binding.tvTransactionName.text = transactionText
        binding.ivTransactionIcon.setImageDrawable(transactionIcon)
        binding.tvTransactionDate.text = transactionDateFormatted
    }
}


class TransactionHeaderViewHolder(
    val binding: ItemTransactionHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TransactionItemHeader, position: Int) {
        item.profile.avatar?.let { avatarUrl ->
            Glide.with(binding.root)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_profile_avatar_placeholder)
                .addListener(object: RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.vAvatarBackground.isVisible = true
                        return false
                    }
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.vAvatarBackground.isVisible = false
                        return false
                    }
                })
                .into(binding.sivAvatar)
        }
    }
}