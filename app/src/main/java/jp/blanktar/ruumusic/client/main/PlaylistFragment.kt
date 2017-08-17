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

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.Preference
import jp.blanktar.ruumusic.util.RuuDirectory
import jp.blanktar.ruumusic.util.RuuFileBase
import kotlinx.android.synthetic.main.fragment_playlist.*
import android.widget.Toast
import android.R.attr.path


class PlaylistFragment : Fragment() {
    var searchQuery: String? = null

    var directory: RuuDirectory? = null
        set(x) {
            field = x
            if (x != null) {
                recycler?.adapter?.notifyDataSetChanged()
            }
        }


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
                    val ommit = re.adapter.itemCount - 1 - layout.findFirstVisibleItemPosition()
                    directory = directory?.getParent(ommit)
                }
            }

            override fun onScrolled(re: RecyclerView, dx: Int, dy: Int) {}
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
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text = view.findViewById<TextView>(R.id.text)
        }

        override fun getItemViewType(position: Int) = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.playlist_page, parent, false))

        override fun onBindViewHolder(holder: Adapter.ViewHolder, position: Int) {
            holder.text?.text = directory?.getParent(getItemCount() - 1 - position)?.fullPath
        }

        override fun getItemCount(): Int {
            try {
                return directory?.ruuDepth() ?: 1
            } catch(e: RuuFileBase.OutOfRootDirectory) {
                return 1
            }
        }
    }
}
