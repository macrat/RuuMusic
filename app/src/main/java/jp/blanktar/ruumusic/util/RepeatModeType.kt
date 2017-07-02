package jp.blanktar.ruumusic.util


enum class RepeatModeType {
    OFF { override val next get() = LOOP },
    LOOP { override val next get() = SINGLE },
    SINGLE { override val next get() = OFF };

    abstract val next: RepeatModeType
}
