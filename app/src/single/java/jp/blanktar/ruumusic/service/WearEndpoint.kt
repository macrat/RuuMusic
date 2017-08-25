package jp.blanktar.ruumusic.service

import android.content.Context

import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.RuuFile


class WearEndpoint(context: Context, controller: RuuService.Controller) : Endpoint {
    override fun close() {}

    override fun onStatusUpdated(status: PlayingStatus) {}
    override fun onEqualizerInfo(info: EqualizerInfo) {}
    override fun onMediaStoreUpdated() {}
    override fun onFailedPlay(status: PlayingStatus) {}
    override fun onError(message: String, status: PlayingStatus) {}
    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {}
}
