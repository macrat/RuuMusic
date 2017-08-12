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
    
    fun sendMessage(path: String, message: String) {
        thread {
            for (node in Wearable.NodeApi.getConnectedNodes(client).await().getNodes()) {
                val result = Wearable.MessageApi.sendMessage(client, node.getId(), path, message.toByteArray()).await()

                if (result.status.isSuccess()) {
                    println("sent to: " + node.getDisplayName())
                    println("data: " + path + ", message: " + message)
                } else {
                    println("send error")
                }
            }
        }
    }
}
