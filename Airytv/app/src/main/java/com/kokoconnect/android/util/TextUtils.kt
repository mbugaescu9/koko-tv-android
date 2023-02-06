package com.kokoconnect.android.util

object TextUtils {
    val priceValueRegex = Regex("([0-9]+((\\.|\\,)[0-9]*)?)")
    val priceCurrencyRegex = Regex("(\\\$)")
    val priceRegex = Regex("(\\\$\\s?[0-9]+((\\.|\\,)[0-9]*)?)|(([0-9]+((\\.|\\,)[0-9]*)?)\\s?\\\$)")

    fun getPricesStringsFrom(text: String): List<String> {
        return priceRegex.findAll(text).map { it.value }.toList()
    }

    fun getPriceValueString(text: String): String? {
        return priceValueRegex.find(text)?.value
    }

    fun getPriceCurrency(text: String): String? {
        return priceCurrencyRegex.find(text)?.value
    }
}