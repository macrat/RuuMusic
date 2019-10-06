package jp.blanktar.ruumusic.wear

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class PlaylistAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {
    private var directory: Directory? = null
    private var isRoot: Boolean = false

    var onParentClickListener: (() -> Unit)? = null
    var onDirectoryClickListener: ((String) -> Unit)? = null
    var onMusicClickListener: ((String) -> Unit)? = null

    fun changeDirectory(dir: Directory, isRoot_: Boolean) {
        val rootReuse = !isRoot && !isRoot_
        notifyItemRangeRemoved(if (rootReuse) 1 else 0, itemCount)
        directory = dir
        isRoot = isRoot_
        notifyItemRangeInserted(if (rootReuse) 1 else 0, itemCount)
    }

    override fun getItemViewType(position: Int) = if (!isRoot && position == 0) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistAdapter.ViewHolder {
        val holder = if (viewType == 0) {
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_upper, parent, false))
        } else {
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))
        }
        holder.itemView.setOnClickListener { _ ->
            if (!isRoot && holder.adapterPosition == 0) {
                onParentClickListener?.invoke()
            } else if (holder.adapterPosition < directory!!.directories.size + if (isRoot) 0 else 1) {
                onDirectoryClickListener?.invoke(directory!!.path + directory!!.all[holder.adapterPosition - if (isRoot) 0 else 1])
            } else {
                onMusicClickListener?.invoke(directory!!.path + directory!!.all[holder.adapterPosition - if (isRoot) 0 else 1])
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: PlaylistAdapter.ViewHolder, position: Int) {
        holder.text?.text = directory!!.all[position - if (isRoot) 0 else 1]
    }

    override fun getItemCount(): Int {
        if (directory != null) {
            return directory!!.directories.size + directory!!.musics.size + if (isRoot) 0 else 1
        } else {
            return 0
        }
    }


    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val text: TextView? = view.findViewById<TextView>(R.id.text)
    }
}
