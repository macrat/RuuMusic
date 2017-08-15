package jp.blanktar.ruumusic.wear

import android.app.Activity
import android.app.Fragment
import android.app.FragmentManager
import android.os.Bundle
import android.support.v13.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : Activity() {
    var receiver: RuuReceiver? = null
    var player: PlayerFragment? = null
    var playlist: PlaylistFragment? = null

    override protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pagerAdapter = PagerAdapter(getFragmentManager())
        pager.setAdapter(pagerAdapter)

        receiver = RuuReceiver(applicationContext)
        receiver?.onStatusUpdated = { status ->
            if (status.hasError && status.errorTime + 1000 > System.currentTimeMillis()) {
                Toast.makeText(applicationContext, status.error, Toast.LENGTH_LONG).show()
            }
            player?.onStatusUpdated(status)
            playlist?.onStatusUpdated(status)
        }

        val controller = RuuController(applicationContext)

        player = PlayerFragment(controller)
        playlist = PlaylistFragment(controller, receiver!!)
        playlist?.onMusicChanged = {
            pager.setCurrentItem(0)
        }
    }

    override fun onStart() {
        super.onStart()

        receiver?.connect()
    }

    override fun onStop() {
        receiver?.disconnect()

        super.onStop()
    }

    inner class PagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment? {
            when (position) {
                0 -> return player
                1 -> return playlist!!
            }
            return null
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence {
            return listOf("Player", "Playlist")[position]
        }
    }
}
