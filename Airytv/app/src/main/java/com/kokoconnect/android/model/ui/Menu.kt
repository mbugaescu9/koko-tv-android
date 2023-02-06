package com.kokoconnect.android.model.ui

data class MenuItem (
    var id: Int = 0,
    //menu item name
    var name: String,
    //menu item icon (resources id)
    var icon: Int,
    //menu item description
    var description: String
)