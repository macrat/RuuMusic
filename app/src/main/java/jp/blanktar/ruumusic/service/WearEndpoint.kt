package jp.blanktar.ruumusic.service


import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.RuuFile


class WearEndpoint : Endpoint {
    override fun close() {
    }

    override fun onStatusUpdated(status: PlayingStatus) {}
    override fun onEqualizerInfo(info: EqualizerInfo) {}
    override fun onFailedPlay(status: PlayingStatus) {}
    override fun onError(message: String, status: PlayingStatus) {}

    class Listener : WearableListenerService() {
        override fun onDataChanged(ev: DataEventBuffer) {
        }

        override fun onMessageReceived(ev: MessageEvent) {
            println(ev.data)
        }
    }
}
