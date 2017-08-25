package jp.blanktar.ruumusic.util


import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.client.main.MainActivity


const val SHORTCUTS_EXTRA_SHORTCUT_ID = "jp.blanktar.ruumusic.EXTRA_SHORTCUT_ID"


fun createDynamicShortcutInfo(context: Context, file: RuuFileBase): ShortcutInfo? {
    if (Build.VERSION.SDK_INT < 26) {
        return null
    } else {
        val id = "view:" + (if (file.isDirectory) "dir:" else "file:") + file.fullPath
        val intent = Intent(context, MainActivity::class.java)
                            .setAction(if (file.isDirectory) MainActivity.ACTION_OPEN_PLAYLIST else MainActivity.ACTION_START_PLAY)
                            .setData(file.toUri())
                            .putExtra(SHORTCUTS_EXTRA_SHORTCUT_ID, id)

        return ShortcutInfo.Builder(context, id)
                           .setActivity(ComponentName(context, MainActivity::class.java))
                           .setIntent(intent)
                           .setIcon(Icon.createWithResource(context, if (file.isDirectory) R.drawable.ic_shortcut_playlist else R.drawable.ic_shortcut_player))
                           .setShortLabel(if (file.name.length <= 10) file.name else file.name.substring(0, 9) + "…")
                           .setLongLabel(if (file.name.length <= 25) file.name else file.name.substring(0, 24) + "…")
                           .build()
    }
}


class DynamicShortcuts(val context: Context) {
    val manager: ShortcutManager? = context.getSystemService(ShortcutManager::class.java)

    var shortcuts
        get() = manager?.getDynamicShortcuts()
        set(xs) {
            manager?.setDynamicShortcuts(xs)
        }

    val isRequestPinSupported
        get() = manager?.isRequestPinShortcutSupported ?: false

    fun requestPin(x: ShortcutInfo): Boolean {
        val intent = manager?.createShortcutResultIntent(x)
        if (intent != null) {
            return manager?.requestPinShortcut(x, PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT).getIntentSender()) ?: false
        }
        return false
    }

    fun requestPin(context: Context, file: RuuFileBase): Boolean {
        val shortcut = createDynamicShortcutInfo(context, file)
        if (shortcut != null) {
            return requestPin(shortcut)
        }
        return false
    }

    fun reportShortcutUsed(id: String?) {
        if (id != null) {
            manager?.reportShortcutUsed(id)
        }
    }

    fun managePinnedShortcuts() {
        val shortcuts = manager?.getPinnedShortcuts()
        if (shortcuts == null) {
            return
        }

        val enabled = shortcuts.map(fun (it): Boolean {
            if (!it.id.startsWith("view:")) {
                return true
            }

            val path = it.intent?.data?.path
            if (path == null) {
                return false
            }

            try {
                val file = if (it.id.startsWith("view:file:")) {
                    RuuFile.getInstance(context, path.dropLastWhile { it != '.' }.dropLast(1))
                } else {
                    RuuDirectory.getInstance(context, path)
                }
                file.ruuPath
            } catch (e: RuuFileBase.NotFound) {
                return false
            } catch (e: RuuFileBase.OutOfRootDirectory) {
                return false
            }

            return true
        })
        manager?.enableShortcuts(shortcuts.filterIndexed { i, _ -> enabled[i] }.map { it.id })
        manager?.disableShortcuts(shortcuts.filterIndexed { i, _ -> !enabled[i] }.map { it.id })
    }

    companion object {
        const val EXTRA_SHORTCUT_ID = SHORTCUTS_EXTRA_SHORTCUT_ID
    }
}
