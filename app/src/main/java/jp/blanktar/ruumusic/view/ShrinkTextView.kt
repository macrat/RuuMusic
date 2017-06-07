package jp.blanktar.ruumusic.view

import android.widget.TextView
import android.content.Context
import android.util.AttributeSet
import android.graphics.Paint
import android.util.TypedValue
import android.R.attr.textSize

import jp.blanktar.ruumusic.R


class ShrinkTextView(context: Context, val attrs: AttributeSet) : TextView(context, attrs) {
    val namespace = "http://ruumusic.blanktar.jp/view"

    var minTextSize = attrs.getAttributeFloatValue(namespace, "min_size", 0.0f)
        set(x: Float) {
            field = x
            updateTextSize()
        }

    val firstLineMinTextSize: Float
        get() {
            if (secondLine) {
                return (maxTextSize + minTextSize) * 0.5f
            } else {
                return minTextSize
            }
        }

    var maxTextSize = attrs.getAttributeFloatValue(namespace, "max_size", 120.0f)
        set(x: Float) {
            field = x
            updateTextSize()
        }

    var secondLine = attrs.getAttributeBooleanValue(namespace, "secondLine", false)
        set(x: Boolean) {
            field = x
            updateTextSize()
        }

    var resizingEnabled = true

    fun calcTextWidth(size: Float): Float {
        val p = Paint()
        p.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, getResources().getDisplayMetrics()))
        return p.measureText(text.toString())
    }

    fun updateTextSize() {
        if (!resizingEnabled) {
            setTextSize(maxTextSize)
            return
        }

        var temp = maxTextSize

        while (measuredWidth < calcTextWidth(temp) && temp > firstLineMinTextSize) {
            temp--
        }

        if (secondLine && measuredWidth < calcTextWidth(temp)) {
            while (measuredWidth * 1.8 > calcTextWidth(temp) && temp < maxTextSize) {
                temp++
            }
            while (measuredWidth * 1.8 < calcTextWidth(temp) && temp > minTextSize) {
                temp--
            }
        }

        setTextSize(temp)
    }

    override fun onMeasure(width: Int, height: Int) {
        super.onMeasure(width, height)

        if (measuredWidth > 0) {
            updateTextSize()
        }
    }

    override fun setText(text: CharSequence, buf: TextView.BufferType) {
        super.setText(text, buf)

        updateTextSize()
    }
}
