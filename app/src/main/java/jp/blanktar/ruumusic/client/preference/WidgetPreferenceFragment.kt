package jp.blanktar.ruumusic.client.preference


import android.widget.SeekBar
import android.widget.TextView

import jp.blanktar.ruumusic.R


class WidgetPreferenceFragment : android.support.v4.app.Fragment() {
    var preference: jp.blanktar.ruumusic.util.Preference? = null

    override fun onCreateView(inflater: android.view.LayoutInflater?, container: android.view.ViewGroup?, savedInstanceState: android.os.Bundle?): android.view.View? {
        val view = inflater!!.inflate(jp.blanktar.ruumusic.R.layout.fragment_widget_preference, container, false)

        preference = jp.blanktar.ruumusic.util.Preference(getContext())

        bindSeekBarPreference(view.findViewById(R.id.unified_path_size_seekbar) as SeekBar, preference!!.UnifiedWidgetMusicPathSize) {
            size -> (view.findViewById(R.id.unified_path_size_sample) as TextView).setTextSize(size.toFloat())
        }

        bindSeekBarPreference(view.findViewById(R.id.unified_name_size_seekbar) as SeekBar, preference!!.UnifiedWidgetMusicNameSize) {
            size -> (view.findViewById(R.id.unified_name_size_sample) as TextView).setTextSize(size.toFloat())
        }

        bindSeekBarPreference(view.findViewById(R.id.musicname_name_size_seekbar) as SeekBar, preference!!.MusicNameWidgetNameSize) {
            size -> (view.findViewById(R.id.musicname_name_size_sample) as TextView).setTextSize(size.toFloat())
        }

        (view.findViewById(jp.blanktar.ruumusic.R.id.reset) as android.widget.Button).setOnClickListener {
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
