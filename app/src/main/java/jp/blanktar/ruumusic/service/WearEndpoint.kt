package jp.blanktar.ruumusic.service


import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.RuuFile


class WearEndpoint : Endpoint {
    override fun close() {}

    override fun onStatusUpdated(status: PlayingStatus) {}
    override fun onEqualizerInfo(info: EqualizerInfo) {}
    override fun onFailedPlay(status: PlayingStatus) {}
    override fun onError(message: String, status: PlayingStatus) {}


    class Listener : WearableListenerService() {
        init {
            println("initial")
        }
        
        override fun onCreate() {
            super.onCreate()
            println("on create")
        }

        override fun onMessageReceived(ev: MessageEvent) {
            println("on received")
            println(ev.path)
            println(ev.data)
        }
    }
}
