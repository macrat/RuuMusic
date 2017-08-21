package jp.blanktar.ruumusic.view


import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Parcelable
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.RuuFileBase
import jp.blanktar.ruumusic.util.RuuDirectory
import jp.blanktar.ruumusic.util.RuuFile


class FilerView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    val namespace = "http://ruumusic.blanktar.jp/view"

    private val frame = LayoutInflater.from(context).inflate(R.layout.view_filer, this)
    private val list = frame.findViewById<RecyclerView>(R.id.list)!!
    private val adapter = Adapter()
    private val layout = LinearLayoutManager(context)

    init {
        list.adapter = adapter
        list.layoutManager = layout
        list.addItemDecoration(DividerDecoration(context))
    }

    var parent: RuuDirectory? = null
        set(x) {
            val old = hasParent
            field = x

            when {
                old && !hasParent -> adapter.notifyItemRemoved(0)
                !old && hasParent -> adapter.notifyItemInserted(0)
                old && hasParent -> adapter.notifyItemChanged(0)
            }
        }

    val parentName: String?
        get() {
            try {
                if (parent == RuuDirectory.rootDirectory(context)) {
                    return "/"
                } else {
                    return parent?.name + "/"
                }
            } catch (e: RuuFileBase.NotFound) {
                return null
            }
        }

    val hasParent
        get() = parent != null

    var showPath = attrs.getAttributeBooleanValue(namespace, "show_path", false)
        set(x) {
            field = x
            adapter.notifyItemRangeChanged(if (hasParent) 1 else 0, adapter.itemCount)
        }

    var files: List<RuuFileBase>? = null
        private set

    val hasContent
        get() = (files?.size ?: 0) > 0

    var loading = true
        set(x) {
            field = x
            if (x) {
                list.visibility = View.GONE
                frame.findViewById<View>(R.id.progress).visibility = View.VISIBLE
            } else {
                frame.findViewById<View>(R.id.progress).visibility = View.GONE
                list.visibility = View.VISIBLE
            }
        }

    var onClickListener: ((RuuFileBase) -> Unit)? = { onEventListener?.onClick(it) }

    var onLongClickListener: ((RuuFileBase, ContextMenu) -> Unit)? = { file, menu -> onEventListener?.onLongClick(file, menu) }

    var onEventListener: OnEventListener? = null

    fun changeFiles(files: List<RuuFileBase>, parent: RuuDirectory?) {
        this.files = files
        this.parent = parent
        adapter.notifyDataSetChanged()
        list.scrollToPosition(0)

        loading = false
    }

    fun changeDirectory(dir: RuuDirectory) {
        changeFiles(dir.children, if (RuuDirectory.rootDirectory(context) != dir) dir.parent else null)
    }

    var layoutState: Parcelable
        get() = layout.onSaveInstanceState()
        set(state) = layout.onRestoreInstanceState(state)


    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        val VIEWTYPE_NORMAL = 0
        val VIEWTYPE_UPPER_DIR = 1
        val VIEWTYPE_WITH_PATH = 2


        override fun getItemViewType(position: Int) = when {
            position == 0 && hasParent -> VIEWTYPE_UPPER_DIR
            showPath -> VIEWTYPE_WITH_PATH
            else -> VIEWTYPE_NORMAL
        }
        
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): Adapter.ViewHolder {
            val inflater = LayoutInflater.from(context)
            
            val holder = when (viewType) {
                VIEWTYPE_UPPER_DIR -> ViewHolder(inflater.inflate(R.layout.view_filer_upper, viewGroup, false))
                VIEWTYPE_WITH_PATH -> ViewHolder(inflater.inflate(R.layout.view_filer_withpath, viewGroup, false))
                else -> ViewHolder(inflater.inflate(R.layout.view_filer_item, viewGroup, false))
            }
            
            holder.itemView.setOnClickListener { _ ->
                if (viewType == VIEWTYPE_UPPER_DIR) {
                    onClickListener?.invoke(parent!!)
                } else {
                    files?.let {
                        try {
                            onClickListener?.invoke(it[holder.adapterPosition - if (hasParent) 1 else 0])
                        } catch (e: IndexOutOfBoundsException) {
                        }
                    }
                }
            }

            holder.itemView.setOnCreateContextMenuListener { menu, _, _ ->
                if (viewType == VIEWTYPE_UPPER_DIR) {
                    onLongClickListener?.invoke(parent!!, menu)
                } else {
                    files?.let {
                        try {
                            onLongClickListener?.invoke(it[holder.adapterPosition - if (hasParent) 1 else 0], menu)
                        } catch (e: IndexOutOfBoundsException) {
                        }
                    }
                }
            }

            return holder
        }

        override fun onBindViewHolder(holder: Adapter.ViewHolder, position: Int) {
            if (holder.itemViewType == VIEWTYPE_UPPER_DIR) {
                holder.text?.text = context.getString(R.string.upper_dir, parentName)
                return
            }

            files?.let {
                val item = it[position - if (hasParent) 1 else 0]
                holder.name?.text = item.name + if (item.isDirectory) "/" else ""
                try {
                    holder.path?.text = item.ruuPath
                } catch (e: RuuFileBase.OutOfRootDirectory) {
                    holder.path?.text = ""
                }
            }
        }

        override fun getItemCount() = (files?.size ?: 0) + if (hasParent) 1 else 0


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name = view.findViewById<TextView?>(R.id.name)
            val path = view.findViewById<TextView?>(R.id.path)
            val text = view.findViewById<TextView?>(R.id.text)
        }
    }


    class DividerDecoration(context: Context) : RecyclerView.ItemDecoration() {
        val divider = context.resources.getDrawable(R.drawable.view_filer_divider)

        override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val left = parent.paddingLeft
            val right = parent.width - parent.paddingRight

            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val params = child.layoutParams as RecyclerView.LayoutParams
                val top = child.bottom + params.bottomMargin
                val bottom = top + divider.intrinsicHeight
                divider.setBounds(left + ViewCompat.getTranslationX(child).toInt(),
                                  top + ViewCompat.getTranslationY(child).toInt(),
                                  right + ViewCompat.getTranslationX(child).toInt(),
                                  bottom + ViewCompat.getTranslationY(child).toInt())
                divider.alpha = (ViewCompat.getAlpha(child) * 255f).toInt()
                divider.draw(canvas)
            }
        }
        
        override fun getItemOffsets(rect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            rect.set(0, 0, 0, divider.intrinsicHeight)
        }
    }


    abstract class OnEventListener {
        abstract fun onClick(file: RuuFileBase)
        abstract fun onLongClick(file: RuuFileBase, menu: ContextMenu)
    }
}
