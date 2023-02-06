package com.kokoconnect.android.model.ads.banner

class PubdeskCharIterator {
    val chars = listOf<Char>('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j')
    var iterator = chars.listIterator()

    fun getChar(): Char {
        return if (iterator.hasNext()) {
            iterator.next()
        } else {
            iterator = chars.listIterator()
            iterator.next()
        }
    }

}