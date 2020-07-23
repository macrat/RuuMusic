package jp.blanktar.ruumusic.service


import kotlin.concurrent.thread

import android.content.Context
import android.os.Build

import jp.blanktar.ruumusic.util.DynamicShortcuts
import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.Preference
import jp.blanktar.ruumusic.util.createDynamicShortcutInfo


class ShortcutsEndpoint(val context: Context) : Endpoint {
    override val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

    private val manager = if (supported) DynamicShortcuts(context) else null
    private val preference = Preference(context)

    init {
        preference.RootDirectory.setOnChangeListener {
            thread {
                manager?.managePinnedShortcuts()
            }
        }
    }

    override fun close() {
        preference.unsetAllListeners()
    }

    override fun onStatusUpdated(status: PlayingStatus) {
        if (manager == null || status.currentMusic == null) {
            return
        }

        val shortcuts = preference.ListedDynamicShortcuts.get().filter { it != status.currentMusic.parent }.takeLast(manager.maxShortcuts - 1).toMutableList()
        shortcuts.add(status.currentMusic.parent)

        manager.shortcuts = shortcuts.mapNotNull {
            createDynamicShortcutInfo(context, it)
        }

        preference.ListedDynamicShortcuts.set(shortcuts)
    }

    override fun onEqualizerInfo(info: EqualizerInfo) {}

    override fun onFailedPlay(status: PlayingStatus) {}
    override fun onError(message: String, status: PlayingStatus) {}
    override fun onEndOfList(isFirst: Boolean, status: PlayingStatus) {}
}
