package com.github.libretube.obj.update

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class UpdateAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0
) : Parcelable

@Serializable
@Parcelize
data class UpdateInfo(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<UpdateAsset> = emptyList()
) : Parcelable
