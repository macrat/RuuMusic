package jp.blanktar.ruumusic.service


import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus


class EndpointManager : Endpoint {
    override val supported = true

    val endpoints = mutableListOf<Endpoint>()

    fun add(e: Endpoint) {
        if (e.supported) {
            endpoints.add(e)
        }
    }

    override fun close() {
        for (e in endpoints) {
            e.close()
        }
    }

    override fun onStatusUpdated(status: PlayingStatus) {
        for (e in endpoints) {
            e.onStatusUpdated(status)
        }
    }

    override fun onEqualizerInfo(info: EqualizerInfo) {
        for (e in endpoints) {
            e.onEqualizerInfo(info)
        }
    }

    override fun onMediaStoreUpdated() {
        for (e in endpoints) {
            e.onMediaStoreUpdated()
        }
    }

    override fun onFailedPlay(status: PlayingStatus) {
        for (e in endpoints) {
            e.onFailedPlay(status)
        }
    }

    override fun onError(message: String, status: PlayingStatus) {
        for (e in endpoints) {
            e.onError(message, status)
        }
    }

    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {
        for (e in endpoints) {
            e.onEndOfList(isFirst, status)
        }
    }
}
