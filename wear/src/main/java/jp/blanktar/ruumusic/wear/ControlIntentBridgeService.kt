package jp.blanktar.ruumusic.wear


import android.app.Service
import android.content.Intent


class ControlIntentBridgeService : Service() {
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val client = RuuClient(applicationContext)

        client.onConnectedListener = {
            when (intent.action) {
                ACTION_PLAY -> client.play()
                ACTION_PAUSE -> client.pause()
                ACTION_NEXT -> client.next()
                ACTION_PREV -> client.prev()
            }

            stopSelf()

            client.disconnect()
        }

        client.connect()

        return START_NOT_STICKY;
    }

    override fun onBind(intent: Intent) = null


    companion object {
        const val ACTION_PLAY = "jp.blanktar.ruumusic.wear.PLAY"
        const val ACTION_PAUSE = "jp.blanktar.ruumusic.wear.PAUSE"
        const val ACTION_NEXT = "jp.blanktar.ruumusic.wear.NEXT"
        const val ACTION_PREV = "jp.blanktar.ruumusic.wear.PREV"
    }
}
