package jp.blanktar.ruumusic.service

import jp.blanktar.ruumusic.util.EqualizerInfo
import jp.blanktar.ruumusic.util.PlayingStatus
import jp.blanktar.ruumusic.util.RuuFile


interface Endpoint {
    fun onDestroy()

    fun onStatusUpdated(status: PlayingStatus)
    fun onEqualizerInfo(info: EqualizerInfo)
    fun onFailedPlay(file: RuuFile)
}
