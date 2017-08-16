package jp.blanktar.ruumusic.wear

import android.app.Fragment
import android.app.FragmentManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v13.app.FragmentPagerAdapter
import android.support.wearable.activity.ConfirmationActivity
import android.support.wearable.activity.WearableActivity
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : WearableActivity() {
    var client: RuuClient? = null
    var player: PlayerFragment? = null
    var playlist: PlaylistFragment? = null
    var defaultBackgroundColor: Int = 0

    override protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setAmbientEnabled()
        defaultBackgroundColor = (pager.getBackground() as ColorDrawable).color

        val pagerAdapter = PagerAdapter(getFragmentManager())
        pager.setAdapter(pagerAdapter)

        client = RuuClient(applicationContext)

        client?.onStatusUpdated = { status ->
            if (status.hasError && status.errorTime + 1000 > System.currentTimeMillis()) {
                Toast.makeText(applicationContext, status.error, Toast.LENGTH_LONG).show()
            }
            player?.onStatusUpdated(status)
            playlist?.onStatusUpdated(status)
        }

        client?.onFailedSendMessage = {
            player?.resetAnimation()

            startActivity(Intent(this, ConfirmationActivity::class.java)
                              .putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION)
                              .putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.failed_send_message)))
        }

        player = PlayerFragment(client!!)
        playlist = PlaylistFragment(client!!)

        playlist?.onMusicChanged = {
            pager.setCurrentItem(0)
        }

        player?.onMusicNameTapped = {
            playlist?.setDirectoryByPath(client!!.status.musicPath.dropLast(client!!.status.musicPath.length - client!!.status.musicPath.lastIndexOf('/') - 1)) {
                playlist?.scrollTo(client!!.status.musicName)
                player?.startLoadingAnimation()
                pager.setCurrentItem(1)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        client?.connect()
    }

    override fun onPause() {
        client?.disconnect()

        super.onPause()
    }

    override fun onEnterAmbient(ambientDetails: Bundle) {
        super.onEnterAmbient(ambientDetails)

        player?.onEnterAmbient()
        playlist?.onEnterAmbient()

        pager.setBackgroundColor(Color.BLACK)
    }

    override fun onExitAmbient() {
        super.onExitAmbient()

        player?.onExitAmbient()
        playlist?.onExitAmbient()

        pager.setBackgroundColor(defaultBackgroundColor)
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
