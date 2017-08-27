package jp.blanktar.ruumusic.wear

import kotlin.concurrent.thread

import android.content.Context
import android.os.Bundle
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.DataApi
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable


class RuuClient(ctx: Context) : GoogleApiClient.ConnectionCallbacks, DataApi.DataListener {
    val client = GoogleApiClient.Builder(ctx)
                                .addApi(Wearable.API)
                                .addConnectionCallbacks(this)
                                .build()!!

    var status = Status()
        set(x) {
            field = x
            onStatusUpdated?.invoke(x)
        }

    var onStatusUpdated: ((Status) -> Unit)? = null

    var onFailedSendMessage: (() -> Unit)? = null

    var onConnectedListener: (() -> Unit)? = null

    fun connect() {
        client.connect()
    }

    fun disconnect() {
        Wearable.DataApi.removeListener(client, this)
        client.disconnect()
    }

    override fun onConnected(bundle: Bundle?) {
        Wearable.DataApi.getDataItems(client).setResultCallback { items ->
            for (item in items) {
                if (item.uri.path == "/status") {
                    status = Status(DataMap.fromByteArray(item.data))
                    break
                }
            }
            items.release()
        }
        Wearable.DataApi.addListener(client, this)

        onConnectedListener?.invoke()
    }

    override fun onConnectionSuspended(cause: Int) {}

    override fun onDataChanged(evs: DataEventBuffer) {
        for (ev in evs) {
            if (ev.type == DataEvent.TYPE_CHANGED && ev.dataItem.uri.path == "/status") {
                status = Status(DataMap.fromByteArray(ev.dataItem.data))
                break
            }
        }
        evs.release()
    }

    fun getDirectory(dir: String): Directory? {
        if (!client.isConnected) {
            client.blockingConnect()
        }

        val items = Wearable.DataApi.getDataItems(client).await()

        for (item in items) {
            if (item.uri.path == "/musics" + dir) {
                val map = DataMap.fromByteArray(item.data)

                val musics = map.getStringArray("musics")
                val dirs = map.getStringArray("dirs")

                items.release()

                return Directory(dir, dirs, musics)
            }
        }

        return null
    }

    private fun sendMessage(path: String, message: String = "") {
        thread {
            for (node in Wearable.NodeApi.getConnectedNodes(client).await().nodes) {
                Wearable.MessageApi.sendMessage(client, node.id, path, message.toByteArray()).setResultCallback { result ->
                    if (!result.status.isSuccess && !result.status.isCanceled) {
                        onFailedSendMessage?.invoke()
                    }
                }
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
