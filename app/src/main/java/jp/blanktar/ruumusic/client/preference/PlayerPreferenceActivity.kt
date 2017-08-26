package jp.blanktar.ruumusic.client.preference


import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.Preference
import kotlinx.android.synthetic.main.activity_player_preference.*


class PlayerPreferenceActivity : AppCompatActivity() {
    var preference: Preference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_preference)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        preference = Preference(applicationContext)

        bindPreferenceOnOff(player_auto_shrink_switch, preference!!.PlayerAutoShrinkEnabled) {
            player_auto_shrink_switch.isChecked = preference!!.PlayerAutoShrinkEnabled.get()
        }

        bindSeekBarPreference(music_path_size_seekbar, preference!!.PlayerMusicPathSize) {
            size -> music_path_size_sample.textSize = size.toFloat()
        }

        bindSeekBarPreference(music_name_size_seekbar, preference!!.PlayerMusicNameSize) {
            size -> music_name_size_sample.textSize = size.toFloat()
        }

        reset.setOnClickListener {
            preference!!.PlayerAutoShrinkEnabled.remove()
            preference!!.PlayerMusicPathSize.remove()
            preference!!.PlayerMusicNameSize.remove()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        preference?.unsetAllListeners()
    }
}
