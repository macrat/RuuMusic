package jp.blanktar.ruumusic.client.preference


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import android.widget.SeekBar
import android.widget.TextView

import jp.blanktar.ruumusic.R
import kotlinx.android.synthetic.main.fragment_widget_preference.*


class WidgetPreferenceFragment : Fragment() {
    var preference: jp.blanktar.ruumusic.util.Preference? = null

    override fun onCreateView(inflater: android.view.LayoutInflater?, container: android.view.ViewGroup?, savedInstanceState: android.os.Bundle?): android.view.View? {
        return inflater!!.inflate(jp.blanktar.ruumusic.R.layout.fragment_widget_preference, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        preference = jp.blanktar.ruumusic.util.Preference(getContext())

        bindSeekBarPreference(unified_path_size_seekbar as SeekBar, preference!!.UnifiedWidgetMusicPathSize) {
            size -> (unified_path_size_sample as TextView).setTextSize(size.toFloat())
        }

        bindSeekBarPreference(unified_name_size_seekbar as SeekBar, preference!!.UnifiedWidgetMusicNameSize) {
            size -> (unified_name_size_sample as TextView).setTextSize(size.toFloat())
        }

        bindSeekBarPreference(musicname_name_size_seekbar as SeekBar, preference!!.MusicNameWidgetNameSize) {
            size -> (musicname_name_size_sample as TextView).setTextSize(size.toFloat())
        }

        (reset as android.widget.Button).setOnClickListener {
            preference!!.UnifiedWidgetMusicPathSize.remove()
            preference!!.UnifiedWidgetMusicNameSize.remove()
            preference!!.MusicNameWidgetNameSize.remove()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        preference?.unsetAllListeners()
    }
}
