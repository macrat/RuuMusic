package jp.blanktar.ruumusic.client


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.Preference


class WidgetPreferenceFragment : Fragment() {
    var preference: Preference? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_widget_preference, container, false)

        preference = Preference(getContext())

        bindSeekBarPreference(view.findViewById(R.id.unified_path_size_seekbar) as SeekBar, preference!!.UnifiedWidgetMusicPathSize) {
            size -> (view.findViewById(R.id.unified_path_size_sample) as TextView).setTextSize(size.toFloat())
        }

        bindSeekBarPreference(view.findViewById(R.id.unified_name_size_seekbar) as SeekBar, preference!!.UnifiedWidgetMusicNameSize) {
            size -> (view.findViewById(R.id.unified_name_size_sample) as TextView).setTextSize(size.toFloat())
        }

        bindSeekBarPreference(view.findViewById(R.id.musicname_name_size_seekbar) as SeekBar, preference!!.MusicNameWidgetNameSize) {
            size -> (view.findViewById(R.id.musicname_name_size_sample) as TextView).setTextSize(size.toFloat())
        }

        (view.findViewById(R.id.reset) as Button).setOnClickListener {
            preference!!.UnifiedWidgetMusicPathSize.remove()
            preference!!.UnifiedWidgetMusicNameSize.remove()
            preference!!.MusicNameWidgetNameSize.remove()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()

        preference?.unsetAllListeners()
    }
}
