package jp.blanktar.ruumusic.service


import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus


interface Endpoint {
    val supported: Boolean

    fun close()

    fun onStatusUpdated(status: PlayingStatus)
    fun onEqualizerInfo(info: EqualizerInfo)
    fun onMediaStoreUpdated()

    fun onFailedPlay(status: PlayingStatus)
    fun onError(message: String, status: PlayingStatus)
    fun onEndOfList(isFirst: Boolean, status: PlayingStatus)
}
