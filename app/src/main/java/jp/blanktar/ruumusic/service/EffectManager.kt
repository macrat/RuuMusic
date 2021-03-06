package jp.blanktar.ruumusic.service

import kotlin.concurrent.thread

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.Handler
import android.widget.Toast

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.Preference


class EffectManager(val player: MediaPlayer, val context: Context) {
    private val preference = Preference(context)

    private var bassBoost: BassBoost? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var virtualizer: Virtualizer? = null


    init {
        preference.BassBoostEnabled.setOnChangeListener { updateBassBoost() }
        preference.BassBoostLevel.setOnChangeListener { updateBassBoost() }

        preference.ReverbEnabled.setOnChangeListener { updateReverb() }
        preference.ReverbType.setOnChangeListener { updateReverb() }

        preference.LoudnessEnabled.setOnChangeListener { updateLoudnessEnhancer() }
        preference.LoudnessLevel.setOnChangeListener { updateLoudnessEnhancer() }

        preference.EqualizerEnabled.setOnChangeListener { updateEqualizer() }
        preference.EqualizerPreset.setOnChangeListener { updateEqualizer() }
        preference.EqualizerLevel.setOnChangeListener { updateEqualizer() }

        preference.VirtualizerEnabled.setOnChangeListener { updateVirtualizer() }
        preference.VirtualizerMode.setOnChangeListener { updateVirtualizer() }
        preference.VirtualizerStrength.setOnChangeListener { updateVirtualizer() }

        updateBassBoost()
        updateReverb()
        updateLoudnessEnhancer()
        updateEqualizer()
        updateVirtualizer()
    }

    fun release() {
        preference.unsetAllListeners()

        bassBoost?.release()
        presetReverb?.release()
        loudnessEnhancer?.release()
        equalizer?.release()
    }

    fun updateBassBoost() {
        if (preference.BassBoostEnabled.get()) {
            try {
                if (bassBoost == null) {
                    bassBoost = BassBoost(0, player.audioSessionId)
                }
                bassBoost!!.setStrength(preference.BassBoostLevel.get())
                bassBoost!!.enabled = true
            } catch (e: UnsupportedOperationException) {
                showToast(context.getString(R.string.audioeffect_cant_enable))
                preference.BassBoostEnabled.set(false)
            } catch (e: RuntimeException) {
                showToast(context.getString(R.string.audioeffect_failed_enable, context.getString(R.string.bass_boost_switch)))
                preference.BassBoostEnabled.set(false)
            }

        } else {
            bassBoost?.release()
            bassBoost = null
        }
    }

    fun updateReverb() {
        if (preference.ReverbEnabled.get()) {
            try {
                if (presetReverb == null) {
                    presetReverb = PresetReverb(0, player.audioSessionId)
                }
                presetReverb!!.preset = preference.ReverbType.get()
                presetReverb!!.enabled = true
            } catch (e: UnsupportedOperationException) {
                showToast(context.getString(R.string.audioeffect_cant_enable))
                preference.ReverbEnabled.set(false)
            } catch (e: RuntimeException) {
                showToast(context.getString(R.string.audioeffect_failed_enable, context.getString(R.string.reverb_switch)))
                preference.ReverbEnabled.set(false)
            }

        } else {
            presetReverb?.release()
            presetReverb = null
        }
    }

    fun updateLoudnessEnhancer() {
        if (Build.VERSION.SDK_INT < 19) {
            return
        }

        if (preference.LoudnessEnabled.get()) {
            try {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
                }
                loudnessEnhancer!!.setTargetGain(preference.LoudnessLevel.get())
                loudnessEnhancer!!.enabled = true
            } catch (e: UnsupportedOperationException) {
                showToast(context.getString(R.string.audioeffect_cant_enable))
                preference.LoudnessEnabled.set(false)
            } catch (e: RuntimeException) {
                showToast(context.getString(R.string.audioeffect_failed_enable, context.getString(R.string.loudness_switch)))
                preference.LoudnessEnabled.set(false)
            }
        } else {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        }
    }

    fun updateEqualizer() {
        if (preference.EqualizerEnabled.get()) {
            try {
                if (equalizer == null) {
                    equalizer = Equalizer(0, player.audioSessionId)
                }
                val preset = preference.EqualizerPreset.get()
                if (preset < 0) {
                    for (i in 0..(equalizer!!.numberOfBands - 1)) {
                        equalizer!!.setBandLevel(i.toShort(), preference.EqualizerLevel.get(i).toShort())
                    }
                } else {
                    equalizer!!.usePreset(preset)
                    for ((i, x) in equalizer!!.properties.bandLevels.withIndex()) {
                        preference.EqualizerLevel.set(i, x.toInt())
                    }
                }
                equalizer!!.enabled = true
            } catch (e: UnsupportedOperationException) {
                showToast(context.getString(R.string.audioeffect_cant_enable))
                preference.EqualizerEnabled.set(false)
            } catch (e: RuntimeException) {
                showToast(context.getString(R.string.audioeffect_failed_enable, context.getString(R.string.equalizer_switch)))
                preference.EqualizerEnabled.set(false)
            }
        } else {
            equalizer?.release()
            equalizer = null
        }
    }

    fun getEqualizerInfo(): EqualizerInfo {
        var eq = equalizer
        val have_to_release = eq == null
        if (have_to_release) {
            eq = Equalizer(0, player.audioSessionId)
        }

        val info = EqualizerInfo()

        info.min = eq!!.bandLevelRange[0]
        info.max = eq.bandLevelRange[1]

        info.freqs = IntArray(eq.numberOfBands.toInt()) { i -> eq!!.getCenterFreq(i.toShort()) }

        info.presets = Array(eq.numberOfPresets.toInt()) { i -> eq!!.getPresetName(i.toShort()) }

        if (have_to_release) {
            eq.release()
        }

        return info
    }

    fun updateVirtualizer() {
        if (preference.VirtualizerEnabled.get()) {
            try {
                if (virtualizer == null) {
                    virtualizer = Virtualizer(0, player.audioSessionId)
                }
                if (Build.VERSION.SDK_INT >= 21) {
                    virtualizer!!.forceVirtualizationMode(preference.VirtualizerMode.get())
                }
                virtualizer!!.setStrength(preference.VirtualizerStrength.get())
                virtualizer!!.enabled = true
            } catch (e: UnsupportedOperationException) {
                showToast(context.getString(R.string.audioeffect_cant_enable))
                preference.VirtualizerEnabled.set(false)
            } catch (e: RuntimeException) {
                showToast(context.getString(R.string.audioeffect_failed_enable, context.getString(R.string.virtualizer_switch)))
                preference.VirtualizerEnabled.set(false)
            }

        } else {
            virtualizer?.release()
            virtualizer = null
        }
    }

    private fun showToast(message: String) {
        val handler = Handler()
        thread {
            handler.post {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}