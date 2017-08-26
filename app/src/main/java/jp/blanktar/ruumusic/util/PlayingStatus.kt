package jp.blanktar.ruumusic.util

import android.content.Context
import android.content.Intent

import jp.blanktar.ruumusic.service.RuuService


fun msec2str(msec: Long): String {
    if (msec >= 0) {
        val sec = Math.round(msec / 1000.0)
        return "%d:%02d".format(Math.floor(sec / 60.0).toInt(), sec % 60)
    } else {
        return "-"
    }
}


class PlayingStatus(@JvmField val playing: Boolean = false,
                    @JvmField val currentMusic: RuuFile? = null,

                    @JvmField val duration: Long = 0,
                    @JvmField val baseTime: Long = 0,
                    @JvmField val receivedCurrentTime: Long = 0,

                    @JvmField val repeatMode: RepeatModeType = RepeatModeType.OFF,
                    @JvmField val shuffleMode: Boolean = false,

                    @JvmField val searchPath: RuuDirectory? = null,
                    @JvmField val searchQuery: String? = null,

                    @JvmField val recursivePath: RuuDirectory? = null) {

    val currentTime: Long
        get() {
            if (playing && baseTime > 0) {
                if (duration >= 0) {
                    return Math.min(duration, System.currentTimeMillis() - baseTime)
                } else {
                    return System.currentTimeMillis() - baseTime
                }
            } else {
                return Math.max(0, receivedCurrentTime)
            }
        }

    val durationStr
        get() = msec2str(duration)

    val currentTimeStr
        get() = msec2str(currentTime)

    constructor(context: Context, intent: Intent)
            : this(playing = intent.getBooleanExtra("playing", false),
            currentMusic = try {
                RuuFile.getInstance(context, intent.getStringExtra("path"))
            } catch (e: RuuFileBase.NotFound) {
                null
            } catch (e: NullPointerException) {
                null
            },

            duration = intent.getLongExtra("duration", -1),
            baseTime = intent.getLongExtra("basetime", -1),
            receivedCurrentTime = intent.getLongExtra("current", -1),

            repeatMode = RepeatModeType.valueOf(intent.getStringExtra("repeat")),
            shuffleMode = intent.getBooleanExtra("shuffle", false),

            searchPath = try {
                RuuDirectory.getInstance(context, intent.getStringExtra("searchPath"))
            } catch (e: RuuFileBase.NotFound) {
                null
            } catch (e: NullPointerException) {
                null
            },
            searchQuery = intent.getStringExtra("searchQuery"),

            recursivePath = try {
                RuuDirectory.getInstance(context, intent.getStringExtra("recursivePath"))
            } catch (e: RuuFileBase.NotFound) {
                null
            } catch (e: NullPointerException) {
                null
            })

    fun toIntent(): Intent {
        val intent = Intent()

        intent.action = RuuService.ACTION_STATUS

        intent.putExtra("repeat", repeatMode.name)
        intent.putExtra("shuffle", shuffleMode)

        intent.putExtra("path", currentMusic?.fullPath)
        intent.putExtra("recursivePath", recursivePath?.fullPath)
        intent.putExtra("searchPath", searchPath?.fullPath)
        intent.putExtra("searchQuery", searchQuery)

        intent.putExtra("playing", playing)
        intent.putExtra("duration", duration)
        intent.putExtra("current", receivedCurrentTime)
        intent.putExtra("basetime", baseTime)

        return intent
    }
}
