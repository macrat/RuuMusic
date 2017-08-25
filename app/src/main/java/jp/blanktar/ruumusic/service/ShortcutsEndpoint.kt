package jp.blanktar.ruumusic.service


import kotlin.concurrent.thread

import android.content.Context
import android.os.Build

import jp.blanktar.ruumusic.util.DynamicShortcuts
import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.Preference
import jp.blanktar.ruumusic.util.RuuFile
import jp.blanktar.ruumusic.util.createDynamicShortcutInfo


class ShortcutsEndpoint(val context: Context) : Endpoint {
    val manager = if (Build.VERSION.SDK_INT >= 25) DynamicShortcuts(context) else null
    val preference = if (Build.VERSION.SDK_INT >= 25) Preference(context) else null

    init {
        preference?.RootDirectory?.setOnChangeListener {
            thread {
                manager!!.managePinnedShortcuts()
            }
        }
    }

    override fun close() {
        preference?.unsetAllListeners()
    }

    override fun onStatusUpdated(status: PlayingStatus) {
        if (Build.VERSION.SDK_INT < 25 || status.currentMusic == null) {
            return
        }

        val shortcuts = preference!!.ListedDynamicShortcuts.get().filter { it != status.currentMusic.parent }.toMutableList()
        shortcuts.add(status.currentMusic.parent)

        manager!!.shortcuts = shortcuts.map {
            createDynamicShortcutInfo(context, it)
        }.filterNotNull()

        preference!!.ListedDynamicShortcuts.set(shortcuts)
    }

    override fun onEqualizerInfo(info: EqualizerInfo) {}

    override fun onMediaStoreUpdated() {
        if (Build.VERSION.SDK_INT >= 25) {
            thread {
                manager!!.managePinnedShortcuts()
            }
        }
    }

    override fun onFailedPlay(status: PlayingStatus) {}
    override fun onError(message: String, status: PlayingStatus) {}
    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {}
}
