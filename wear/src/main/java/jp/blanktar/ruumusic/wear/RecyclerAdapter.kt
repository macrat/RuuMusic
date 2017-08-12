package jp.blanktar.ruumusic.wear

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.support.wearable.view.WearableRecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class RecyclerAdapter(val data: Array<String>) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text = view.findViewById<TextView>(R.id.text)
    }

    override fun getItemViewType(position: Int) = if (position == 0) { 0 } else { 1 }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.ViewHolder {
        if (viewType == 0) {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_upper, parent, false))
        } else {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerAdapter.ViewHolder, position: Int) {
        holder.text?.text = data[position - 1]
    }

    override fun getItemCount() = data.size
}
