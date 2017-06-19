package jp.blanktar.ruumusic.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import jp.blanktar.ruumusic.service.RuuService
import jp.blanktar.ruumusic.service.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.RepeatModeType
import jp.blanktar.ruumusic.util.RuuDirectory
import jp.blanktar.ruumusic.util.RuuFile


open class RuuClientEventListener {
    open fun onUpdatedStatus(status: PlayingStatus) {}
    open fun onEqualizerInfo(info: EqualizerInfo) {}
    open fun onFailedPlay(path: String) {}
    open fun onMusicNotFound(path: String) {}
}


class RuuClient(val context: Context) {
    @JvmField var status = PlayingStatus()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                RuuService.ACTION_STATUS -> onReceiveStatus(intent)
                RuuService.ACTION_EQUALIZER_INFO -> eventListener?.onEqualizerInfo(EqualizerInfo(intent))
                RuuService.ACTION_FAILED_PLAY -> eventListener?.onFailedPlay(intent.getStringExtra("path"))
                RuuService.ACTION_NOT_FOUND -> eventListener?.onMusicNotFound(intent.getStringExtra("path"))
            }
        }
    }

    fun release() {
        eventListener = null
    }

    private fun intent(action: String) = Intent(context, RuuService::class.java).setAction(action)
    
    private fun send(i: Intent) = context.startService(i)

    var eventListener: RuuClientEventListener? = null
        set(listener) {
            if (listener != null && field == null) {
                val intentFilter = IntentFilter()
                intentFilter.addAction(RuuService.ACTION_STATUS)
                intentFilter.addAction(RuuService.ACTION_EQUALIZER_INFO)
                intentFilter.addAction(RuuService.ACTION_FAILED_PLAY)
                intentFilter.addAction(RuuService.ACTION_NOT_FOUND)
                try {
                    context.unregisterReceiver(receiver)
                } catch (e: IllegalArgumentException) {
                }
                context.registerReceiver(receiver, intentFilter)
            }
            if (listener == null && field != null) {
                context.unregisterReceiver(receiver)
            }
            field = listener
        }

    fun play() {
        send(intent(RuuService.ACTION_PLAY))
    }

    fun play(file: RuuFile) {
        send(intent(RuuService.ACTION_PLAY).putExtra("path", file.fullPath))
    }

    fun playRecursive(dir: RuuDirectory) {
        send(intent(RuuService.ACTION_PLAY_RECURSIVE).putExtra("path", dir.fullPath))
    }

    fun playSearch(dir: RuuDirectory, query: String) {
        send(intent(RuuService.ACTION_PLAY_SEARCH)
               .putExtra("path", dir.fullPath)
               .putExtra("query", query))
    }

    fun pause() {
        send(intent(RuuService.ACTION_PAUSE))
    }

    fun pauseTransient() {
        send(intent(RuuService.ACTION_PAUSE_TRANSIENT))
    }

    fun playPause() {
        send(intent(RuuService.ACTION_PLAY_PAUSE))
    }

    fun seek(newtime: Int) {
        send(intent(RuuService.ACTION_SEEK).putExtra("newtime", newtime))
    }

    fun next() {
        send(intent(RuuService.ACTION_NEXT))
    }

    fun prev() {
        send(intent(RuuService.ACTION_PREV))
    }

    fun repeat(mode: RepeatModeType) {
        send(intent(RuuService.ACTION_REPEAT).putExtra("mode", mode.name))
    }

    fun shuffle(enabled: Boolean) {
        send(intent(RuuService.ACTION_SHUFFLE).putExtra("mode", enabled))
    }

    fun ping() {
        send(intent(RuuService.ACTION_PING))
    }

    fun requestEqualizerInfo() {
        send(intent(RuuService.ACTION_REQUEST_EQUALIZER_INFO))
    }

    fun onReceiveStatus(intent: Intent) {
        status = PlayingStatus(context, intent)
        eventListener?.onUpdatedStatus(status)
    }
}