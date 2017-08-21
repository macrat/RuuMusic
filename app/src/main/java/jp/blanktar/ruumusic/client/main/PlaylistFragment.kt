package jp.blanktar.ruumusic.client.main

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PagerSnapHelper
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.Preference
import jp.blanktar.ruumusic.util.RuuDirectory
import jp.blanktar.ruumusic.util.RuuFile
import jp.blanktar.ruumusic.util.RuuFileBase
import jp.blanktar.ruumusic.view.FilerView
import kotlinx.android.synthetic.main.fragment_playlist.*


class PlaylistFragment : Fragment() {
    var searchQuery: String? = null

    var shownDirectory: RuuDirectory? = null
        private set(dir) {
            field = dir
            if (dir != null) {
                recycler?.adapter?.notifyDataSetChanged()
            }
        }

    var directory: RuuDirectory? = null
        set(dir) {
            if (shownDirectory?.contains(dir!!) ?: true) {
                shownDirectory = dir
                field = null
                recycler?.smoothScrollToPosition(recycler?.adapter?.itemCount ?: 0)
            } else {
                try {
                    recycler?.smoothScrollToPosition((dir?.ruuDepth() ?: 1) - 1)
                } catch(e: RuuFileBase.OutOfRootDirectory) {
                }
                field = dir
            }
        }

    var onMusicSelectListener: OnMusicSelectListener? = null

    private fun restoreCurrentPath(preference: Preference) {
        val currentPath = preference.CurrentViewPath.get()
        try {
            if (currentPath != null) {
                directory = RuuDirectory.getInstance(context, currentPath)

                searchQuery = preference.LastSearchQuery.get()
                if (searchQuery != null) {
                    onQueryTextSubmit(searchQuery!!)
                }
            } else {
                val dir = RuuDirectory.rootCandidate(context)
                val root = RuuDirectory.rootDirectory(context)
                if (!root.contains(dir)) {
                    directory = root
                } else {
                    directory = dir
                }
            }
        } catch (err: RuuFileBase.NotFound) {
            try {
                directory = RuuDirectory.rootDirectory(context)
            } catch (e: RuuFileBase.NotFound) {
                Toast.makeText(activity, getString(R.string.cant_open_dir, e.path), Toast.LENGTH_LONG).show()
                preference.RootDirectory.remove()
            }

        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_playlist, container, false)

        val preference = Preference(view.context)

        restoreCurrentPath(preference)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        val layout = LinearLayoutManager(context)
        layout.orientation = LinearLayoutManager.HORIZONTAL
        recycler.layoutManager = layout
        recycler.adapter = Adapter()
        PagerSnapHelper().attachToRecyclerView(recycler)

        recycler.scrollToPosition(recycler.adapter.itemCount - 1)

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(re: RecyclerView, state: Int) {
                if (state == RecyclerView.SCROLL_STATE_IDLE) {
                    if (directory != null) {
                        shownDirectory = directory
                    } else {
                        val ommit = re.adapter.itemCount - 1 - layout.findFirstVisibleItemPosition()
                        shownDirectory = shownDirectory?.getParent(ommit)
                    }
                }
            }

            override fun onScrolled(re: RecyclerView, dx: Int, dy: Int) {
                val target = layout.findViewByPosition(layout.findFirstVisibleItemPosition() + 1)
                if (target != null) {
                    target.alpha = Math.pow(1.0f - target.left.toDouble()/target.width, 3.0).toFloat()
                }
            }
        })

        return view
    }

    fun updateStatus() {}
    fun updateRoot() {}
    fun updateTitle(activity: Activity) {}
    fun updateMenu(activity: Activity) {}
    fun setSearchQuery(path: RuuDirectory, query: String) {}
    fun onQueryTextSubmit(query: String) {}

    fun onBackKey(): Boolean {
        if (recycler.layoutManager.itemCount > 1) {
            recycler.smoothScrollToPosition(recycler.layoutManager.itemCount - 2)
            return true
        } else {
            return false
        }
    }


    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {}

        override fun getItemViewType(position: Int) = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.playlist_page, parent, false))

        override fun onBindViewHolder(holder: Adapter.ViewHolder, position: Int) {
            holder.itemView.alpha = 1.0f

            val filer = holder.itemView as FilerView

            filer.loading = true

            filer.onItemClickListener = { item ->
                if (item.isDirectory()) {
                    directory = item as RuuDirectory
                } else {
                    onMusicSelectListener?.onMusicSelect(item as RuuFile)
                }
            }

            filer.onParentClickListener = {
                directory = shownDirectory!!.getParent(getItemCount() - position)
            }

            filer.hasParent = position != 0

            val files = shownDirectory?.getParent(getItemCount() - 1 - position)?.getChildren()
            if (files != null) {
                filer.setFiles(files)
            }
        }

        override fun getItemCount(): Int {
            try {
                return shownDirectory?.ruuDepth() ?: 1
            } catch(e: RuuFileBase.OutOfRootDirectory) {
                return 1
            }
        }
    }

    abstract class OnMusicSelectListener {
        abstract fun onMusicSelect(music: RuuFile)
    }
}
