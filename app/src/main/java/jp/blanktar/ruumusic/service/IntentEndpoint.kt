package jp.blanktar.ruumusic.service

import android.content.Context
import android.content.Intent
import android.os.Build

import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.RepeatModeType
import jp.blanktar.ruumusic.widget.MusicNameWidget
import jp.blanktar.ruumusic.widget.PlayPauseWidget
import jp.blanktar.ruumusic.widget.SkipNextWidget
import jp.blanktar.ruumusic.widget.SkipPrevWidget
import jp.blanktar.ruumusic.widget.UnifiedWidget


class IntentEndpoint(val context: Context, private val controller: RuuService.Controller) : Endpoint {
    override val supported = true

    override fun close() {}

    override fun onStatusUpdated(status: PlayingStatus) {
        context.sendBroadcast(status.toIntent())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.sendBroadcast(status.toIntent(Intent(context, MusicNameWidget::class.java)))
            context.sendBroadcast(status.toIntent(Intent(context, PlayPauseWidget::class.java)))
            context.sendBroadcast(status.toIntent(Intent(context, SkipNextWidget::class.java)))
            context.sendBroadcast(status.toIntent(Intent(context, SkipPrevWidget::class.java)))
            context.sendBroadcast(status.toIntent(Intent(context, UnifiedWidget::class.java)))
        }
    }

    override fun onEqualizerInfo(info: EqualizerInfo) {
        context.sendBroadcast(info.toIntent())
    }

    override fun onFailedPlay(status: PlayingStatus) {
        context.sendBroadcast(status.toIntent().setAction(RuuService.ACTION_FAILED_PLAY))
    }

    override fun onError(message: String, status: PlayingStatus) {}
    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {}

    fun onIntent(intent: Intent){
        when (intent.action) {
            RuuService.ACTION_PLAY -> {
                val path = intent.getStringExtra("path")
                if (path == null) {
                    controller.play()
                } else {
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
                intent.getStringExtra("mode")?.let {
                    controller.setRepeatMode(RepeatModeType.valueOf(it))
                }
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
