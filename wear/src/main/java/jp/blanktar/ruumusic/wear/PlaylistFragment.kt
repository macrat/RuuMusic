package jp.blanktar.ruumusic.wear

import java.util.Stack
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


class DirectoryLog(val dir: Directory) {
    var scrollPosition = 0
}


class PlaylistFragment(val client: RuuClient) : Fragment() {
    var statusUpdated = false

    val handler = Handler()

    val snapHelper = LinearSnapHelper()

    val directoryStack = Stack<DirectoryLog>()

    var directory: Directory? = null
        set(x) {
            if (!directoryStack.empty()) {
                val layout = list.layoutManager as CurvedChildLayoutManager
                directoryStack.peek().scrollPosition = layout.findFirstVisibleItemPosition()
            }

            field = x
            directoryStack.push(DirectoryLog(x!!))

            adapter.changeDirectory(x, client.status.rootPath == x.path)

            if (isRoundDisplay && (x.all.size > 1 || client.status.rootPath != x.path && x.all.isNotEmpty())) {
                scrollTo(1)
            }
        }

    var onMusicChanged: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_playlist, container, false)
    }

    private val isRoundDisplay
        get() = list.paddingTop > 0

    private val adapter = PlaylistAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list.layoutManager = CurvedChildLayoutManager(context)

        adapter.onParentClickListener = {
            setDirectoryByPath(directory!!.parentPath)
        }
        adapter.onDirectoryClickListener = { dir: String -> setDirectoryByPath(dir) }
        adapter.onMusicClickListener = { music: String ->
            client.play(music)
            onMusicChanged?.invoke()
        }
        list.adapter = adapter

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

    private var loading = false

    fun setDirectoryByPath(path: String, callback: (() -> Unit)? = null) {
        while (!directoryStack.empty() && !path.startsWith(directoryStack.peek().dir.path)) {
            directoryStack.pop()
        }

        if (!directoryStack.empty() && directoryStack.peek().dir.path == path) {
            val log = directoryStack.pop()
            directory = log.dir
            scrollTo(log.scrollPosition)
            callback?.invoke()
            return
        }

        loading = true

        handler.postDelayed({
            if (loading) {
                list.visibility = View.GONE
                progress.visibility = View.VISIBLE
            }
        }, 300)

        thread {
            val d = client.getDirectory(path)
            handler.post {
                directory = d

                loading = false
                progress.visibility = View.GONE
                list.visibility = View.VISIBLE

                callback?.invoke()
            }
        }
    }

    fun scrollTo(position: Int) {
        handler.post {
            val view = list.findViewHolderForAdapterPosition(position)?.itemView
            if (view != null) {
                val pos = snapHelper.calculateDistanceToFinalSnap(list.layoutManager, view)!!
                list.scrollBy(pos[0], pos[1])
            } else {
                list.scrollToPosition(position)
            }
        }
    }

    fun scrollTo(name: String) {
        scrollTo(directory!!.all.indexOf(name) + if (client.status.rootPath == client.status.musicPath) 0 else 1)
    }
}
