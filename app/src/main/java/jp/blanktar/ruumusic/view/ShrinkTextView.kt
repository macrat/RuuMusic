package jp.blanktar.ruumusic.view

import android.widget.TextView
import android.content.Context
import android.util.AttributeSet
import android.graphics.Paint
import android.util.TypedValue


class ShrinkTextView(context: Context, attrs: AttributeSet) : TextView(context, attrs) {
    val namespace = "http://ruumusic.blanktar.jp/view"

    var minTextSize = attrs.getAttributeFloatValue(namespace, "min_size", 0.0f)
        set(x) {
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
        set(x) {
            field = x
            updateTextSize()
        }

    var secondLine = attrs.getAttributeBooleanValue(namespace, "secondLine", false)
        set(x) {
            field = x
            updateTextSize()
        }

    var resizingEnabled = true

    fun calcTextWidth(size: Float): Float {
        val p = Paint()
        p.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, resources.displayMetrics)
        return p.measureText(text.toString())
    }

    fun updateTextSize() {
        if (!resizingEnabled) {
            textSize = maxTextSize
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

        textSize = temp
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
