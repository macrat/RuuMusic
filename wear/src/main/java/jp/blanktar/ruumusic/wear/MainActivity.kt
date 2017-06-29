package jp.blanktar.ruumusic.wear

import android.app.Activity
import android.app.Fragment
import android.app.FragmentManager
import android.os.Bundle
import android.support.v13.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager


class MainActivity : Activity() {
    override protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pagerAdapter = PagerAdapter(getFragmentManager())
        (findViewById(R.id.pager) as ViewPager).setAdapter(pagerAdapter)
    }

    inner class PagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment? {
            when (position) {
                0 -> return PlayerFragment()
                1 -> return PlaylistFragment()
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
