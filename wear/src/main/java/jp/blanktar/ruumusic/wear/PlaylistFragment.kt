package jp.blanktar.ruumusic.wear

import kotlin.concurrent.thread

import android.app.Fragment
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearSnapHelper
import android.support.wearable.view.CurvedChildLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import kotlinx.android.synthetic.main.fragment_playlist.*


class PlaylistFragment(val client: RuuClient) : Fragment() {
    var statusUpdated = false

    val handler = Handler()

    val snapHelper = LinearSnapHelper()

    var directory: Directory? = null
        set(x) {
            field = x

            val adapter = RecyclerAdapter(x!!, client.status.rootPath == x.path)
            adapter.onParentClickListener = {
                setDirectoryByPath(x.parentPath)
            }
            adapter.onDirectoryClickListener = { dir: String -> setDirectoryByPath(dir) }
            adapter.onMusicClickListener = { music: String ->
                client.play(music)
                onMusicChanged?.invoke()
            }
            list.adapter = adapter

            if (isRoundDisplay && (x!!.all.size > 1 || client.status.rootPath != x.path && x!!.all.size > 0)) {
                scrollTo(1)
            }
        }

    var onMusicChanged: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_playlist, container, false)
    }

    private val isRoundDisplay
        get() = list.paddingTop > 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list.layoutManager = CurvedChildLayoutManager(getContext())

        snapHelper.attachToRecyclerView(list)

        if (isRoundDisplay) {
            val point = Point()
            activity.windowManager.defaultDisplay.getSize(point)
            list.setPadding(0, point.y / 2, 0, point.y / 2)
        }
    }

    fun onEnterAmbient() {}

    fun onExitAmbient() {}

    fun onStatusUpdated(s: Status) {
        if (!statusUpdated) {
            setDirectoryByPath(s.rootPath)
            statusUpdated = true
        }
    }

    fun setDirectoryByPath(path: String, callback: (() -> Unit)? = null) {
        thread {
            val d = client.getDirectory(path)
            handler.post {
                directory = d
                callback?.invoke()
            }
        }
    }

    fun scrollTo(position: Int) {
        handler.post {
            val pos = snapHelper.calculateDistanceToFinalSnap(list.layoutManager, list.findViewHolderForAdapterPosition(position).itemView)!!
            list.scrollBy(pos[0], pos[1])
        }
    }

    fun scrollTo(name: String) {
        scrollTo(directory!!.all.indexOf(name) + if (client.status.rootPath == client.status.musicPath) 0 else 1)
    }
}
