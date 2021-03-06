package jp.blanktar.ruumusic.wear

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils

import kotlinx.android.synthetic.main.fragment_player.*


class PlayerFragment(val client: RuuClient) : Fragment() {
    var status = Status()
    var ambientMode = false

    var onMusicNameTapped: (() -> Unit)? = null

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

    fun resetAnimation() {
        playpause.clearAnimation()
        next.clearAnimation()
        prev.clearAnimation()
        repeat.clearAnimation()
        shuffle.clearAnimation()
    }

    fun startLoadingAnimation() {
        startAnimation(playpause)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        musicName.setOnClickListener {
            onMusicNameTapped?.invoke()
        }

        musicPath.setOnClickListener {
            onMusicNameTapped?.invoke()
        }

        playpause.setOnClickListener {
            startAnimation(playpause)

            if (status.playing) {
                client.pause()
            } else {
                client.play()
            }
        }

        next.setOnClickListener {
            startAnimation(next)

            client.next()
        }

        prev.setOnClickListener {
            startAnimation(prev)

            client.prev()
        }

        repeat.setOnClickListener {
            startAnimation(repeat)

            client.repeat(status.repeat.next)
        }

        shuffle.setOnClickListener {
            startAnimation(shuffle)

            client.shuffle(!status.shuffle)
        }
    }

    fun onEnterAmbient() {
        ambientMode = true
        musicPath.visibility = View.INVISIBLE
        musicName.paint.isAntiAlias = false
        updateButtons()
    }

    fun onExitAmbient() {
        ambientMode = false
        musicPath.visibility = View.VISIBLE
        musicName.paint.isAntiAlias = true
        updateButtons()
    }

    fun updateButtons() {
        if (ambientMode) {
            playpause.setImageResource(if (status.playing) R.drawable.ic_pause_ambient else R.drawable.ic_play_ambient)
            next.setImageResource(R.drawable.ic_next_ambient)
            prev.setImageResource(R.drawable.ic_prev_ambient)
            repeat.setImageResource(when (status.repeat) {
                RepeatModeType.OFF -> R.drawable.ic_repeat_off_ambient
                RepeatModeType.LOOP -> R.drawable.ic_repeat_all_ambient
                RepeatModeType.SINGLE -> R.drawable.ic_repeat_one_ambient
            })
            shuffle.setImageResource(if (status.shuffle) R.drawable.ic_shuffle_on_ambient else R.drawable.ic_shuffle_off_ambient)
        } else {
            playpause.setImageResource(if (status.playing) R.drawable.ic_pause else R.drawable.ic_play)
            next.setImageResource(R.drawable.ic_next)
            prev.setImageResource(R.drawable.ic_prev)
            repeat.setImageResource(when (status.repeat) {
                RepeatModeType.OFF -> R.drawable.ic_repeat_off
                RepeatModeType.LOOP -> R.drawable.ic_repeat_all
                RepeatModeType.SINGLE -> R.drawable.ic_repeat_one
            })
            shuffle.setImageResource(if (status.shuffle) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off)
        }
    }

    fun onStatusUpdated(s: Status) {
        status = s

        resetAnimation()

        musicName.text = status.musicName
        musicPath.text = status.musicDir

        updateButtons()
    }
}
