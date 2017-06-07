package jp.blanktar.ruumusic.client


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.Preference


class PlayerPreferenceFragment : Fragment() {
    var preference: Preference? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_player_preference, container, false)

        preference = Preference(getContext())

        bindSeekBarPreference(view.findViewById(R.id.music_path_size_seekbar) as SeekBar, preference!!.PlayerMusicPathSize) {
            size -> (view.findViewById(R.id.music_path_size_sample) as TextView).setTextSize(size.toFloat())
        }

        bindSeekBarPreference(view.findViewById(R.id.music_name_size_seekbar) as SeekBar, preference!!.PlayerMusicNameSize) {
            size -> (view.findViewById(R.id.music_name_size_sample) as TextView).setTextSize(size.toFloat())
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()

        preference?.unsetAllListeners()
    }
}
