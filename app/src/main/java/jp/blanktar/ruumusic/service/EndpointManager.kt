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
            try {
                e.onStatusUpdated(status)
            } catch (err: SecurityException) {
            }
        }
    }

    override fun onEqualizerInfo(info: EqualizerInfo) {
        for (e in endpoints) {
            try {
                e.onEqualizerInfo(info)
            } catch (err: SecurityException) {
            }
        }
    }

    override fun onMediaStoreUpdated() {
        for (e in endpoints) {
            try {
                e.onMediaStoreUpdated()
            } catch (err: SecurityException) {
            }
        }
    }

    override fun onFailedPlay(status: PlayingStatus) {
        for (e in endpoints) {
            try {
                e.onFailedPlay(status)
            } catch (err: SecurityException) {
            }
        }
    }

    override fun onError(message: String, status: PlayingStatus) {
        for (e in endpoints) {
            try {
                e.onError(message, status)
            } catch (err: SecurityException) {
            }
        }
    }

    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {
        for (e in endpoints) {
            try {
                e.onEndOfList(isFirst, status)
            } catch (err: SecurityException) {
            }
        }
    }
}
