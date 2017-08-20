package jp.blanktar.ruumusic.view

import android.content.Context
import android.util.AttributeSet
import android.view.ContextMenu
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.ListView
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.view.ViewGroup
import android.widget.TextView

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.RuuFileBase


class FilerView(context: Context, val attrs: AttributeSet) : FrameLayout(context, attrs) {
    val namespace = "http://ruumusic.blanktar.jp/view"

    private val adapter = Adapter()
    
    private val frame = LayoutInflater.from(context).inflate(R.layout.view_filer, this)

    init {
        val list = frame.findViewById<ListView>(R.id.list)
        list.setAdapter(adapter)

        list.setOnItemClickListener { _, _, position, _ ->
            if (hasParent && position == 0) {
                onParentClickListener?.invoke()
            } else {
                onItemClickListener?.invoke(adapter.getItem(position - if (hasParent) 1 else 0) as RuuFileBase)
            }
        }

        list.setOnCreateContextMenuListener { menu, _, menuInfo ->
            val position = (menuInfo as AdapterView.AdapterContextMenuInfo).position - if (hasParent) 1 else 0
            if (hasParent && position == 0) {
                onParentLongClickListener?.invoke(menu)
            } else {
                onItemLongClickListener?.invoke(adapter.getItem(position), menu)
            }
        }
    }

    var hasParent = attrs.getAttributeBooleanValue(namespace, "has_parent", false)
    var showPath = attrs.getAttributeBooleanValue(namespace, "show_path", false)

    var loading = true
        set(x) {
            field = x
            if (x) {
                frame.findViewById<View>(R.id.list).visibility = View.GONE
                frame.findViewById<View>(R.id.progress).visibility = View.VISIBLE
            } else {
                frame.findViewById<View>(R.id.progress).visibility = View.GONE
                frame.findViewById<View>(R.id.list).visibility = View.VISIBLE
            }
        }

    var onItemClickListener: ((RuuFileBase) -> Unit)? = null

    var onParentClickListener: (() -> Unit)? = null

    var onItemLongClickListener: ((RuuFileBase, ContextMenu) -> Unit)? = null

    var onParentLongClickListener: ((ContextMenu) -> Unit)? = null

    fun setFiles(files: Collection<RuuFileBase>) {
        loading = true
        adapter.clear()
        adapter.addAll(files)
        loading = false
    }


    inner class Adapter : ArrayAdapter<RuuFileBase>(context, R.layout.view_filer_item) {
        override fun getViewTypeCount() = 3

        private val inflater
            get() = LayoutInflater.from(context)

        override fun getItemViewType(position: Int) = when {
            position == 0 && hasParent -> 1
            showPath -> 2
            else -> 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            if (position == 0 && hasParent) {
                return convertView ?: inflater.inflate(R.layout.view_filer_upper, parent, false)
            }

            val item = getItem(position - if (hasParent) 1 else 0)

            if (showPath) {
                val result = convertView ?: inflater.inflate(R.layout.view_filer_withpath, parent, false)

                result.findViewById<TextView>(R.id.name).text = item.name

                try {
                    result.findViewById<TextView>(R.id.path).text = item.parent.ruuPath + if (item.isDirectory()) "/" else ""
                } catch (e: RuuFileBase.OutOfRootDirectory) {
                    result.findViewById<TextView>(R.id.path).text = ""
                }

                return result
            } else {
                val result = convertView ?: inflater.inflate(R.layout.view_filer_item, parent, false)

                (result as TextView).text = item.name + if (item.isDirectory()) "/" else ""

                return result
            }
        }

        override fun getCount() = super.getCount() + if (hasParent) 1 else 0
    }
}
