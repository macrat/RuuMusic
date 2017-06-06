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
    @JvmField val RepeatMode = EnumPreferenceHandler<RepeatModeType>(context, "repeat_mode", RepeatModeType.OFF, {x -> RepeatModeType.valueOf(x)})
    @JvmField val ShuffleMode = BooleanPreferenceHandler(context, "shuffle_mode")
    @JvmField val RecursivePath = StringPreferenceHandler(context, "recursive_path")
    @JvmField val SearchQuery = StringPreferenceHandler(context, "search_query")
    @JvmField val SearchPath = StringPreferenceHandler(context, "search_path")
    @JvmField val LastPlayMusic = StringPreferenceHandler(context, "last_play_music")
    @JvmField val LastPlayPosition = IntPreferenceHandler(context, "last_play_position")

    // playlist state
    @JvmField val CurrentViewPath = StringPreferenceHandler(context, "current_view_path")
    @JvmField val LastSearchQuery = StringPreferenceHandler(context, "last_search_query")


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

    abstract inner class PreferenceHandler<T>(val context: Context, val key: String, val default: T) : SharedPreferences.OnSharedPreferenceChangeListener {
        var receiver: (() -> Unit)? = null

        val sharedPreferences: SharedPreferences
            get() = PreferenceManager.getDefaultSharedPreferences(context)

        abstract fun get(): T
        abstract fun set(value: T)

        open fun remove() {
            sharedPreferences.edit().remove(key).apply()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
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


    inner class IntPreferenceHandler(context: Context, key: String, default: Int = 0) : PreferenceHandler<Int>(context, key, default) {
        override fun get() = sharedPreferences.getInt(key, default)

        override fun set(value: Int) {
            sharedPreferences.edit().putInt(key, value).apply()
        }
    }


    inner class ShortPreferenceHandler(context: Context, key: String, default: Short = 0) : PreferenceHandler<Short>(context, key, default) {
        override fun get() = sharedPreferences.getInt(key, default.toInt()).toShort()

        override fun set(value: Short) {
            sharedPreferences.edit().putInt(key, value.toInt()).apply()
        }
    }


    inner class IntListPreferenceHandler(context: Context, key: String, val defaultInt: Int = 0) : PreferenceHandler<List<Int>>(context, key, listOf<Int>()) {
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


    inner class BooleanPreferenceHandler(context: Context, key: String, default: Boolean = false) : PreferenceHandler<Boolean>(context, key, default) {
        override fun get() = sharedPreferences.getBoolean(key, default)

        override fun set(value: Boolean) {
            sharedPreferences.edit().putBoolean(key, value).apply()
        }
    }


    inner class StringPreferenceHandler(context: Context, key: String, default: String? = null) : PreferenceHandler<String?>(context, key, default) {
        override fun get() = sharedPreferences.getString(key, default)

        override fun set(value: String?) {
            sharedPreferences.edit().putString(key, value).apply()
        }
    }

    inner class EnumPreferenceHandler<T: Enum<T>>(context: Context, key: String, default: T, val asEnum: (String) -> T) : PreferenceHandler<T>(context, key, default) {
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
