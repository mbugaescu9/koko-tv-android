package com.kokoconnect.android.model.notification

class Notifications {
    var notifications: List<Notification> = emptyList()
}

class Notification {
    var id: Int = 0
    var text: String = ""
    var name = ""
}

class NotificationMessage {
    var message = ""
}