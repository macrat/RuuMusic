package jp.blanktar.ruumusic.service


import kotlin.concurrent.thread

import android.content.Context

import jp.blanktar.ruumusic.util.DynamicShortcuts
import jp.blanktar.ruumusic.util.createDynamicShortcutInfo
import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.Preference
import jp.blanktar.ruumusic.util.RuuFile


class ShortcutsEndpoint(val context: Context) : Endpoint {
    val manager = DynamicShortcuts(context)
    val preference = Preference(context)

    init {
        preference.RootDirectory.setOnChangeListener {
            thread {
                manager.managePinnedShortcuts()
            }
        }
    }

    override fun close() {
        preference.unsetAllListeners()
    }

    override fun onStatusUpdated(status: PlayingStatus) {
        if (status.currentMusic == null) {
            return
        }

        val shortcuts = preference.ListedDynamicShortcuts.get().filter { it != status.currentMusic.parent }.toMutableList()
        shortcuts.add(status.currentMusic.parent)

        manager.shortcuts = shortcuts.map {
            createDynamicShortcutInfo(context, it)
        }.filterNotNull()

        preference.ListedDynamicShortcuts.set(shortcuts)
    }

    override fun onEqualizerInfo(info: EqualizerInfo) {}

    override fun onMediaStoreUpdated() {
        thread {
            manager.managePinnedShortcuts()
        }
    }

    override fun onFailedPlay(status: PlayingStatus) {}
    override fun onError(message: String, status: PlayingStatus) {}
    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {}
}
