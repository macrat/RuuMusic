package jp.blanktar.ruumusic.util

import android.content.Intent

import jp.blanktar.ruumusic.service.RuuService


class EqualizerInfo() {
    var min: Short = 0
    var max: Short = 0

    var freqs = intArrayOf()
    var presets = arrayOf<String>()


    fun toIntent(): Intent {
        val intent = Intent(RuuService.ACTION_EQUALIZER_INFO)

        intent.putExtra("equalizer_min", min)
        intent.putExtra("equalizer_max", max)
        intent.putExtra("equalizer_freqs", freqs)
        intent.putExtra("equalizer_presets", presets)

        return intent
    }

    constructor(intent: Intent) : this() {
        min = intent.getShortExtra("equalizer_min", 0)
        max = intent.getShortExtra("equalizer_max", 0)
        freqs = intent.getIntArrayExtra("equalizer_freqs")!!
        presets = intent.getStringArrayExtra("equalizer_presets")!!
    }
}
