package jp.blanktar.ruumusic.wear

import kotlin.concurrent.thread

import android.app.Fragment
import android.os.Bundle
import android.os.Handler
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
                println(x.parentPath)
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
    }

    fun onStatusUpdated(s: Status) {
        if (!statusUpdated) {
            setDirectoryByPath(receiver.status.rootPath)
            statusUpdated = true
        }
    }

    fun setDirectoryByPath(path: String) {
        val handler = Handler()
        thread {
            val d = receiver.getDirectory(path)
            handler.post {
                directory = d
            }
        }
    }
}
