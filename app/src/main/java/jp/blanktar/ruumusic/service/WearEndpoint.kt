package jp.blanktar.ruumusic.service

import kotlin.concurrent.thread

import android.content.Context
import android.os.Bundle
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.RepeatModeType
import jp.blanktar.ruumusic.util.RuuClient
import jp.blanktar.ruumusic.util.RuuDirectory
import jp.blanktar.ruumusic.util.RuuFile
import jp.blanktar.ruumusic.util.RuuFileBase


class WearEndpoint(val context: Context, val controller: RuuService.Controller) : Endpoint, GoogleApiClient.ConnectionCallbacks {
    override val supported = true

    val client = GoogleApiClient.Builder(context)
                                .addApi(Wearable.API)
                                .addConnectionCallbacks(this)
                                .build()

    init {
        client.connect()
    }

    override fun close() {
        client.disconnect()
    }

    private fun statusUpdate(status: PlayingStatus, errorMessage: String?) {
        try {
            val dataMapRequest = PutDataMapRequest.create("/status")
            val dataMap = dataMapRequest.getDataMap()

            dataMap.putBoolean("playing", status.playing)
            dataMap.putString("root_path", RuuDirectory.rootDirectory(context).fullPath)
            dataMap.putString("music_path", status.currentMusic?.fullPath ?: "")
            dataMap.putString("repeat_mode", status.repeatMode.name)
            dataMap.putBoolean("shuffle_mode", status.shuffleMode)
            dataMap.putString("error_message", errorMessage)
            dataMap.putLong("error_time", if (errorMessage != null) System.currentTimeMillis() else 0)

            val request = dataMapRequest.asPutDataRequest()
            Wearable.DataApi.putDataItem(client, request)
        } catch(e: RuuFileBase.NotFound) {
        }
    }

    override fun onStatusUpdated(status: PlayingStatus) {
        statusUpdate(status, null)
    }

    override fun onEqualizerInfo(info: EqualizerInfo) {}

    override fun onFailedPlay(status: PlayingStatus) {
        statusUpdate(status, context.getString(R.string.failed_play, status.currentMusic?.realPath))
    }

    override fun onError(message: String, status: PlayingStatus) {
        statusUpdate(status, message)
    }

    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {
        statusUpdate(status, context.getString(if (isFirst) R.string.first_of_directory else R.string.last_of_directory, status.currentMusic?.realPath))
    }

    private fun makeDataMapFromDirectory(dir: RuuDirectory): PutDataRequest {
        val request = PutDataMapRequest.create("/musics" + dir.fullPath)

        request.dataMap.putStringArray("musics", dir.musics.map { x -> x.name } .toTypedArray())
        request.dataMap.putStringArray("dirs", dir.directories.map { x -> x.name } .toTypedArray())

        return request.asPutDataRequest()
    }

    fun updateDirectory() {
        try {
            val root = RuuDirectory.rootDirectory(context)

            Wearable.DataApi.putDataItem(client, makeDataMapFromDirectory(root))

            for (dir in root.directoriesRecursive) {
                Wearable.DataApi.putDataItem(client, makeDataMapFromDirectory(dir))
            }
        } catch (e: RuuFileBase.NotFound) {
        }
    }

    override fun onConnected(bundle: Bundle?) {
        thread {
            updateDirectory()
        }
    }

    override fun onConnectionSuspended(cause: Int) {}


    class Listener : WearableListenerService() {
        var client: RuuClient? = null

        override fun onCreate() {
            super.onCreate()

            client = RuuClient(getApplicationContext())
        }

        override fun onMessageReceived(ev: MessageEvent) {
            when (ev.path) {
                "/control/play" -> {
                    if (String(ev.data) == "") {
                        client?.play()
                    } else {
                        client?.play(RuuFile.getInstance(getApplicationContext(), String(ev.data)))
                    }
                }
                "/control/pause" -> client?.pause()
                "/control/next" -> client?.next()
                "/control/prev" -> client?.prev()
                "/control/repeat" -> client?.repeat(RepeatModeType.valueOf(String(ev.data)))
                "/control/shuffle" -> client?.shuffle(String(ev.data) == "ON")
            }
        }
    }
}
