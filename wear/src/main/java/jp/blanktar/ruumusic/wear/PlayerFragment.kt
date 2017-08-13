package jp.blanktar.ruumusic.wear

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import jp.blanktar.ruumusic.wear.R
import kotlinx.android.synthetic.main.fragment_player.*


class PlayerFragment(val controller: RuuController) : Fragment() {
    var status = Status()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        playpause.setOnClickListener {
            if (status.playing) {
                controller.pause()
            } else {
                controller.play()
            }
        }

        next.setOnClickListener {
            controller.next()
        }

        prev.setOnClickListener {
            controller.prev()
        }

        repeat.setOnClickListener {
            controller.repeat(status.repeat.next)
        }

        shuffle.setOnClickListener {
            controller.shuffle(!status.shuffle)
        }
    }

    override fun onResume() {
        super.onResume()

        controller.connect()
    }

    override fun onPause() {
        super.onPause()

        controller.disconnect()
    }

    fun onStatusUpdated(s: Status) {
        status = s
        playpause.setImageResource(if (status.playing) R.drawable.ic_pause else R.drawable.ic_play)
        repeat.setImageResource(when (status.repeat) {
            RepeatModeType.OFF -> R.drawable.ic_repeat_off
            RepeatModeType.LOOP -> R.drawable.ic_repeat_all
            RepeatModeType.SINGLE -> R.drawable.ic_repeat_one
        })
        shuffle.setImageResource(if (status.shuffle) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off)
        musicName.text = status.musicName
        musicPath.text = status.musicDir
    }
}
