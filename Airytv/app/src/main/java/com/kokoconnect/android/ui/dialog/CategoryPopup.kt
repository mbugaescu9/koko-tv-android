package com.kokoconnect.android.ui.dialog

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.*
import android.widget.PopupWindow
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.adapter.tv.GenresListAdapter
import com.kokoconnect.android.databinding.PopupCategoriesBinding
import kotlin.math.roundToInt

class CategoryPopup {
    companion object {
        private val POPUP_HEIGHT = 270.dip
        private val POPUP_WIDTH = 100.dip
    }

    private lateinit var popup: PopupWindow
    private var binding: PopupCategoriesBinding? = null

    private lateinit var activity: Activity
    private var genresAdapter: GenresListAdapter? = null
    private var onCategorySelected: ((String) -> Unit)? = null
    private var onPopupClosed: (() -> Unit)? = null


    constructor(
        activity: Activity
    ) {
        this.activity = activity
        val decorView = activity.window.decorView as ViewGroup

        val binding = PopupCategoriesBinding.inflate(LayoutInflater.from(activity), decorView, false)
        this.binding = binding

        val popup = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        this.popup = popup
    }

    fun setCategories(categories: List<String>, selectedPosition: Int): CategoryPopup {
        val listCategories = binding?.listCategories
        val genresAdapter = GenresListAdapter(
            activity,
            categories,
            selectedPosition
        )
        this.genresAdapter = genresAdapter
        listCategories?.adapter = genresAdapter
        listCategories?.isVerticalScrollBarEnabled = false
        listCategories?.divider = null
        listCategories?.setOnItemClickListener { parent, view, position, id ->
            genresAdapter.getItem(position)?.let { name ->
                onCategorySelected?.invoke(name)
                close()
            }
        }
        return this
    }

    fun setOnCategorySelected(onSelected: (String) -> Unit): CategoryPopup {
        onCategorySelected = onSelected
        return this
    }

    fun setOnPopupClosed(onClosed: () -> Unit): CategoryPopup {
        onPopupClosed = onClosed
        return this
    }

    fun showUnder(view: View) {
        val width = genresAdapter?.getMaxWidth()?.roundToInt() ?: 165.dip
        popup.isFocusable = true
        popup.width = width
        popup.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popup.contentView = binding?.root
        popup.setOnDismissListener {
            onPopupClosed?.invoke()
            onPopupClosed = null
        }
        popup.showAsDropDown(view, 4.dip, 0)
        popup.dimBehind()

        binding?.listCategories?.post {
            val selectedPosition = genresAdapter?.selectedPosition ?: 0
            binding?.listCategories?.smoothScrollToPosition(selectedPosition)
        }
    }

    fun close() {
        popup.dismiss()
        onPopupClosed?.invoke()
        onPopupClosed = null
    }
}

val Int.dip
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        AiryTvApp.instance.resources.displayMetrics
    ).toInt()

fun PopupWindow.dimBehind() {
    val container = contentView.rootView
    val context = contentView.context
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val p = container.layoutParams as WindowManager.LayoutParams
    p.flags = p.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
    p.dimAmount = 0.6f
    wm.updateViewLayout(container, p)
}