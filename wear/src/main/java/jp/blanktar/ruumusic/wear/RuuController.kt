package jp.blanktar.ruumusic.wear


import android.content.Context
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Wearable


class RuuConctoller(ctx: Context) {
    client = GoogleApiClient.Builder(ctx)
        .addApi(Wearable.API)
        .addConnectionCallbacks(ctx)
        .addOnConnectionFailedListener(ctx)
        .build()

    init {
        client.connect()
    }

    fun close() {
        client.disconnect()
    }
    
    fun sendMessage(path: String, message: String) {
        for (Node node : Wearable.NodeApi.getConnectedNodes(client).await()) {
            SendMessageResult result = Wearable.MessageApi.sendMessage(client, node.getId(), path, message.getBytes()).await();

            if (result.status.isSuccess()) {
                println("sent to: " + node.getDisplayName());
            } else {
                println("Send error");
            }
        }
    }
}
