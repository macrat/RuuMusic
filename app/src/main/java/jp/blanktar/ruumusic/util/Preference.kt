package jp.blanktar.ruumusic.util

import android.preference.PreferenceManager
import android.content.SharedPreferences
import android.content.Context


class Preference(val context: Context) {
    @JvmField val AudioPrefix = "audio_"

    @JvmField val RootDirectory = StringPreferenceHandler(context, "root_directory")

    @JvmField val BassBoostEnabled = BooleanPreferenceHandler(context, AudioPrefix + "bassboost_enabled")
    @JvmField val BassBoostLevel = ShortPreferenceHandler(context, AudioPrefix + "bassboost_level")

    @JvmField val ReverbEnabled = BooleanPreferenceHandler(context, AudioPrefix + "reverb_enabled")
    @JvmField val ReverbType = ShortPreferenceHandler(context, AudioPrefix + "reverb_type")

    @JvmField val LoudnessEnabled = BooleanPreferenceHandler(context, AudioPrefix + "loudness_enabled")
    @JvmField val LoudnessLevel = IntPreferenceHandler(context, AudioPrefix + "loudness_level")

    @JvmField val EqualizerEnabled = BooleanPreferenceHandler(context, AudioPrefix + "equalizer_enabled")
    @JvmField val EqualizerPreset = ShortPreferenceHandler(context, AudioPrefix + "equalizer_preset", default = -1)
    @JvmField val EqualizerLevel = IntListPreferenceHandler(context, AudioPrefix + "equalizer_level")

    // client state
    @JvmField val LastViewPage = IntPreferenceHandler(context, "last_view_page", 1)

    // player state
    @JvmField val RepeatMode = StringPreferenceHandler(context, "repeat_mode", default = "off")
    @JvmField val ShuffleMode = BooleanPreferenceHandler(context, "shuffle_mode")
    @JvmField val RecursivePath = StringPreferenceHandler(context, "recursive_path")
    @JvmField val SearchQuery = StringPreferenceHandler(context, "search_query")
    @JvmField val SearchPath = StringPreferenceHandler(context, "search_path")
    @JvmField val LastPlayMusic = StringPreferenceHandler(context, "last_play_music")
    @JvmField val LastPlayPosition = IntPreferenceHandler(context, "last_play_position")

    // playlist state
    @JvmField val CurrentViewPath = StringPreferenceHandler(context, "current_view_path")
    @JvmField val LastSearchQuery = StringPreferenceHandler(context, "last_search_query")


    interface PreferenceHandler<T> {
        val context: Context
        val key: String
        val default: T

        val sharedPreferences: SharedPreferences
            get() = PreferenceManager.getDefaultSharedPreferences(context)

        fun get(): T
        fun set(value: T)

        fun remove() {
            sharedPreferences.edit().remove(key).apply()
        }
    }


    class IntPreferenceHandler(override val context: Context, override val key: String, override val default: Int = 0) : PreferenceHandler<Int> {
        override fun get() = sharedPreferences.getInt(key, default)

        override fun set(value: Int) {
            sharedPreferences.edit().putInt(key, value).apply()
        }
    }


    class ShortPreferenceHandler(override val context: Context, override val key: String, override val default: Short = 0) : PreferenceHandler<Short> {
        override fun get() = sharedPreferences.getInt(key, default.toInt()).toShort()

        override fun set(value: Short) {
            sharedPreferences.edit().putInt(key, value.toInt()).apply()
        }
    }


    class IntListPreferenceHandler(override val context: Context, override val key: String, val defaultInt: Int = 0) : PreferenceHandler<List<Int>> {
        override val default = listOf<Int>()
        private fun keyOf(index: Int) = "%s_%d".format(key, index)

        fun get(index: Int) = sharedPreferences.getInt(keyOf(index), defaultInt)

        override fun get(): List<Int> {
            var result = mutableListOf<Int>()
            var index = 0
            while (sharedPreferences.contains(keyOf(index))) {
                result.add(get(index))
                index++
            }
            return result
        }

        fun set(index: Int, value: Int) {
            sharedPreferences.edit().putInt(keyOf(index), value).apply()
        }

        override fun set(value: List<Int>) {
            var index = 0
            for (x in value) {
                set(index, x)
                index++
            }
        }

        override fun remove() {
            var index = 0
            while (sharedPreferences.contains(keyOf(index))) {
                sharedPreferences.edit().remove(keyOf(index)).apply()
                index++
            }
        }
    }


    class BooleanPreferenceHandler(override val context: Context, override val key: String, override val default: Boolean = false) : PreferenceHandler<Boolean> {
        override fun get() = sharedPreferences.getBoolean(key, default)

        override fun set(value: Boolean) {
            sharedPreferences.edit().putBoolean(key, value).apply()
        }
    }


    class StringPreferenceHandler(override val context: Context, override val key: String, override val default: String? = null) : PreferenceHandler<String?> {
        override fun get() = sharedPreferences.getString(key, default)

        override fun set(value: String?) {
            sharedPreferences.edit().putString(key, value).apply()
        }
    }
}

