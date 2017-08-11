package jp.blanktar.ruumusic.client.preference


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.Preference
import kotlinx.android.synthetic.main.fragment_player_preference.*


class PlayerPreferenceFragment : Fragment() {
    var preference: Preference? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_player_preference, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        preference = Preference(getContext())

        bindPreferenceOnOff(player_auto_shrink_switch as SwitchCompat, preference!!.PlayerAutoShrinkEnabled) {
            (player_auto_shrink_switch as SwitchCompat).setChecked(preference!!.PlayerAutoShrinkEnabled.get())
        }

        bindSeekBarPreference(music_path_size_seekbar as SeekBar, preference!!.PlayerMusicPathSize) {
            size -> (music_path_size_sample as TextView).setTextSize(size.toFloat())
        }

        bindSeekBarPreference(music_name_size_seekbar as SeekBar, preference!!.PlayerMusicNameSize) {
            size -> (music_name_size_sample as TextView).setTextSize(size.toFloat())
        }

        (reset as Button).setOnClickListener {
            preference!!.PlayerAutoShrinkEnabled.remove()
            preference!!.PlayerMusicPathSize.remove()
            preference!!.PlayerMusicNameSize.remove()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        preference?.unsetAllListeners()
    }
}
