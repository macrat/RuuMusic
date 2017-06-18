package jp.blanktar.ruumusic.client.preference


fun bindPreferenceOnOff(switch: android.support.v7.widget.SwitchCompat, pref: jp.blanktar.ruumusic.util.Preference.BooleanPreferenceHandler, receiver: (Boolean) -> Unit) {
    receiver(pref.get())
    switch.setChecked(pref.get())

    switch.setOnCheckedChangeListener { _, checked -> pref.set(checked) }
    pref.setOnChangeListener { receiver(pref.get()) }
}


fun bindSeekBarPreference(bar: android.widget.SeekBar, pref: jp.blanktar.ruumusic.util.Preference.IntPreferenceHandler, callback: ((Int) -> Unit)? = null) {
    bar.progress = pref.get()

    callback?.invoke(pref.get())

    bar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
            pref.set(progress)

            callback?.invoke(progress)
        }

        override fun onStartTrackingTouch(p0: android.widget.SeekBar?) {}
        override fun onStopTrackingTouch(p0: android.widget.SeekBar?) {}
    })

    pref.setOnChangeListener {
        bar.progress = pref.get()
    }
}


fun bindSeekBarPreference(bar: android.widget.SeekBar, pref: jp.blanktar.ruumusic.util.Preference.ShortPreferenceHandler, callback: ((Short) -> Unit)? = null) {
    bar.progress = pref.get().toInt()

    callback?.invoke(pref.get())

    bar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
            pref.set(progress.toShort())

            callback?.invoke(progress.toShort())
        }

        override fun onStartTrackingTouch(p0: android.widget.SeekBar?) {}
        override fun onStopTrackingTouch(p0: android.widget.SeekBar?) {}
    })

    pref.setOnChangeListener {
        bar.progress = pref.get().toInt()
    }
}


fun <T> bindSpinnerPreference(spinner: android.widget.Spinner, pref: jp.blanktar.ruumusic.util.Preference.PreferenceHandler<T>, values: List<T>) {
    spinner.setSelection(values.indexOf(pref.get()))

    spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long){
            pref.set(values[position])
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
    }

    pref.setOnChangeListener {
        spinner.setSelection(values.indexOf(pref.get()))
    }
}


class PreferenceActivity : android.support.v7.app.AppCompatActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(jp.blanktar.ruumusic.R.layout.activity_preference)

        setSupportActionBar(findViewById(jp.blanktar.ruumusic.R.id.toolbar) as android.support.v7.widget.Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val viewPager = findViewById(jp.blanktar.ruumusic.R.id.viewpager) as android.support.v4.view.ViewPager
        viewPager.adapter = object : android.support.v4.app.FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): android.support.v4.app.Fragment? {
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
                return getString(listOf(jp.blanktar.ruumusic.R.string.title_sound_preference,
                                        jp.blanktar.ruumusic.R.string.title_player_preference,
                                        jp.blanktar.ruumusic.R.string.title_widget_preference)[position])
            }
        }
        (findViewById(jp.blanktar.ruumusic.R.id.tablayout) as android.support.design.widget.TabLayout).setupWithViewPager(viewPager)
    }
}
