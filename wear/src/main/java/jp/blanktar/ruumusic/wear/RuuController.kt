package jp.blanktar.ruumusic.wear

import kotlin.concurrent.thread

import android.content.Context
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Wearable


class RuuController(ctx: Context) {
    val client = GoogleApiClient.Builder(ctx)
                                .addApi(Wearable.API)
                                .build()

    fun connect() {
        client.connect()
    }

    fun disconnect() {
        client.disconnect()
    }
    
    private fun sendMessage(path: String, message: String = "") {
        thread {
            for (node in Wearable.NodeApi.getConnectedNodes(client).await().getNodes()) {
                Wearable.MessageApi.sendMessage(client, node.getId(), path, message.toByteArray())
            }
        }
    }

    fun play() {
        sendMessage("/control/play")
    }

    fun play(path: String) {
        sendMessage("/control/play", path)
    }

    fun pause() {
        sendMessage("/control/pause")
    }

    fun next() {
        sendMessage("/control/next")
    }

    fun prev() {
        sendMessage("/control/prev")
    }

    fun repeat(mode: RepeatModeType) {
        sendMessage("/control/repeat", mode.name)
    }

    fun shuffle(mode: Boolean) {
        sendMessage("/control/shuffle", if (mode) "ON" else "OFF")
    }
}
