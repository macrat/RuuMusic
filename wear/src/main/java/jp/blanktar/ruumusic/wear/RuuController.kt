package jp.blanktar.ruumusic.wear


import android.content.Context
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Wearable


class RuuConctoller(ctx: Context) {
    val client = GoogleApiClient.Builder(ctx)
                                .addApi(Wearable.API)
                                .build()

    init {
        client.connect()
    }

    fun close() {
        client.disconnect()
    }
    
    fun sendMessage(path: String, message: String) {
        for (node in Wearable.NodeApi.getConnectedNodes(client).await().getNodes()) {
            val result = Wearable.MessageApi.sendMessage(client, node.getId(), path, message.toByteArray()).await()

            if (result.status.isSuccess()) {
                println("sent to: " + node.getDisplayName())
            } else {
                println("send error")
            }
        }
    }
}
