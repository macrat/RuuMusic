package jp.blanktar.ruumusic.wear

import kotlin.concurrent.thread

import android.app.Fragment
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSnapHelper
import android.support.wearable.view.CurvedChildLayoutManager
import android.support.wearable.view.WearableRecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import kotlinx.android.synthetic.main.fragment_playlist.*


class PlaylistFragment(val controller: RuuController, val receiver: RuuReceiver) : Fragment() {
    var statusUpdated = false
    var directory: Directory? = null
        set(x) {
            field = x

            val adapter = RecyclerAdapter(x!!, receiver.status.rootPath == x.path)
            adapter.onParentClickListener = {
                setDirectoryByPath(x.parentPath)
            }
            adapter.onDirectoryClickListener = { dir: String -> setDirectoryByPath(dir) }
            adapter.onMusicClickListener = { music: String ->
                controller.play(music)
                onMusicChanged?.invoke()
            }
            list.adapter = adapter
        }

    var onMusicChanged: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list.layoutManager = CurvedChildLayoutManager(getContext())

        LinearSnapHelper().attachToRecyclerView(list)
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
        val handler = Handler()
        thread {
            val d = receiver.getDirectory(path)
            handler.post {
                directory = d
                callback?.invoke()
            }
        }
    }

    fun scrollTo(position: Int) {
        (list.layoutManager as LinearLayoutManager).scrollToPosition(position)
    }

    fun scrollTo(name: String) {
        scrollTo(directory!!.all.indexOf(name) + if (receiver.status.rootPath == receiver.status.musicPath) 0 else 1)
    }
}
