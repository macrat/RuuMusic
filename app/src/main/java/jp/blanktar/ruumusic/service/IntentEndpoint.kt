package jp.blanktar.ruumusic.service

import android.content.Context
import android.content.Intent

import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.RepeatModeType
import jp.blanktar.ruumusic.util.RuuFile


class IntentEndpoint(val context: Context, val controller: RuuService.Controller) : Endpoint {
    override fun close() {}

    override fun onStatusUpdated(status: PlayingStatus) {
        context.sendBroadcast(status.toIntent())
    }

    override fun onEqualizerInfo(info: EqualizerInfo) {
        context.sendBroadcast(info.toIntent())
    }

    override fun onFailedPlay(status: PlayingStatus) {
        context.sendBroadcast(Intent(RuuService.ACTION_FAILED_PLAY).putExtra("path", status.currentMusic?.realPath));
    }

    override fun onError(message: String, status: PlayingStatus) {}

    fun onIntent(intent: Intent){
        when (intent.getAction()) {
            RuuService.ACTION_PLAY -> {
                val path = intent.getStringExtra("path")
                if(path == null){
                    controller.play()
                }else{
                    controller.play(path)
                }
            }
            RuuService.ACTION_PLAY_RECURSIVE -> {
                controller.playRecursive(intent.getStringExtra("path"))
            }
            RuuService.ACTION_PLAY_SEARCH -> {
                controller.playSearch(intent.getStringExtra("path"), intent.getStringExtra("query"))
            }
            RuuService.ACTION_PAUSE -> {
                controller.pause()
            }
            RuuService.ACTION_PAUSE_TRANSIENT -> {
                controller.pauseTransient()
            }
            RuuService.ACTION_PLAY_PAUSE -> {
                controller.playPause()
            }
            RuuService.ACTION_SEEK -> {
                controller.seek(intent.getIntExtra("newtime", -1).toLong())
            }
            RuuService.ACTION_REPEAT -> {
                controller.setRepeatMode(RepeatModeType.valueOf(intent.getStringExtra("mode")))
            }
            RuuService.ACTION_SHUFFLE -> {
                controller.setShuffleMode(intent.getBooleanExtra("mode", false))
            }
            RuuService.ACTION_NEXT -> {
                controller.next()
            }
            RuuService.ACTION_PREV -> {
                controller.prev()
            }
            RuuService.ACTION_PING -> {
                controller.sendStatus()
            }
            RuuService.ACTION_REQUEST_EQUALIZER_INFO -> {
                controller.sendEqualizerInfo()
            }
        }
    }
}
