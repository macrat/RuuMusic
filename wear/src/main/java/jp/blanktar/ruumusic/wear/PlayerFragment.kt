package jp.blanktar.ruumusic.wear

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils

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

    private fun startAnimation(view: View) {
        view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.doing_effect))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        playpause.setOnClickListener {
            startAnimation(playpause)

            if (status.playing) {
                controller.pause()
            } else {
                controller.play()
            }
        }

        next.setOnClickListener {
            startAnimation(next)

            controller.next()
        }

        prev.setOnClickListener {
            startAnimation(prev)

            controller.prev()
        }

        repeat.setOnClickListener {
            startAnimation(repeat)

            controller.repeat(status.repeat.next)
        }

        shuffle.setOnClickListener {
            startAnimation(shuffle)

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

    fun onEnterAmbient() {
        musicPath.visibility = View.INVISIBLE
        musicName.getPaint().setAntiAlias(false)
    }

    fun onExitAmbient() {
        musicPath.visibility = View.VISIBLE
        musicName.getPaint().setAntiAlias(true)
    }

    fun onStatusUpdated(s: Status) {
        playpause.clearAnimation()
        next.clearAnimation()
        prev.clearAnimation()
        repeat.clearAnimation()
        shuffle.clearAnimation()

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
