package com.kokoconnect.android.model.deeplink

import android.content.Intent
import android.net.Uri
import com.kokoconnect.android.model.tv.Channel
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.util.WEBAPP_DOMAIN
import com.kokoconnect.android.util.resumeWithSafe
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

class DeeplinkManager {
    companion object {
        const val DYNAMIC_LINK_URL = "https://airytv.page.link"
        const val SHARE_LINK_PATH = "/sharelink"
    }


    fun createShareChannelLink(channel: Channel?): Uri? {
        channel ?: return null
        val uriPrefix = "${DYNAMIC_LINK_URL}/"
        val category = channel.category?.replace(" ", "_")
        val channelNumber = channel.number.toString()
        val channelName = channel.name.replace(" ", "_")
        val channelDescription = channel.description
        val channelNameOutput = channelName.replace("_", " ")
        val webAppLink = "https://${WEBAPP_DOMAIN}/${category}/${channelNumber}_${channelName}"
        val dynamicLink = Firebase.dynamicLinks.createDynamicLink()
            .setDomainUriPrefix(uriPrefix)
            .setLink(Uri.parse(webAppLink))
            // Open links with this app on Android
            .setAndroidParameters(
                DynamicLink.AndroidParameters.Builder()
                    .setFallbackUrl(Uri.parse(AppParams.appStoreUrl))
                    .build()
            )
            .setSocialMetaTagParameters(
                DynamicLink.SocialMetaTagParameters.Builder()
                    .setTitle(channelNameOutput)
                    .setDescription("Watch free channel ${channelNameOutput}! ${channelDescription}")
                    .build()
            )
            .buildDynamicLink()
        Timber.d("createShareChannelLink() ${dynamicLink.uri}")
        return dynamicLink.uri
    }

    fun createShareContentLink(content: Content?): Uri? {
        content ?: return null

        val uriPrefix = "${DYNAMIC_LINK_URL}/"
        val type = content.type ?: return null
        val name = content.name ?: return null
        val description = content.description ?: ""
        val id = content.id ?: return null
        val posterUrl = content.poster?.getUrl()
        val webAppLink = "https://${WEBAPP_DOMAIN}/content?type=$type&name=$name&id=$id"
        val dynamicLink = Firebase.dynamicLinks.createDynamicLink()
            .setDomainUriPrefix(uriPrefix)
            .setLink(Uri.parse(webAppLink))
            // Open links with this app on Android
            .setAndroidParameters(
                DynamicLink.AndroidParameters.Builder()
                    .setFallbackUrl(Uri.parse(AppParams.appStoreUrl))
                    .build()
            )
            .setSocialMetaTagParameters(
                DynamicLink.SocialMetaTagParameters.Builder().apply {
                    setTitle(name)
                    setDescription("Watch free ${type} ${name}! ${description}")
                    posterUrl?.let {
                        setImageUrl(Uri.parse(it))
                    }
                }.build()
            )
            .buildDynamicLink()
        Timber.d("createShareContentLink() ${dynamicLink.uri}")
        return dynamicLink.uri
    }


    fun isShareChannelLink(url: Uri?): Boolean {
        url ?: return false
        val isCorrectPathSize = url.pathSegments.size >= 2
        val channelNumberName = url.pathSegments.getOrNull(url.pathSegments.size - 1)
        val channelCategory = url.pathSegments.getOrNull(url.pathSegments.size - 2)
        val isShareChannelLink = isCorrectPathSize && channelNumberName != null && channelCategory != null
        Timber.d("isShareChannelLink() ${isShareChannelLink}")
        return isShareChannelLink
    }

    fun isShareContentLink(url: Uri?): Boolean {
        url ?: return false
        val isContentPath = url.lastPathSegment.equals("content")
        val hasParams = url.getQueryParameter("id") != null
                && url.getQueryParameter("name") != null
                && url.getQueryParameter("type") != null
        val isShareContentLink = isContentPath && hasParams
        Timber.d("isShareContentLink() ${isShareContentLink}")
        return isShareContentLink
    }

    suspend fun fetchDeeplinkData(intent: Intent?): DeeplinkData? {
        intent ?: return null
        return suspendCancellableCoroutine<DeeplinkData?> { continuation ->
            Firebase.dynamicLinks
                .getDynamicLink(intent)
                .addOnSuccessListener { pendingDynamicLinkData ->
                    val deepLink: Uri? = pendingDynamicLinkData?.link
                    Timber.d("fetchDeeplinkData() deeplink is received ${deepLink}")
                    val data = getDataFromLink(deepLink)
                    continuation.resumeWithSafe(Result.success(data))
                }
                .addOnFailureListener { e ->
                    Timber.e("fetchDeeplinkData() deeplink is not received")
                    Timber.e(e)
                    continuation.resumeWithSafe(Result.success(null))
                }
        }
    }

    fun getDataFromLink(url: Uri?): DeeplinkData? {
        url ?: return null
        return when {
            isShareContentLink(url) -> {
                val contentId = url.getQueryParameter("id")?.toLongOrNull() ?: return null
                val contentName = url.getQueryParameter("name") ?: return null
                val contentType = url.getQueryParameter("type") ?: return null
                return ContentDeeplinkData(
                    contentId = contentId,
                    contentName = contentName,
                    contentType = contentType
                )
            }
            isShareChannelLink(url) -> {
                val channelNumberName = url.pathSegments
                    .getOrNull(url.pathSegments.size - 1)
                    ?.replace("_", " ") ?: return null
                val channelCategory = url.pathSegments
                    .getOrNull(url.pathSegments.size - 2)
                    ?.replace("_", " ") ?: return null
                var channelNumber: Int? = null
                var channelName: String? = null
                val indexOfDivider = channelNumberName?.indexOf(" ")
                if (indexOfDivider in channelNumberName.indices) {
                    channelNumber = channelNumberName.substring(0, indexOfDivider).toIntOrNull()
                    channelName =
                        channelNumberName.substring(indexOfDivider + 1, channelNumberName.length)
                }
                Timber.d("getDataFromLink() channel number ${channelNumber} channel name ${channelName} category ${channelCategory}")
                channelNumber ?: return null
                channelName ?: return null
                ChannelDeeplinkData(
                    category = channelCategory,
                    channelNumber = channelNumber,
                    channelName = channelName
                )
            }
            else -> {
                null
            }
        }
    }
}