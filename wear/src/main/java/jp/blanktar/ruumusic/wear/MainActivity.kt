package jp.blanktar.ruumusic.wear

import android.app.Activity
import android.os.Bundle
import android.support.v13.app.FragmentPagerAdapter
import android.app.FragmentManager
import android.support.v4.view.ViewPager
import android.app.Fragment


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
            }
            return null
        }

        override fun getCount(): Int {
            return 1
        }

        override fun getPageTitle(position: Int): CharSequence {
            return "Player"
        }
    }
}
