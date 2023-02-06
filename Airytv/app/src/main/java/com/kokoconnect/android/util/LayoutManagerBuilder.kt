package com.kokoconnect.android.util

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LayoutManagerBuilder() {
    enum class Type{
        LINEAR, GRID
    }

    var managerType: Type = Type.LINEAR
    var orientation: Int = RecyclerView.VERTICAL
    var reverseLayout: Boolean = false
    var spanCount = 2

    fun build(context: Context?): RecyclerView.LayoutManager {
        return when(managerType){
            Type.LINEAR -> {
                LinearLayoutManager(context, orientation, reverseLayout)
            }
            Type.GRID -> {
                GridLayoutManager(context, spanCount)
            }
        }
    }

}