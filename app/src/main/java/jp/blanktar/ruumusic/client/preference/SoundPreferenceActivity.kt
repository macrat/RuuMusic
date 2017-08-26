package jp.blanktar.ruumusic.client.preference


import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.RuuClient
import kotlinx.android.synthetic.main.activity_sound_preference.*
import jp.blanktar.ruumusic.util.Preference


class SoundPreferenceActivity : AppCompatActivity() {
    var preference: Preference? = null
    var client: RuuClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_preference)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        preference = Preference(applicationContext)
        client = RuuClient(applicationContext)

        setupBassBoost()
        setupReverb()
        setupLoudness()
        setupVirtualizer()

        client!!.requestEqualizerInfo()
        client!!.eventListener = object : RuuClient.EventListener() {
            override fun onEqualizerInfo(info: EqualizerInfo) {
                equalizer_spinner.visibility = View.VISIBLE
                equalizer_progress.visibility = View.GONE
                equalizer_switch.isEnabled = true
                setupEqualizer(info)
            }
        }

        equalizer_switch.isEnabled = false
        equalizer_switch.isChecked = preference!!.EqualizerEnabled.get()
        equalizer_spinner.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()

        preference?.unsetAllListeners()
        client?.release()
    }

    private fun setupBassBoost() {
        bindPreferenceOnOff(bass_boost_switch, preference!!.BassBoostEnabled) {
            e ->
            bass_boost_level.isEnabled = e
        }
        bindSeekBarPreference(bass_boost_level, preference!!.BassBoostLevel)
    }

    private fun setupReverb() {
        bindPreferenceOnOff(reverb_switch, preference!!.ReverbEnabled) {
            e ->
            reverb_spinner.isEnabled = e
        }

        val adapter = ArrayAdapter.createFromResource(this, R.array.reverb_options, R.layout.spinner_item)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        reverb_spinner.adapter = adapter

        bindSpinnerPreference(reverb_spinner, preference!!.ReverbType, listOf(PresetReverb.PRESET_LARGEHALL,
                                                                              PresetReverb.PRESET_MEDIUMHALL,
                                                                              PresetReverb.PRESET_LARGEROOM,
                                                                              PresetReverb.PRESET_MEDIUMROOM,
                                                                              PresetReverb.PRESET_SMALLROOM))
    }

    private fun setupLoudness() {
        if (Build.VERSION.SDK_INT < 19) {
            loudness_wrapper.visibility = View.GONE
            return
        }

        bindPreferenceOnOff(loudness_switch, preference!!.LoudnessEnabled) {
            e ->
            loudness_level.isEnabled = e
        }
        bindSeekBarPreference(loudness_level, preference!!.LoudnessLevel)
    }

    private fun setupVirtualizer() {
        if (Build.VERSION.SDK_INT >= 21) {
            val adapter = ArrayAdapter.createFromResource(this, R.array.virtualizer_modes, R.layout.spinner_item)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            virtualizer_mode.adapter = adapter
        }

        bindPreferenceOnOff(virtualizer_switch, preference!!.VirtualizerEnabled) {
            virtualizer_mode.isEnabled = it
            virtualizer_strength.isEnabled = it
        }
        bindSeekBarPreference(virtualizer_strength, preference!!.VirtualizerStrength)
        if (Build.VERSION.SDK_INT < 21) {
            virtualizer_mode.visibility = View.GONE
        } else {
            bindSpinnerPreference(virtualizer_mode, preference!!.VirtualizerMode, listOf(Virtualizer.VIRTUALIZATION_MODE_AUTO,
                    Virtualizer.VIRTUALIZATION_MODE_BINAURAL,
                    Virtualizer.VIRTUALIZATION_MODE_TRANSAURAL))
        }
    }

    private fun setupEqualizer(info: EqualizerInfo) {
        val adapter = ArrayAdapter<String>(this, R.layout.spinner_item)
        adapter.add("Custom")
        info.presets.forEach { x -> adapter.add(x) }
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        equalizer_spinner.adapter = adapter

        equalizer_spinner.setSelection(preference!!.EqualizerPreset.get() + 1)

        equalizer_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                preference!!.EqualizerPreset.set((position - 1).toShort())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        preference!!.EqualizerPreset.setOnChangeListener {
            equalizer_spinner.setSelection(preference!!.EqualizerPreset.get() + 1)
        }

        val texts = mutableListOf<TextView>()
        val bars = mutableListOf<SeekBar>()

        val inflater = LayoutInflater.from(this)
        for (i in 0..(info.freqs.size-1)) {
            val table = inflater.inflate(R.layout.equalizer_preference_row, equalizer_container) as ViewGroup
            val row = table.getChildAt(table.childCount - 1)

            row.findViewById<TextView>(R.id.equalizer_freq).text = "${info.freqs[i]/1000}Hz"
            texts.add(row.findViewById(R.id.equalizer_freq))

            val seekBar = row.findViewById<SeekBar>(R.id.equalizer_bar)
            seekBar.max = info.max - info.min
            seekBar.progress = preference!!.EqualizerLevel.get(i) - info.min

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar, progress: Int, fromUser: Boolean){
                    if (fromUser) {
                        preference!!.EqualizerPreset.set((-1).toShort())
                    }
                    preference!!.EqualizerLevel.set(i, progress + info.min)
                }

                override fun onStartTrackingTouch(b: SeekBar) {}
                override fun onStopTrackingTouch(b: SeekBar) {}
            })

            bars.add(seekBar)
        }

        preference!!.EqualizerLevel.setOnChangeListener {
            bars.zip(preference!!.EqualizerLevel.get()).forEach {
                (bar, center) ->
                bar.progress = center - info.min
            }
        }

        bindPreferenceOnOff(equalizer_switch, preference!!.EqualizerEnabled) { e ->
            equalizer_spinner.isEnabled = e

            texts.forEach { x -> x.isEnabled = e }
            bars.forEach { x -> x.isEnabled = e }
        }
    }
}
