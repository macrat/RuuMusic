package jp.blanktar.ruumusic.service


import kotlin.concurrent.thread

import android.content.Context

import jp.blanktar.ruumusic.util.DynamicShortcuts
import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.Preference
import jp.blanktar.ruumusic.util.RuuFile


class ShortcutsEndpoint(val context: Context) : Endpoint {
    val preference = Preference(context)

    init {
        preference.RootDirectory.setOnChangeListener {
            thread {
                DynamicShortcuts(context).managePinnedShortcuts()
            }
        }
    }

    override fun close() {
        preference.unsetAllListeners()
    }

    override fun onStatusUpdated(status: PlayingStatus) {}
    override fun onEqualizerInfo(info: EqualizerInfo) {}

    override fun onMediaStoreUpdated() {
        thread {
            DynamicShortcuts(context).managePinnedShortcuts()
        }
    }

    override fun onFailedPlay(status: PlayingStatus) {}
    override fun onError(message: String, status: PlayingStatus) {}
    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {}
}
