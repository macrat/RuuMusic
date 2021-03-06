package jp.blanktar.ruumusic.client.preference


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import jp.blanktar.ruumusic.R
import kotlinx.android.synthetic.main.activity_widget_preference.*


class WidgetPreferenceActivity : AppCompatActivity() {
    var preference: jp.blanktar.ruumusic.util.Preference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_preference)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        preference = jp.blanktar.ruumusic.util.Preference(applicationContext)

        bindSeekBarPreference(unified_path_size_seekbar, preference!!.UnifiedWidgetMusicPathSize) {
            size ->
            unified_path_size_sample.textSize = size.toFloat()
        }

        bindSeekBarPreference(unified_name_size_seekbar, preference!!.UnifiedWidgetMusicNameSize) {
            size ->
            unified_name_size_sample.textSize = size.toFloat()
        }

        bindSeekBarPreference(musicname_name_size_seekbar, preference!!.MusicNameWidgetNameSize) {
            size ->
            musicname_name_size_sample.textSize = size.toFloat()
        }

        reset.setOnClickListener {
            preference!!.UnifiedWidgetMusicPathSize.remove()
            preference!!.UnifiedWidgetMusicNameSize.remove()
            preference!!.MusicNameWidgetNameSize.remove()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        preference?.unsetAllListeners()
    }
}
