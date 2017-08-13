package jp.blanktar.ruumusic.wear

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.support.wearable.view.WearableRecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class RecyclerAdapter(val dir: Directory, val isRoot: Boolean) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
    var onParentClickListener: (() -> Unit)? = null
    var onDirectoryClickListener: ((String) -> Unit)? = null
    var onMusicClickListener: ((String) -> Unit)? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text = view.findViewById<TextView>(R.id.text)
    }

    override fun getItemViewType(position: Int) = if (!isRoot && position == 0) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.ViewHolder {
        val holder = if (viewType == 0) {
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_upper, parent, false))
        } else {
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))
        }
        holder.itemView.setOnClickListener { _ ->
            if (!isRoot && holder.adapterPosition == 0) {
                onParentClickListener?.invoke()
            } else if (holder.adapterPosition < dir.directories.size + if (isRoot) 0 else 1) {
                onDirectoryClickListener?.invoke(dir.path + dir.all[holder.adapterPosition - if (isRoot) 0 else 1])
            } else {
                onMusicClickListener?.invoke(dir.path + dir.all[holder.adapterPosition - if (isRoot) 0 else 1])
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerAdapter.ViewHolder, position: Int) {
        holder.text?.text = dir.all[position - if (isRoot) 0 else 1]
    }

    override fun getItemCount() = dir.directories.size + dir.musics.size + if (isRoot) 0 else 1
}
