package jp.blanktar.ruumusic.client


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.PresetReverb
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.Preference
import jp.blanktar.ruumusic.service.RuuService


class SoundPreferenceFragment : Fragment() {
    var preference: Preference? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_sound_preference, container, false)

        preference = Preference(getContext())

        setupBassBoost(view)
        setupReverb(view)
        setupLoudness(view)

        getContext().registerReceiver(broadcastReceiver, IntentFilter(RuuService.ACTION_EFFECT_INFO))
        getContext().startService(Intent(getContext(), RuuService::class.java).setAction(RuuService.ACTION_REQUEST_EFFECT_INFO))

        view.findViewById(R.id.equalizer_switch).setEnabled(false)
        (view.findViewById(R.id.equalizer_switch) as SwitchCompat).setChecked(preference!!.EqualizerEnabled.get())
        view.findViewById(R.id.equalizer_spinner).setEnabled(preference!!.EqualizerEnabled.get())

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()

        getContext().unregisterReceiver(broadcastReceiver)
        preference?.unsetAllListeners()
    }

    private fun setupBassBoost(view: View) {
        bindPreferenceOnOff(view.findViewById(R.id.bass_boost_switch) as SwitchCompat, preference!!.BassBoostEnabled) {
            e -> view.findViewById(R.id.bass_boost_level).setEnabled(e)
        }
        bindSeekBarPreference(view.findViewById(R.id.bass_boost_level) as SeekBar, preference!!.BassBoostLevel)
    }

    private fun setupReverb(view: View) {
        bindPreferenceOnOff(view.findViewById(R.id.reverb_switch) as SwitchCompat, preference!!.ReverbEnabled) {
            e -> view.findViewById(R.id.reverb_spinner).setEnabled(e)
        }

        val adapter = ArrayAdapter.createFromResource(getContext(), R.array.reverb_options, R.layout.spinner_item)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        val spinner = view.findViewById(R.id.reverb_spinner) as Spinner
        spinner.adapter = adapter

        bindSpinnerPreference(spinner, preference!!.ReverbType, listOf(PresetReverb.PRESET_LARGEHALL,
                                                             PresetReverb.PRESET_MEDIUMHALL,
                                                             PresetReverb.PRESET_LARGEROOM,
                                                             PresetReverb.PRESET_MEDIUMROOM,
                                                             PresetReverb.PRESET_SMALLROOM))
    }

    private fun setupLoudness(view: View) {
        if (Build.VERSION.SDK_INT < 19) {
            view.findViewById(R.id.loudness_wrapper).setVisibility(View.GONE)
            return
        }

        bindPreferenceOnOff(view.findViewById(R.id.loudness_switch) as SwitchCompat, preference!!.LoudnessEnabled) {
            e -> view.findViewById(R.id.loudness_level).setEnabled(e)
        }
        bindSeekBarPreference(view.findViewById(R.id.loudness_level) as SeekBar, preference!!.LoudnessLevel)
    }

    private fun setupEqualizer(view: View, presets: Array<String>, min: Int, max: Int, freqs: IntArray) {
        val adapter = ArrayAdapter<String>(getContext(), R.layout.spinner_item)
        adapter.add("Custom")
        presets.forEach { x -> adapter.add(x) }
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        val spinner = view.findViewById(R.id.equalizer_spinner) as Spinner
        spinner.adapter = adapter

        spinner.setSelection(preference!!.EqualizerPreset.get() + 1)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                preference!!.EqualizerPreset.set((position - 1).toShort())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        preference!!.EqualizerPreset.setOnChangeListener {
            spinner.setSelection(preference!!.EqualizerPreset.get() + 1)
        }

        val texts = mutableListOf<TextView>()
        val bars = mutableListOf<SeekBar>()

        for (i in 0..(freqs.size-1)) {
            val table = getLayoutInflater(null).inflate(R.layout.equalizer_preference_row, view.findViewById(R.id.equalizer_container) as ViewGroup) as ViewGroup
            val row = table.getChildAt(table.getChildCount() - 1) as ViewGroup

            (row.findViewById(R.id.equalizer_freq) as TextView).setText("${freqs[i]/1000}Hz");
            texts.add(row.findViewById(R.id.equalizer_freq) as TextView)

            val seekBar = row.findViewById(R.id.equalizer_bar) as SeekBar
            seekBar.setMax(max - min)
            seekBar.setProgress(preference!!.EqualizerLevel.get(i) - min)

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean){
                    if (fromUser) {
                        preference!!.EqualizerPreset.set((-1).toShort())
                    }
                    preference!!.EqualizerLevel.set(i, progress + min);
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })

            bars.add(seekBar)
        }

        preference!!.EqualizerLevel.setOnChangeListener {
            bars.zip(preference!!.EqualizerLevel.get()).forEach {
                x -> x.first.setProgress(x.second - min)
            }
        }

        bindPreferenceOnOff(view.findViewById(R.id.equalizer_switch) as SwitchCompat, preference!!.EqualizerEnabled) { e ->
            view.findViewById(R.id.equalizer_spinner).setEnabled(e)

            texts.forEach { x -> x.setEnabled(e) }
            bars.forEach { x -> x.setEnabled(e) }
        }
    }

    private fun setEqualizerInfo(intent: Intent) {
        view!!.findViewById(R.id.equalizer_switch).setEnabled(true)

        setupEqualizer(view!!,
                       intent.getStringArrayExtra("equalizer_presets"),
                       intent.getShortExtra("equalizer_min", 0.toShort()).toInt(),
                       intent.getShortExtra("equalizer_max", 0.toShort()).toInt(),
                       intent.getIntArrayExtra("equalizer_freqs"))
    }


    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (RuuService.ACTION_EFFECT_INFO == intent.action) {
                setEqualizerInfo(intent)
            }
        }
    }
}
