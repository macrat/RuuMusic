package jp.blanktar.ruumusic.client

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SwitchCompat
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Spinner

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.Preference


fun bindPreferenceOnOff(switch: SwitchCompat, pref: Preference.BooleanPreferenceHandler, receiver: (Boolean) -> Unit) {
    receiver(pref.get())
    switch.setChecked(pref.get())

    switch.setOnCheckedChangeListener { _, checked -> pref.set(checked) }
    pref.setOnChangeListener { receiver(pref.get()) }
}


fun bindSeekBarPreference(bar: SeekBar, pref: Preference.IntPreferenceHandler, callback: ((Int) -> Unit)? = null) {
    bar.progress = pref.get()

    callback?.invoke(pref.get())

    bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
            pref.set(progress)

            callback?.invoke(progress)
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
    })

    pref.setOnChangeListener {
        bar.progress = pref.get()
    }
}


fun bindSeekBarPreference(bar: SeekBar, pref: Preference.ShortPreferenceHandler, callback: ((Short) -> Unit)? = null) {
    bar.progress = pref.get().toInt()

    callback?.invoke(pref.get())

    bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
            pref.set(progress.toShort())

            callback?.invoke(progress.toShort())
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
    })

    pref.setOnChangeListener {
        bar.progress = pref.get().toInt()
    }
}


fun <T> bindSpinnerPreference(spinner: Spinner, pref: Preference.PreferenceHandler<T>, values: List<T>) {
    spinner.setSelection(values.indexOf(pref.get()))

    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long){
            pref.set(values[position])
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    pref.setOnChangeListener {
        spinner.setSelection(values.indexOf(pref.get()))
    }
}


class PreferenceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val viewPager = findViewById(R.id.viewpager) as ViewPager
        viewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment? {
                return when (position) {
                    0 -> SoundPreferenceFragment()
                    1 -> PlayerPreferenceFragment()
                    2 -> WidgetPreferenceFragment()
                    else -> null
                }
            }

            override fun getCount(): Int {
                return 3
            }

            override fun getPageTitle(position: Int): CharSequence {
                return getString(listOf(R.string.title_sound_preference,
                                        R.string.title_player_preference,
                                        R.string.title_widget_preference)[position])
            }
        }
        (findViewById(R.id.tablayout) as TabLayout).setupWithViewPager(viewPager)
    }
}
