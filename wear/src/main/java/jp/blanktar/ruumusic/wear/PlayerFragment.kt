package jp.blanktar.ruumusic.wear

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import kotlinx.android.synthetic.main.fragment_player.*


class PlayerFragment : Fragment() {
    var controller: RuuController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controller = RuuController(context)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        playpause.setOnClickListener {
            controller?.play()
        }

        next.setOnClickListener {
            controller?.next()
        }

        prev.setOnClickListener {
            controller?.prev()
        }
    }

    override fun onResume() {
        super.onResume()

        controller?.connect()
    }

    override fun onPause() {
        super.onPause()

        controller?.disconnect()
    }
}
