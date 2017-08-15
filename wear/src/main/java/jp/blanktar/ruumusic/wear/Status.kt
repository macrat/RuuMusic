package jp.blanktar.ruumusic.wear

import com.google.android.gms.wearable.DataMap


enum class RepeatModeType {
    OFF { override val next get() = LOOP },
    LOOP { override val next get() = SINGLE },
    SINGLE { override val next get() = OFF };

    abstract val next: RepeatModeType
}


class Status(val playing: Boolean = false,
             val rootPath: String = "/",
             val musicPath: String = "/",
             val repeat: RepeatModeType = RepeatModeType.OFF,
             val shuffle: Boolean = false,
             val error: String? = null,
             val errorTime: Long = 0) {

    val musicDir: String
        get() {
            return musicPath.dropLast(musicPath.length - musicPath.lastIndexOf('/') - 1).drop(rootPath.length - 1)
        }

    val musicName = musicPath.drop(musicPath.lastIndexOf('/') + 1)

    val hasError
        get() = error != null

    constructor(data: DataMap)
            : this(data.getBoolean("playing", false),
            data.getString("root_path", "/"),
            data.getString("music_path", "/"),
            RepeatModeType.valueOf(data.getString("repeat_mode", "OFF")),
            data.getBoolean("shuffle_mode", false),
            data.getString("error_message", null),
            data.getLong("error_time", 0)) {}
}
