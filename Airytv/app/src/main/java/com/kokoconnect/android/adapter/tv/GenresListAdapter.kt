package com.kokoconnect.android.adapter.tv

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.kokoconnect.android.R
import com.kokoconnect.android.util.ActivityUtils
import com.kokoconnect.android.vh.pxToDp
import com.kokoconnect.android.vh.spToPx
import org.jetbrains.anko.colorAttr
import timber.log.Timber
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class GenresListAdapter(
    private val ctx: Context,
    items: List<String>,
    positionInGenre: Int
) : BaseAdapter() {
    private val inflater = LayoutInflater.from(ctx)
    private val textSize = ActivityUtils.getDimensionFromAttr(ctx, R.attr.genreListTextSize).toFloat()

    var items: List<String>? by Delegates.observable(items) { _: KProperty<*>, _: List<String>?, newValue: List<String>? ->
        notifyDataSetChanged()
    }
    val selectedPosition: Int by Delegates.observable(
        positionInGenre
    ) { _: KProperty<*>, old: Int, new: Int ->
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.item_spinner, parent, false)
        val color = if (position == selectedPosition) {
            view.colorAttr(R.attr.colorAccent)
        } else {
            view.colorAttr(R.attr.colorOnSurfaceVariant2)
        }
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.setTextColor(color)
        textView.setText(getItem(position))
        textView.setTextSize(textSize)
        return view
    }

    fun getMaxWidth(): Float {
        val items = items ?: return 0f
        if (items.isEmpty()) return 0f
        val maxLength = items.maxByOrNull { it.length } ?.length ?: 0
        val width = ((textSize.spToPx() + 20) * maxLength).pxToDp()
        Timber.d("getMaxWidth() maxLength ${maxLength} width ${width} textSize ${textSize}")
        return width
    }

    override fun getItem(position: Int): String? = items?.getOrNull(position)

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = items?.size ?: 0

}