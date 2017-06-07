package jp.blanktar.ruumusic.util

import android.preference.PreferenceManager
import android.content.SharedPreferences
import android.content.Context


enum class RepeatModeType {
    OFF { override val next get() = LOOP },
    LOOP { override val next get() = SINGLE },
    SINGLE { override val next get() = OFF };

    abstract val next: RepeatModeType;
}


class Preference(val context: Context) {
    @JvmField val AudioPrefix = "audio_"

    @JvmField val RootDirectory = StringPreferenceHandler("root_directory")

    @JvmField val BassBoostEnabled = BooleanPreferenceHandler(AudioPrefix + "bassboost_enabled")
    @JvmField val BassBoostLevel = ShortPreferenceHandler(AudioPrefix + "bassboost_level")

    @JvmField val ReverbEnabled = BooleanPreferenceHandler(AudioPrefix + "reverb_enabled")
    @JvmField val ReverbType = ShortPreferenceHandler(AudioPrefix + "reverb_type")

    @JvmField val LoudnessEnabled = BooleanPreferenceHandler(AudioPrefix + "loudness_enabled")
    @JvmField val LoudnessLevel = IntPreferenceHandler(AudioPrefix + "loudness_level")

    @JvmField val EqualizerEnabled = BooleanPreferenceHandler(AudioPrefix + "equalizer_enabled")
    @JvmField val EqualizerPreset = ShortPreferenceHandler(AudioPrefix + "equalizer_preset", default = -1)
    @JvmField val EqualizerLevel = IntListPreferenceHandler(AudioPrefix + "equalizer_level")

    // player preference
    @JvmField val PlayerMusicPathSize = IntPreferenceHandler("player_music_path_size", 20)
    @JvmField val PlayerMusicNameSize = IntPreferenceHandler("player_music_name_size", 40)

    // widget preference
    @JvmField val UnifiedWidgetMusicPathSize = IntPreferenceHandler("widget_unified_music_path_size", 14)
    @JvmField val UnifiedWidgetMusicNameSize = IntPreferenceHandler("widget_unified_music_name_size", 18)
    @JvmField val MusicNameWidgetNameSize = IntPreferenceHandler("widget_musicname_music_name_size", 20)

    // client state
    @JvmField val LastViewPage = IntPreferenceHandler("last_view_page", 1)

    // player state
    @JvmField val RepeatMode = EnumPreferenceHandler<RepeatModeType>("repeat_mode", RepeatModeType.OFF, {x -> RepeatModeType.valueOf(x)})
    @JvmField val ShuffleMode = BooleanPreferenceHandler("shuffle_mode")
    @JvmField val RecursivePath = StringPreferenceHandler("recursive_path")
    @JvmField val SearchQuery = StringPreferenceHandler("search_query")
    @JvmField val SearchPath = StringPreferenceHandler("search_path")
    @JvmField val LastPlayMusic = StringPreferenceHandler("last_play_music")
    @JvmField val LastPlayPosition = IntPreferenceHandler("last_play_position")

    // playlist state
    @JvmField val CurrentViewPath = StringPreferenceHandler("current_view_path")
    @JvmField val LastSearchQuery = StringPreferenceHandler("last_search_query")


    val listeners = mutableSetOf<PreferenceHandler<*>>()

    fun unsetAllListeners() {
        while (!listeners.isEmpty()) {
            try {
                val x = listeners.first()
                x.unsetOnChangeListener()
                listeners.remove(x)
            } catch(e: NoSuchElementException) {
                break
            }
        }
    }

    abstract inner class PreferenceHandler<T>(val key: String, val default: T) : SharedPreferences.OnSharedPreferenceChangeListener {
        var receiver: (() -> Unit)? = null

        val sharedPreferences: SharedPreferences
            get() = PreferenceManager.getDefaultSharedPreferences(context)

        abstract fun get(): T
        abstract fun set(value: T)

        open fun remove() {
            sharedPreferences.edit().remove(key).apply()
        }

        open override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            if (receiver != null && key == this.key) {
                receiver!!()
            }
        }

        fun setOnChangeListener(listener: () -> Unit) {
            if (receiver != null) {
                unsetOnChangeListener()
            }
            receiver = listener

            listeners.add(this)
            sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        fun unsetOnChangeListener() {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            listeners.remove(this)
            receiver = null
        }
    }


    inner class IntPreferenceHandler(key: String, default: Int = 0) : PreferenceHandler<Int>(key, default) {
        override fun get() = sharedPreferences.getInt(key, default)

        override fun set(value: Int) {
            sharedPreferences.edit().putInt(key, value).apply()
        }
    }


    inner class ShortPreferenceHandler(key: String, default: Short = 0) : PreferenceHandler<Short>(key, default) {
        override fun get() = sharedPreferences.getInt(key, default.toInt()).toShort()

        override fun set(value: Short) {
            sharedPreferences.edit().putInt(key, value.toInt()).apply()
        }
    }


    inner class IntListPreferenceHandler(key: String, val defaultInt: Int = 0) : PreferenceHandler<List<Int>>(key, listOf<Int>()) {
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

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            if (receiver != null && key.startsWith(this.key + "_")) {
                receiver!!()
            }
        }
    }


    inner class BooleanPreferenceHandler(key: String, default: Boolean = false) : PreferenceHandler<Boolean>(key, default) {
        override fun get() = sharedPreferences.getBoolean(key, default)

        override fun set(value: Boolean) {
            sharedPreferences.edit().putBoolean(key, value).apply()
        }
    }


    inner class StringPreferenceHandler(key: String, default: String? = null) : PreferenceHandler<String?>(key, default) {
        override fun get() = sharedPreferences.getString(key, default)

        override fun set(value: String?) {
            sharedPreferences.edit().putString(key, value).apply()
        }
    }

    inner class EnumPreferenceHandler<T: Enum<T>>(key: String, default: T, val asEnum: (String) -> T) : PreferenceHandler<T>(key, default) {
        override fun get(): T {
            try {
                return asEnum(sharedPreferences.getString(key, ""))
            } catch(e: IllegalArgumentException) {
                return default
            }
        }

        override fun set(value: T) {
            sharedPreferences.edit().putString(key, value.name).apply()
        }
    }
}
